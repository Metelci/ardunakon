/*
 * Ardunakon - Arduino Mega 2560 Sketch
 *
 * Full support for Arduino Mega 2560 with Bluetooth module
 * Designed for use with the Ardunakon Android Controller App
 *
 * Board: Arduino Mega 2560 (ATmega2560)
 * Connectivity: HC-05, HC-06, HM-10, AT-09, BT05 via Hardware Serial 1
 *
 * Wiring:
 * - Module TX -> Arduino Pin 19 (RX1)
 * - Module RX -> Arduino Pin 18 (TX1)
 *
 * Protocol: Uses shared ArdunakonProtocol library
 */

#include <Servo.h>
#include <ArdunakonProtocol.h>

// User: Serial1 (Hardware Serial) instead of SoftwareSerial
#define BTSerial Serial1

// Board Type (Using Classic ID for compatibility)
#define BOARD_TYPE_MEGA 0x01

// Capability Flags
#define CAP1_SERVO_X    0x01
#define CAP1_SERVO_Y    0x02
#define CAP1_MOTOR      0x04
#define CAP1_BUZZER     0x10

// Pin Definitions
// Motors (L298N / TB6612FNG style)
#define MOTOR_LEFT_PWM    9
#define MOTOR_LEFT_DIR1   8
#define MOTOR_LEFT_DIR2   7
#define MOTOR_RIGHT_PWM   6
#define MOTOR_RIGHT_DIR1  5
#define MOTOR_RIGHT_DIR2  4

// Servos
#define SERVO_X_PIN       2  // Controlled by L/R keys
#define SERVO_Y_PIN       12 // Controlled by W/B keys
#define SERVO_Z_PIN       11 // Controlled by A/Z keys (Mega has plenty of PWM)

#define LED_STATUS        13
#define BUZZER_PIN        3

// Objects
Servo servoX;
Servo servoY;
Servo servoZ;
ArdunakonProtocol protocol;

// Packet buffer
uint8_t packetBuffer[ArdunakonProtocol::PACKET_SIZE];
uint8_t bufferIndex = 0;
unsigned long lastPacketTime = 0;
unsigned long lastHeartbeatTime = 0;

// Control state
int8_t leftX = 0, leftY = 0; // Drive (Steering, Throttle)
int8_t rightX = 0, rightY = 0, rightZ = 0; // Servos
uint8_t auxBits = 0;
bool emergencyStop = false;

// Telemetry
float batteryVoltage = 0.0;
uint32_t packetsReceived = 0;

void setup() {
  Serial.begin(115200);     // USB Debugging
  BTSerial.begin(9600);     // Bluetooth Module (Default baud)

  // Initialize pins
  pinMode(MOTOR_LEFT_PWM, OUTPUT);
  pinMode(MOTOR_LEFT_DIR1, OUTPUT);
  pinMode(MOTOR_LEFT_DIR2, OUTPUT);
  pinMode(MOTOR_RIGHT_PWM, OUTPUT);
  pinMode(MOTOR_RIGHT_DIR1, OUTPUT);
  pinMode(MOTOR_RIGHT_DIR2, OUTPUT);
  pinMode(LED_STATUS, OUTPUT);
  pinMode(BUZZER_PIN, OUTPUT);

  // Initialize Servos
  servoX.attach(SERVO_X_PIN);
  servoY.attach(SERVO_Y_PIN);
  servoZ.attach(SERVO_Z_PIN);
  servoX.write(90); // Center
  servoY.write(90); // Center
  servoZ.write(90); // Center

  Serial.println("Arduino Mega 2560 - Ardunakon Controller");
  Serial.println("Waiting for Bluetooth connection on Serial1 (Pins 19/18)...");
  digitalWrite(LED_STATUS, LOW);

  // Ready blink
  for (int i = 0; i < 3; i++) {
    digitalWrite(LED_STATUS, HIGH); delay(200);
    digitalWrite(LED_STATUS, LOW); delay(200);
  }
}

void loop() {
  // Check for incoming Bluetooth data
  while (BTSerial.available() > 0) {
    uint8_t byte = BTSerial.read();
    processIncomingByte(byte);
    digitalWrite(LED_STATUS, HIGH);
  }

  // Send telemetry every 2 seconds
  if (millis() - lastHeartbeatTime > 2000) {
    sendTelemetry();
    lastHeartbeatTime = millis();
  }

  // Safety timeout - stop if no packet for 2 seconds
  if (millis() - lastPacketTime > 2000) {
    safetyStop();
    digitalWrite(LED_STATUS, LOW);
  }
}

void processIncomingByte(uint8_t byte) {
  if (bufferIndex == 0 && byte != ArdunakonProtocol::START_BYTE) return;
  packetBuffer[bufferIndex++] = byte;

  if (bufferIndex >= ArdunakonProtocol::PACKET_SIZE) {
    if (packetBuffer[9] == ArdunakonProtocol::END_BYTE && protocol.validateChecksum(packetBuffer)) {
        ArdunakonProtocol::ControlPacket packet = protocol.parsePacket(packetBuffer);
        handlePacket(packet);
        packetsReceived++;
    }
    bufferIndex = 0;
  }
}

void handlePacket(ArdunakonProtocol::ControlPacket packet) {
  lastPacketTime = millis();
  
  switch (packet.cmd) {
    case ArdunakonProtocol::CMD_JOYSTICK:
      leftX = packet.leftX;
      leftY = packet.leftY;
      rightX = packet.rightX;
      rightY = packet.rightY;
      auxBits = packet.auxBits;

       // Parse A/Z/W/B/L/R from auxBits for servo Z if needed or discrete control
       if (auxBits & ArdunakonProtocol::AUX_A) rightZ = -127;
       else if (auxBits & ArdunakonProtocol::AUX_Z) rightZ = 127;
       else rightZ = 0;

      if (!emergencyStop) {
        updateDrive();
        updateServos();
      }
      break;

    case ArdunakonProtocol::CMD_BUTTON:
      handleButton(packet.leftX, packet.leftY); 
      break;

    case ArdunakonProtocol::CMD_HEARTBEAT:
      sendHeartbeatAck(packet.leftX, packet.leftY);
      break;

    case ArdunakonProtocol::CMD_ESTOP:
      emergencyStop = true;
      safetyStop();
      tone(BUZZER_PIN, 2000, 500);
      break;
      
    case ArdunakonProtocol::CMD_ANNOUNCE_CAPABILITIES:
      sendCapabilities();
      break;
  }
}

void updateDrive() {
  // ARCADE DRIVE MIXING
  int leftSpeed = constrain(leftY + leftX, -100, 100);
  int rightSpeed = constrain(leftY - leftX, -100, 100);

  setMotor(MOTOR_LEFT_PWM, MOTOR_LEFT_DIR1, MOTOR_LEFT_DIR2, leftSpeed);
  setMotor(MOTOR_RIGHT_PWM, MOTOR_RIGHT_DIR1, MOTOR_RIGHT_DIR2, rightSpeed);
}

void updateServos() {
  int angleX = map(rightX, -100, 100, 0, 180);
  int angleY = map(rightY, -100, 100, 0, 180);
  int angleZ = map(rightZ, -127, 127, 0, 180); // Using full range for buttons
  
  servoX.write(angleX);
  servoY.write(angleY);
  servoZ.write(angleZ);
}

void setMotor(int pwmPin, int dir1Pin, int dir2Pin, int speed) {
  if (speed > 0) {
    digitalWrite(dir1Pin, HIGH); digitalWrite(dir2Pin, LOW);
    analogWrite(pwmPin, map(speed, 0, 100, 0, 255));
  } else if (speed < 0) {
    digitalWrite(dir1Pin, LOW); digitalWrite(dir2Pin, HIGH);
    analogWrite(pwmPin, map(-speed, 0, 100, 0, 255));
  } else {
    digitalWrite(dir1Pin, LOW); digitalWrite(dir2Pin, LOW);
    analogWrite(pwmPin, 0);
  }
}

void handleButton(uint8_t buttonId, uint8_t pressed) {
  if (buttonId == 1 && pressed == 1) {
    emergencyStop = false;
    Serial.println("E-STOP CLEARED");
  }
}

void safetyStop() {
  analogWrite(MOTOR_LEFT_PWM, 0); analogWrite(MOTOR_RIGHT_PWM, 0);
  digitalWrite(MOTOR_LEFT_DIR1, LOW); digitalWrite(MOTOR_LEFT_DIR2, LOW);
  digitalWrite(MOTOR_RIGHT_DIR1, LOW); digitalWrite(MOTOR_RIGHT_DIR2, LOW);
}

void sendTelemetry() {
  int rawValue = analogRead(A0);
  batteryVoltage = (rawValue / 1023.0) * 5.0 * 3.0; // 3x voltage divider

  uint8_t telemetry[ArdunakonProtocol::PACKET_SIZE];
  uint8_t status = emergencyStop ? 0x01 : 0x00;
  
  protocol.formatTelemetry(telemetry, 0x01, batteryVoltage, status, packetsReceived);
  
  BTSerial.write(telemetry, ArdunakonProtocol::PACKET_SIZE);
}

void sendHeartbeatAck(uint8_t seqHigh, uint8_t seqLow) {
  uint8_t ack[ArdunakonProtocol::PACKET_SIZE];
  ack[0] = ArdunakonProtocol::START_BYTE; 
  ack[1] = 0x01; 
  ack[2] = ArdunakonProtocol::CMD_HEARTBEAT;
  ack[3] = seqHigh; 
  ack[4] = seqLow;
  for(int i=5; i<=7; i++) ack[i] = 0; // Padding
  protocol.createChecksum(ack);
  ack[9] = ArdunakonProtocol::END_BYTE;

  BTSerial.write(ack, ArdunakonProtocol::PACKET_SIZE);
}

void sendCapabilities() {
  uint8_t packet[ArdunakonProtocol::PACKET_SIZE];
  packet[0] = ArdunakonProtocol::START_BYTE;
  packet[1] = 0x01;
  packet[2] = ArdunakonProtocol::CMD_ANNOUNCE_CAPABILITIES;
  packet[3] = CAP1_SERVO_X | CAP1_SERVO_Y | CAP1_MOTOR | CAP1_BUZZER;
  packet[4] = 0x00; 
  packet[5] = BOARD_TYPE_MEGA;
  packet[6] = 0x00;
  packet[7] = 0x00;
  
  protocol.createChecksum(packet);
  packet[9] = ArdunakonProtocol::END_BYTE;
  
  BTSerial.write(packet, ArdunakonProtocol::PACKET_SIZE);
}
