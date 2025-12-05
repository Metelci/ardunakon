/*
 * Ardunakon - Classic Arduino UNO Sketch (with HC-05/HC-06/HM-10)
 *
 * Full support for classic Arduino UNO with Bluetooth module
 * Designed for use with the Ardunakon Android Controller App
 *
 * Board: Arduino UNO (ATmega328P)
 * Connectivity: HC-05, HC-06, HM-10, AT-09, BT05 via SoftwareSerial
 *
 * Protocol: 10-byte binary packets @ 20Hz
 * [START, DEV_ID, CMD, D1, D2, D3, D4, D5, CHECKSUM, END]
 *
 * v2.0 - Arcade Drive + Servo Support
 */

#include <SoftwareSerial.h>
#include <Servo.h>

// Bluetooth Serial Pins
#define BT_RX 10  // Connect to Module TX
#define BT_TX 11  // Connect to Module RX

// Protocol Constants
#define START_BYTE 0xAA
#define END_BYTE   0x55
#define PACKET_SIZE 10

// Commands
#define CMD_JOYSTICK  0x01
#define CMD_BUTTON    0x02
#define CMD_HEARTBEAT 0x03
#define CMD_ESTOP     0x04

// Pin Definitions
// Motors (L298N / TB6612FNG style)
#define MOTOR_LEFT_PWM    9
#define MOTOR_LEFT_DIR1   8
#define MOTOR_LEFT_DIR2   7
#define MOTOR_RIGHT_PWM   6
#define MOTOR_RIGHT_DIR1  5
#define MOTOR_RIGHT_DIR2  4

// Servos
#define SERVO_X_PIN       2  // Controlled by A/L keys
#define SERVO_Y_PIN       12 // Controlled by W/R keys

#define LED_STATUS        13
#define BUZZER_PIN        3

// Objects
SoftwareSerial BTSerial(BT_RX, BT_TX);
Servo servoX;
Servo servoY;

// Packet buffer
uint8_t packetBuffer[PACKET_SIZE];
uint8_t bufferIndex = 0;
unsigned long lastPacketTime = 0;
unsigned long lastHeartbeatTime = 0;

// Control state
int8_t leftX = 0, leftY = 0; // Drive (Steering, Throttle)
int8_t rightX = 0, rightY = 0; // Servos
uint8_t auxBits = 0;
bool emergencyStop = false;

// Telemetry
float batteryVoltage = 0.0;

void setup() {
  Serial.begin(115200);
  BTSerial.begin(9600); // Standard baud rate for HC-05/06/HM-10

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

  Serial.println("Arduino UNO - Ardunakon Controller v2.0");
  Serial.println("Waiting for Bluetooth connection...");
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

  // Send telemetry every 4 seconds
  if (millis() - lastHeartbeatTime > 4000) {
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
  if (bufferIndex == 0 && byte != START_BYTE) return;
  packetBuffer[bufferIndex++] = byte;

  if (bufferIndex >= PACKET_SIZE) {
    if (packetBuffer[9] == END_BYTE && validateChecksum()) {
      handlePacket();
    }
    bufferIndex = 0;
  }
}

bool validateChecksum() {
  uint8_t xor_check = 0;
  for (int i = 1; i <= 7; i++) xor_check ^= packetBuffer[i];
  return (xor_check == packetBuffer[8]);
}

void handlePacket() {
  lastPacketTime = millis();
  uint8_t cmd = packetBuffer[2];

  switch (cmd) {
    case CMD_JOYSTICK:
      // Map inputs (-100 to 100)
      leftX = map(packetBuffer[3], 0, 200, -100, 100); // Steering
      leftY = map(packetBuffer[4], 0, 200, -100, 100); // Throttle
      rightX = map(packetBuffer[5], 0, 200, -100, 100); // Servo X
      rightY = map(packetBuffer[6], 0, 200, -100, 100); // Servo Y
      auxBits = packetBuffer[7];

      if (!emergencyStop) {
        updateDrive();
        updateServos();
      }
      break;

    case CMD_BUTTON:
      handleButton(packetBuffer[3], packetBuffer[4]);
      break;

    case CMD_HEARTBEAT:
      sendHeartbeatAck(packetBuffer[3], packetBuffer[4]);
      break;

    case CMD_ESTOP:
      emergencyStop = true;
      safetyStop();
      tone(BUZZER_PIN, 2000, 500);
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
  // rightX covers A/L keys
  // rightY covers W/R keys
  
  int angleX = map(rightX, -100, 100, 0, 180);
  int angleY = map(rightY, -100, 100, 0, 180);
  
  servoX.write(angleX);
  servoY.write(angleY);
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

  uint8_t telemetry[PACKET_SIZE];
  telemetry[0] = START_BYTE;
  telemetry[1] = 0x01;
  telemetry[2] = CMD_HEARTBEAT;
  telemetry[3] = (uint8_t)(batteryVoltage * 10);
  telemetry[4] = emergencyStop ? 1 : 0;
  for(int i=5; i<=7; i++) telemetry[i] = 0;

  uint8_t xor_check = 0;
  for (int i = 1; i <= 7; i++) xor_check ^= telemetry[i];
  telemetry[8] = xor_check;
  telemetry[9] = END_BYTE;

  BTSerial.write(telemetry, PACKET_SIZE);
}

void sendHeartbeatAck(uint8_t seqHigh, uint8_t seqLow) {
  uint8_t ack[PACKET_SIZE];
  ack[0] = START_BYTE; ack[1] = 0x01; ack[2] = CMD_HEARTBEAT;
  ack[3] = seqHigh; ack[4] = seqLow;
  for(int i=5; i<=7; i++) ack[i] = 0;

  uint8_t xor_check = 0;
  for (int i = 1; i <= 7; i++) xor_check ^= ack[i];
  ack[8] = xor_check;
  ack[9] = END_BYTE;

  BTSerial.write(ack, PACKET_SIZE);
}

