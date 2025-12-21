/*
 * Ardunakon - Arduino UNO Q Sketch
 *
 * Full support for Arduino UNO Q with built-in Bluetooth 5.1 BLE
 * Designed for use with the Ardunakon Android Controller App
 *
 * Board: Arduino UNO Q (Qualcomm QRB2210 + STM32U585)
 * Connectivity: Bluetooth 5.1 BLE (built-in)
 *
 * Protocol: Uses shared ArdunakonProtocol library
 *
 * v2.1 - Refactored to use ArdunakonProtocol library
 */

#include <ArduinoBLE.h>
#include <Servo.h>
#include <ArdunakonProtocol.h>

// BLE Service and Characteristic UUIDs (HM-10 compatible)
// Using standard HM-10/HC-08 UUIDs for maximum compatibility - FIXED: Separate TX/RX
#define SERVICE_UUID        "0000ffe0-0000-1000-8000-00805f9b34fb"
#define CHARACTERISTIC_UUID_TX "0000ffe1-0000-1000-8000-00805f9b34fb"  // TX: Arduino sends (notify)
#define CHARACTERISTIC_UUID_RX "0000ffe2-0000-1000-8000-00805f9b34fb"  // RX: Arduino receives (write)

// Board Type (UNO Q - not officially supported yet, use 0x00)
#define BOARD_TYPE_UNO_Q 0x00

// Capability Flags
#define CAP1_SERVO_X    0x01
#define CAP1_SERVO_Y    0x02
#define CAP1_MOTOR      0x04
#define CAP1_BUZZER     0x10
#define CAP1_BLE        0x40

// Pin Definitions
// Motors
#define MOTOR_LEFT_PWM    9
#define MOTOR_LEFT_DIR1   8
#define MOTOR_LEFT_DIR2   7
#define MOTOR_RIGHT_PWM   6
#define MOTOR_RIGHT_DIR1  5
#define MOTOR_RIGHT_DIR2  4

// Servos
#define SERVO_X_PIN       2  // Controlled by L/R keys
#define SERVO_Y_PIN       12 // Controlled by W/B keys
#define SERVO_Z_PIN       A1 // Controlled by A/Z keys

#define LED_STATUS        LED_BUILTIN
#define BUZZER_PIN        3

// Objects
Servo servoX;
Servo servoY;
Servo servoZ;
ArdunakonProtocol protocol;

// BLE Objects
BLEService bleService(SERVICE_UUID);
BLECharacteristic txCharacteristic(CHARACTERISTIC_UUID_TX, BLERead | BLENotify, 20);
BLECharacteristic rxCharacteristic(CHARACTERISTIC_UUID_RX, BLEWrite | BLEWriteWithoutResponse, 20);

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

// Function prototypes
void handleIncoming(BLECharacteristic& characteristic);
void processIncomingByte(uint8_t byte);
void handlePacket(const ArdunakonProtocol::ControlPacket& packet);
void updateDrive();
void updateServos();
void setMotor(int pwmPin, int dir1Pin, int dir2Pin, int8_t speed);
void handleButton(uint8_t buttonId, uint8_t pressed);
void safetyStop();
void sendTelemetry();
void sendHeartbeatAck(uint8_t seqHigh, uint8_t seqLow);
void sendCapabilities();

void setup() {
  Serial.begin(115200);

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
  servoX.write(90); // Center
  servoY.write(90); // Center
  servoZ.attach(SERVO_Z_PIN);
  servoZ.write(90); // Center

  // Initialize BLE
  if (!BLE.begin()) {
    Serial.println("Starting BLE failed!");
    while (1);
  }

  // Set BLE device name (will appear as "ArdunakonQ" in scan)
  BLE.setLocalName("ArdunakonQ");
  BLE.setDeviceName("ArdunakonQ");

  // Add characteristics to service
  bleService.addCharacteristic(txCharacteristic);
  bleService.addCharacteristic(rxCharacteristic);

  // Add service
  BLE.addService(bleService);

  // Start advertising
  BLE.advertise();

  Serial.println("Arduino UNO Q - Ardunakon Controller v2.1");
  Serial.println("Waiting for BLE connection...");
  digitalWrite(LED_STATUS, LOW);
}

void loop() {
  // Check for BLE connection
  BLEDevice central = BLE.central();

  if (central) {
    Serial.print("Connected to: ");
    Serial.println(central.address());
    digitalWrite(LED_STATUS, HIGH);
    
    // Announce device capabilities
    sendCapabilities();

    while (central.connected()) {
      // Check if data available
      if (rxCharacteristic.written()) {
        int len = rxCharacteristic.valueLength();
        const uint8_t* data = rxCharacteristic.value();
        for (int i = 0; i < len; i++) processIncomingByte(data[i]);
      }

      // Send telemetry every 4 seconds
      if (millis() - lastHeartbeatTime > 4000) {
        sendTelemetry();
        lastHeartbeatTime = millis();
      }

      // Safety timeout
      if (millis() - lastPacketTime > 2000) {
        safetyStop();
      }
    }

    Serial.println("Disconnected");
    digitalWrite(LED_STATUS, LOW);
    safetyStop();
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

void handlePacket(const ArdunakonProtocol::ControlPacket& packet) {
  lastPacketTime = millis();
  
  switch (packet.cmd) {
    case ArdunakonProtocol::CMD_JOYSTICK:
      leftX = packet.leftX;
      leftY = packet.leftY;
      rightX = packet.rightX;
      rightY = packet.rightY;
      auxBits = packet.auxBits;

      if (!emergencyStop) {
        updateDrive();
        updateServos();
      }
      break;

    case ArdunakonProtocol::CMD_BUTTON:
      handleButton(packet.leftX, packet.leftY); // overloaded mapped fields
      break;

    case ArdunakonProtocol::CMD_SERVO_Z:
      rightZ = packet.rightZ;
      if (!emergencyStop) {
        updateServos();
      }
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
  // Y = Throttle, X = Steering
  // Left Motor = Y + X
  // Right Motor = Y - X
  
  int leftSpeed = leftY + leftX;
  int rightSpeed = leftY - leftX;

  // Clamp to -100..100
  leftSpeed = constrain(leftSpeed, -100, 100);
  rightSpeed = constrain(rightSpeed, -100, 100);

  setMotor(MOTOR_LEFT_PWM, MOTOR_LEFT_DIR1, MOTOR_LEFT_DIR2, leftSpeed);
  setMotor(MOTOR_RIGHT_PWM, MOTOR_RIGHT_DIR1, MOTOR_RIGHT_DIR2, rightSpeed);
}

void updateServos() {
  // Map -100..100 to 0..180 degrees
  int angleX = map(rightX, -100, 100, 0, 180);
  int angleY = map(rightY, -100, 100, 0, 180);
  int angleZ = map(rightZ, -100, 100, 0, 180);
  
  servoX.write(angleX);
  servoY.write(angleY);
  servoZ.write(angleZ);
}

void setMotor(int pwmPin, int dir1Pin, int dir2Pin, int8_t speed) {
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
  batteryVoltage = (rawValue / 1023.0) * 5.0 * 3.0;

  uint8_t telemetry[ArdunakonProtocol::PACKET_SIZE];
  uint8_t status = emergencyStop ? 0x01 : 0x00;
  
  protocol.formatTelemetry(telemetry, 0x01, batteryVoltage, status, packetsReceived);

  txCharacteristic.writeValue(telemetry, ArdunakonProtocol::PACKET_SIZE);
}

void sendHeartbeatAck(uint8_t seqHigh, uint8_t seqLow) {
  uint8_t ack[ArdunakonProtocol::PACKET_SIZE];
  ack[0] = ArdunakonProtocol::START_BYTE; 
  ack[1] = 0x01; 
  ack[2] = ArdunakonProtocol::CMD_HEARTBEAT;
  ack[3] = seqHigh; 
  ack[4] = seqLow;
  for(int i=5; i<=7; i++) ack[i] = 0;

  protocol.createChecksum(ack);
  ack[9] = ArdunakonProtocol::END_BYTE;

  txCharacteristic.writeValue(ack, ArdunakonProtocol::PACKET_SIZE);
}

void sendCapabilities() {
  uint8_t packet[ArdunakonProtocol::PACKET_SIZE];
  packet[0] = ArdunakonProtocol::START_BYTE;
  packet[1] = 0x01;
  packet[2] = ArdunakonProtocol::CMD_ANNOUNCE_CAPABILITIES;
  packet[3] = CAP1_SERVO_X | CAP1_SERVO_Y | CAP1_MOTOR | CAP1_BUZZER | CAP1_BLE;
  packet[4] = 0x00; // No Modulino
  packet[5] = BOARD_TYPE_UNO_Q;
  packet[6] = 0x00;
  packet[7] = 0x00;
  
  protocol.createChecksum(packet);
  packet[9] = ArdunakonProtocol::END_BYTE;
  
  txCharacteristic.writeValue(packet, ArdunakonProtocol::PACKET_SIZE);
  Serial.println("Capabilities sent");
}
