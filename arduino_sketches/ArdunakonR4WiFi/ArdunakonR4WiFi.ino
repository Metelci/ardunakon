/*
 * Ardunakon - Arduino UNO R4 WiFi Sketch
 *
 * Full support for Arduino UNO R4 WiFi with built-in BLE
 * Designed for use with the Ardunakon Android Controller App
 *
 * Board: Arduino UNO R4 WiFi (Renesas RA4M1 + ESP32-S3)
 * Connectivity: Bluetooth Low Energy (BLE) via ESP32-S3 module
 *
 * Protocol: 10-byte binary packets @ 20Hz
 * [START, DEV_ID, CMD, D1, D2, D3, D4, D5, CHECKSUM, END]
 *
 * Compatible with Ardunakon v0.1.1-alpha and newer
 * https://github.com/yourusername/ardunakon
 */

#include <ArduinoBLE.h>

// BLE Service and Characteristic UUIDs (HM-10 compatible)
// Using standard HM-10/HC-08 UUIDs for maximum compatibility
#define SERVICE_UUID        "0000ffe0-0000-1000-8000-00805f9b34fb"
#define CHARACTERISTIC_UUID "0000ffe1-0000-1000-8000-00805f9b34fb"

// Protocol Constants
#define START_BYTE 0xAA
#define END_BYTE   0x55
#define PACKET_SIZE 10

// Commands
#define CMD_JOYSTICK  0x01
#define CMD_BUTTON    0x02
#define CMD_HEARTBEAT 0x03
#define CMD_ESTOP     0x04

// Pin Definitions (customize for your project)
#define MOTOR_LEFT_PWM    9
#define MOTOR_LEFT_DIR1   8
#define MOTOR_LEFT_DIR2   7
#define MOTOR_RIGHT_PWM   6
#define MOTOR_RIGHT_DIR1  5
#define MOTOR_RIGHT_DIR2  4
#define LED_STATUS        LED_BUILTIN
#define BUZZER_PIN        3

// BLE Objects
BLEService bleService(SERVICE_UUID);
BLECharacteristic txCharacteristic(CHARACTERISTIC_UUID, BLERead | BLENotify, 20);
BLECharacteristic rxCharacteristic(CHARACTERISTIC_UUID, BLEWrite | BLEWriteWithoutResponse, 20);

// Packet buffer
uint8_t packetBuffer[PACKET_SIZE];
uint8_t bufferIndex = 0;
unsigned long lastPacketTime = 0;
unsigned long lastHeartbeatTime = 0;

// Control state
int8_t leftX = 0, leftY = 0, rightX = 0, rightY = 0;
uint8_t auxBits = 0;
bool emergencyStop = false;

// Telemetry
float batteryVoltage = 0.0;
uint8_t systemStatus = 0; // 0 = Active, 1 = Safe Mode

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

  // Initialize BLE
  if (!BLE.begin()) {
    Serial.println("Starting BLE failed!");
    while (1);
  }

  // Set BLE device name (will appear as "ArdunakonR4" in scan)
  BLE.setLocalName("ArdunakonR4");
  BLE.setDeviceName("ArdunakonR4");

  // Add characteristics to service
  bleService.addCharacteristic(txCharacteristic);
  bleService.addCharacteristic(rxCharacteristic);

  // Add service
  BLE.addService(bleService);

  // Start advertising
  BLE.advertise();

  Serial.println("Arduino UNO R4 WiFi - Ardunakon Controller");
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

    while (central.connected()) {
      // Check if data available
      if (rxCharacteristic.written()) {
        int len = rxCharacteristic.valueLength();
        const uint8_t* data = rxCharacteristic.value();

        // Process received bytes
        for (int i = 0; i < len; i++) {
          processIncomingByte(data[i]);
        }
      }

      // Send telemetry every 4 seconds
      if (millis() - lastHeartbeatTime > 4000) {
        sendTelemetry();
        lastHeartbeatTime = millis();
      }

      // Safety timeout - stop if no packet for 2 seconds
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
  // Look for START_BYTE
  if (bufferIndex == 0 && byte != START_BYTE) {
    return;
  }

  packetBuffer[bufferIndex++] = byte;

  // Check if packet complete
  if (bufferIndex >= PACKET_SIZE) {
    if (packetBuffer[9] == END_BYTE && validateChecksum()) {
      handlePacket();
    }
    bufferIndex = 0;
  }
}

bool validateChecksum() {
  uint8_t xor_check = 0;
  for (int i = 1; i <= 7; i++) {
    xor_check ^= packetBuffer[i];
  }
  return (xor_check == packetBuffer[8]);
}

void handlePacket() {
  lastPacketTime = millis();
  uint8_t cmd = packetBuffer[2];

  switch (cmd) {
    case CMD_JOYSTICK:
      // D1-D4: Left X/Y, Right X/Y (0-200, 100 is center)
      // D5: Aux bits (bitfield for 8 aux buttons)
      leftX = map(packetBuffer[3], 0, 200, -100, 100);
      leftY = map(packetBuffer[4], 0, 200, -100, 100);
      rightX = map(packetBuffer[5], 0, 200, -100, 100);
      rightY = map(packetBuffer[6], 0, 200, -100, 100);
      auxBits = packetBuffer[7];

      if (!emergencyStop) {
        updateMotors();
      }
      break;

    case CMD_BUTTON:
      // D1: Button ID, D2: Pressed (1) or Released (0)
      handleButton(packetBuffer[3], packetBuffer[4]);
      break;

    case CMD_HEARTBEAT:
      // Heartbeat acknowledged - connection alive
      // D1-D2: Sequence number (for RTT measurement)
      sendHeartbeatAck(packetBuffer[3], packetBuffer[4]);
      break;

    case CMD_ESTOP:
      emergencyStop = true;
      safetyStop();
      tone(BUZZER_PIN, 2000, 500); // Warning beep
      break;
  }
}

void updateMotors() {
  // Example: Tank-style steering
  // leftY controls left motor, rightY controls right motor

  setMotor(MOTOR_LEFT_PWM, MOTOR_LEFT_DIR1, MOTOR_LEFT_DIR2, leftY);
  setMotor(MOTOR_RIGHT_PWM, MOTOR_RIGHT_DIR1, MOTOR_RIGHT_DIR2, rightY);
}

void setMotor(int pwmPin, int dir1Pin, int dir2Pin, int8_t speed) {
  if (speed > 0) {
    digitalWrite(dir1Pin, HIGH);
    digitalWrite(dir2Pin, LOW);
    analogWrite(pwmPin, map(speed, 0, 100, 0, 255));
  } else if (speed < 0) {
    digitalWrite(dir1Pin, LOW);
    digitalWrite(dir2Pin, HIGH);
    analogWrite(pwmPin, map(-speed, 0, 100, 0, 255));
  } else {
    digitalWrite(dir1Pin, LOW);
    digitalWrite(dir2Pin, LOW);
    analogWrite(pwmPin, 0);
  }
}

void handleButton(uint8_t buttonId, uint8_t pressed) {
  Serial.print("Button ");
  Serial.print(buttonId);
  Serial.println(pressed ? " pressed" : " released");

  // Example: Button 1 clears emergency stop
  if (buttonId == 1 && pressed == 1) {
    emergencyStop = false;
    Serial.println("Emergency stop cleared");
  }

  // Add your custom button handlers here
}

void safetyStop() {
  analogWrite(MOTOR_LEFT_PWM, 0);
  analogWrite(MOTOR_RIGHT_PWM, 0);
  digitalWrite(MOTOR_LEFT_DIR1, LOW);
  digitalWrite(MOTOR_LEFT_DIR2, LOW);
  digitalWrite(MOTOR_RIGHT_DIR1, LOW);
  digitalWrite(MOTOR_RIGHT_DIR2, LOW);
}

void sendTelemetry() {
  // Read battery voltage (example: voltage divider on A0)
  // Adjust formula based on your voltage divider circuit
  int rawValue = analogRead(A0);
  batteryVoltage = (rawValue / 1023.0) * 5.0 * 3.0; // Example: 3x voltage divider

  // Build telemetry packet
  uint8_t telemetry[PACKET_SIZE];
  telemetry[0] = START_BYTE;
  telemetry[1] = 0x01; // Device ID
  telemetry[2] = CMD_HEARTBEAT;
  telemetry[3] = (uint8_t)(batteryVoltage * 10); // Battery in tenths of volts
  telemetry[4] = emergencyStop ? 1 : 0; // Status byte
  telemetry[5] = 0;
  telemetry[6] = 0;
  telemetry[7] = 0;

  // Calculate checksum
  uint8_t xor_check = 0;
  for (int i = 1; i <= 7; i++) {
    xor_check ^= telemetry[i];
  }
  telemetry[8] = xor_check;
  telemetry[9] = END_BYTE;

  // Send via BLE
  txCharacteristic.writeValue(telemetry, PACKET_SIZE);
}

void sendHeartbeatAck(uint8_t seqHigh, uint8_t seqLow) {
  // Echo back heartbeat for RTT measurement
  uint8_t ack[PACKET_SIZE];
  ack[0] = START_BYTE;
  ack[1] = 0x01;
  ack[2] = CMD_HEARTBEAT;
  ack[3] = seqHigh;
  ack[4] = seqLow;
  ack[5] = 0;
  ack[6] = 0;
  ack[7] = 0;

  uint8_t xor_check = 0;
  for (int i = 1; i <= 7; i++) {
    xor_check ^= ack[i];
  }
  ack[8] = xor_check;
  ack[9] = END_BYTE;

  txCharacteristic.writeValue(ack, PACKET_SIZE);
}
