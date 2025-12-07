// Ardunakon - Arduino UNO R4 WiFi Sketch
// Full support for Arduino UNO R4 WiFi with built-in BLE
// Designed for use with the Ardunakon Android Controller App
//
// Board: Arduino UNO R4 WiFi (Renesas RA4M1 + ESP32-S3)
// Connectivity: Bluetooth Low Energy (BLE) via ESP32-S3 module
//
// Protocol: 10-byte binary packets at 20Hz
// [START, DEV_ID, CMD, D1, D2, D3, D4, D5, CHECKSUM, END]
//
// v2.2 - Servo + Brushless ESC Support + Fixed Matrix Animation

#include <ArduinoBLE.h>
#include <Servo.h>
#include <Arduino_LED_Matrix.h>

ArduinoLEDMatrix matrix;

// ---------------------------------------------------------
// NEW ANIMATION FRAMES (Verified 8x12 Bitmaps)
// ---------------------------------------------------------

// Frame 1: Rock Hand UP
uint8_t rockHand[8][12] = {
  { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, 
  { 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0 }, // Finger tips
  { 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0 }, 
  { 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0 }, 
  { 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0 }, // Knuckles
  { 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0 }, // Palm
  { 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0 }, // Wrist
  { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }
};

// Frame 2: Rock Hand DOWN (Headbang effect)
uint8_t rockHandDown[8][12] = {
  { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
  { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, // Fingers moved down
  { 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0 }, 
  { 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0 }, 
  { 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0 },
  { 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0 }, 
  { 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0 }, 
  { 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0 }  
};

void playRockAnimation() {
  // Play the animation loop 10 times
  for (int cycle = 0; cycle < 10; cycle++) {
      matrix.renderBitmap(rockHand, 8, 12);
      delay(300);
      matrix.renderBitmap(rockHandDown, 8, 12);
      delay(300);
  }
  // Keep the main static hand visible after animation finishes
  matrix.renderBitmap(rockHand, 8, 12);
}

// BLE Service and Characteristic UUIDs
// 1) HM-10 compatible (common BLE UART clones)
#define SERVICE_UUID_HM10           "0000ffe0-0000-1000-8000-00805f9b34fb"
#define CHARACTERISTIC_UUID_HM10_TX "0000ffe1-0000-1000-8000-00805f9b34fb"  // TX: Arduino sends (notify)
#define CHARACTERISTIC_UUID_HM10_RX "0000ffe2-0000-1000-8000-00805f9b34fb"  // RX: Arduino receives (write)
// 2) ArduinoBLE official profile
#define SERVICE_UUID_ARDUINO           "19b10000-e8f2-537e-4f6c-d104768a1214"
#define CHARACTERISTIC_UUID_ARDUINO_TX "19b10001-e8f2-537e-4f6c-d104768a1214"  // TX: Arduino sends (notify)
#define CHARACTERISTIC_UUID_ARDUINO_RX "19b10002-e8f2-537e-4f6c-d104768a1214"  // RX: Arduino receives (write)

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
// Servos (directly connected to these pins)
#define SERVO_X_PIN       6   // Horizontal servo (L/R buttons)
#define SERVO_Y_PIN       7   // Vertical servo (W/B buttons)

// Brushless ESC (airplane ESC, no reverse)
#define ESC_PIN           9   // ESC signal wire

#define LED_STATUS        LED_BUILTIN
#define BUZZER_PIN        3

// Objects
Servo servoX;
Servo servoY;
Servo esc;  // Brushless ESC controlled like a servo

// BLE Objects
BLEService bleServiceHm10(SERVICE_UUID_HM10);
BLECharacteristic txCharacteristicHm10(CHARACTERISTIC_UUID_HM10_TX, BLERead | BLENotify | BLEWrite | BLEWriteWithoutResponse, 20);
BLECharacteristic rxCharacteristicHm10(CHARACTERISTIC_UUID_HM10_RX, BLEWrite | BLEWriteWithoutResponse, 20);

BLEService bleServiceArduino(SERVICE_UUID_ARDUINO);
BLECharacteristic txCharacteristicArduino(CHARACTERISTIC_UUID_ARDUINO_TX, BLERead | BLENotify | BLEWrite | BLEWriteWithoutResponse, 20);
BLECharacteristic rxCharacteristicArduino(CHARACTERISTIC_UUID_ARDUINO_RX, BLEWrite | BLEWriteWithoutResponse, 20);

// Packet buffer
uint8_t packetBuffer[PACKET_SIZE];
uint8_t bufferIndex = 0;
unsigned long lastPacketTime = 0;
unsigned long lastTelemetryTime = 0;

// Control state
int8_t leftX = 0, leftY = 0; // Drive (Steering, Throttle)
int8_t rightX = 0, rightY = 0; // Servos
uint8_t auxBits = 0;
bool emergencyStop = false;
float batteryVoltage = 0.0;

// Function prototypes
void handleIncoming(BLECharacteristic& characteristic);
void processIncomingByte(uint8_t byte);
bool validateChecksum();
void handlePacket();
void updateThrottle();
void updateServos();
void handleButton(uint8_t buttonId, uint8_t pressed);
void safetyStop();
void sendTelemetry();
void sendHeartbeatAck(uint8_t seqHigh, uint8_t seqLow);

void setup() {
  Serial.begin(115200);

  // Initialize pins
  pinMode(LED_STATUS, OUTPUT);
  pinMode(BUZZER_PIN, OUTPUT);

  // Initialize Servos
  servoX.attach(SERVO_X_PIN);
  servoY.attach(SERVO_Y_PIN);
  servoX.write(90); // Center
  servoY.write(90); // Center

  // Initialize ESC (arm at minimum throttle)
  esc.attach(ESC_PIN);
  esc.write(0);  // ESC arms when it sees minimum throttle on startup
  
  // Initialize LED Matrix
  matrix.begin();
  
  // Play rock animation on startup (Now works with renderBitmap)
  playRockAnimation();
  
  // Configure ADC for UNO R4 WiFi (14-bit)
  analogReadResolution(14);

  // Initialize BLE
  if (!BLE.begin()) {
    Serial.println("Starting BLE failed!");
    while (1);
  }

  // Set BLE device name
  BLE.setLocalName("ArdunakonR4");
  BLE.setDeviceName("ArdunakonR4");

  // Add characteristics
  bleServiceHm10.addCharacteristic(txCharacteristicHm10);
  bleServiceHm10.addCharacteristic(rxCharacteristicHm10);
  bleServiceArduino.addCharacteristic(txCharacteristicArduino);
  bleServiceArduino.addCharacteristic(rxCharacteristicArduino);
  
  // Add service
  BLE.addService(bleServiceHm10);
  BLE.addService(bleServiceArduino);

  // Advertise both service UUIDs for easier discovery
  BLE.setAdvertisedServiceUuid(SERVICE_UUID_HM10);
  BLE.setAdvertisedServiceUuid(SERVICE_UUID_ARDUINO);
  
  // Start advertising
  BLE.advertise();

  Serial.println("Arduino UNO R4 WiFi - Ardunakon Controller v2.0");
  Serial.println("Waiting for BLE connection...");
  digitalWrite(LED_STATUS, LOW);
  
  // Ready blink
  for (int i = 0; i < 3; i++) {
    digitalWrite(LED_STATUS, HIGH); delay(200);
    digitalWrite(LED_STATUS, LOW); delay(200);
  }
}

void loop() {
  // Check for BLE connection
  BLEDevice central = BLE.central();
  if (central) {
    Serial.print("Connected to: ");
    Serial.println(central.address());
    digitalWrite(LED_STATUS, HIGH);

    while (central.connected()) {
      if (rxCharacteristicHm10.written()) handleIncoming(rxCharacteristicHm10);
      if (txCharacteristicHm10.written()) handleIncoming(txCharacteristicHm10);
      // Support Legacy writes
      if (rxCharacteristicArduino.written()) handleIncoming(rxCharacteristicArduino);
      if (txCharacteristicArduino.written()) handleIncoming(txCharacteristicArduino);
      // Support Legacy writes

      BLE.poll();
      
      // Send telemetry every 4 seconds
      if (millis() - lastTelemetryTime > 4000) {
        sendTelemetry();
        lastTelemetryTime = millis();
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

void handleIncoming(BLECharacteristic& characteristic) {
  int len = characteristic.valueLength();
  const uint8_t* data = characteristic.value();
  for (int i = 0; i < len; i++) processIncomingByte(data[i]);
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
      leftX = map(packetBuffer[3], 0, 200, -100, 100); // Left Joystick X (Steering)
      leftY = map(packetBuffer[4], 0, 200, -100, 100); // Left Joystick Y (Throttle)
      rightX = map(packetBuffer[5], 0, 200, -100, 100); // Right Joystick X (Servo X)
      rightY = map(packetBuffer[6], 0, 200, -100, 100); // Right Joystick Y (Servo Y)
      auxBits = packetBuffer[7];
      
      if (!emergencyStop) {
        updateThrottle();
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

void updateThrottle() {
  // Brushless ESC control (airplane ESC - no reverse)
  // Map joystick Y (0 to +100) to ESC (0 to 180)
  // Ignore negative values (no reverse on airplane ESC)
  int throttle = 0;
  if (leftY > 0) {
    throttle = map(leftY, 0, 100, 0, 180);
  }
  esc.write(throttle);
}

void updateServos() {
  // Map -100..100 to 0..180 degrees
  int angleX = map(rightX, -100, 100, 0, 180);
  int angleY = map(rightY, -100, 100, 0, 180);
  
  servoX.write(angleX);
  servoY.write(angleY);
}

void handleButton(uint8_t buttonId, uint8_t pressed) {
  if (buttonId == 1 && pressed == 1) {
    emergencyStop = false;
    Serial.println("E-STOP CLEARED");
  }
}

void safetyStop() {
  esc.write(0);  // Cut throttle
}

void sendTelemetry() {
  int rawValue = analogRead(A0);
  batteryVoltage = (rawValue / 16383.0) * 5.0 * 3.0;

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

  txCharacteristicHm10.writeValue(telemetry, PACKET_SIZE);
  txCharacteristicArduino.writeValue(telemetry, PACKET_SIZE);
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

  txCharacteristicHm10.writeValue(ack, PACKET_SIZE);
  txCharacteristicArduino.writeValue(ack, PACKET_SIZE);
}