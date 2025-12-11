/*
 * Ardunakon - Arduino UNO R4 WiFi Sketch
 *
 * Full support for Arduino UNO R4 WiFi with built-in BLE/WiFi
 * Designed for use with the Ardunakon Android Controller App
 *
 * Board: Arduino UNO R4 WiFi (Renesas RA4M1 + ESP32-S3)
 * Connectivity: Bluetooth 5 BLE + WiFi (built-in)
 *
 * Protocol: 10-byte binary packets @ 20Hz
 * [START, DEV_ID, CMD, D1, D2, D3, D4, D5, CHECKSUM, END]
 *
 * Features:
 * - Dual connectivity: BLE and WiFi (UDP)
 * - Arcade drive mixing for differential drive robots
 * - 2-axis servo control (pan/tilt or steering)
 * - Battery voltage monitoring
 * - E-Stop safety system
 * - Device capability announcement
 *
 * Compatible UUIDs:
 * - Primary: ArduinoBLE default (19B10000/19B10001)
 * - Fallback: HM-10 style (0000FFE0/0000FFE1)
 *
 * v2.2 - WiFi + BLE Dual Mode
 * 
 * IMPORTANT: Update Servo library to v1.2.2+ for R4 WiFi compatibility
 */

#include <ArduinoBLE.h>
#include <Servo.h>
#include <WiFiS3.h>
#include <WiFiUdp.h>

// ============== CONFIGURATION ==============
// WiFi credentials - Change these to match your network
const char* WIFI_SSID = "YOUR_WIFI_SSID";
const char* WIFI_PASSWORD = "YOUR_WIFI_PASSWORD";

// UDP port for WiFi control
#define UDP_PORT 4210

// Set to true to enable WiFi mode (requires valid credentials above)
#define WIFI_ENABLED false

// ============== BLE CONFIGURATION ==============
// Primary: ArduinoBLE default UUIDs
#define PRIMARY_SERVICE_UUID        "19B10000-E8F2-537E-4F6C-D104768A1214"
#define PRIMARY_CHAR_UUID_TX        "19B10001-E8F2-537E-4F6C-D104768A1214"
#define PRIMARY_CHAR_UUID_RX        "19B10002-E8F2-537E-4F6C-D104768A1214"

// Fallback: HM-10 compatible UUIDs (for clones and older apps)
#define HM10_SERVICE_UUID           "0000ffe0-0000-1000-8000-00805f9b34fb"
#define HM10_CHAR_UUID_TX           "0000ffe1-0000-1000-8000-00805f9b34fb"
#define HM10_CHAR_UUID_RX           "0000ffe2-0000-1000-8000-00805f9b34fb"

// ============== PROTOCOL CONSTANTS ==============
#define START_BYTE 0xAA
#define END_BYTE   0x55
#define PACKET_SIZE 10

// Commands
#define CMD_JOYSTICK  0x01
#define CMD_BUTTON    0x02
#define CMD_HEARTBEAT 0x03
#define CMD_ESTOP     0x04
#define CMD_ANNOUNCE_CAPABILITIES 0x05

// Board Type
#define BOARD_TYPE_R4_WIFI 0x02

// Capability Flags (Byte 1)
#define CAP1_SERVO_X    0x01  // Has horizontal servo
#define CAP1_SERVO_Y    0x02  // Has vertical servo
#define CAP1_MOTOR      0x04  // Has motor driver
#define CAP1_ENCODER    0x08  // Has wheel encoders
#define CAP1_BUZZER     0x10  // Has buzzer
#define CAP1_WIFI       0x20  // Has WiFi
#define CAP1_BLE        0x40  // Has BLE
#define CAP1_BATTERY    0x80  // Has battery monitoring

// ============== PIN DEFINITIONS ==============
// Motor Driver (L298N / TB6612FNG / Single ESC)
// Left joystick Y-axis controls throttle, X-axis controls steering
// Note: R4 WiFi PWM pins: 3, 5, 6, 9, 10, 11
#define MOTOR_PWM         3   // ESC/Motor PWM signal (brushless ESC or DC motor)
#define MOTOR_DIR1        8   // Direction 1 (for DC motor, not used for ESC)
#define MOTOR_DIR2        7   // Direction 2 (for DC motor, not used for ESC)

// Servos - Controlled by W/B/L/R buttons
// Pin 9: Servo X (Horizontal) - L=Left, R=Right
// Pin 10: Servo Y (Vertical) - W=Forward/Up, B=Backward/Down
#define SERVO_X_PIN       9   // Horizontal servo (L/R buttons)
#define SERVO_Y_PIN       10  // Vertical servo (W/B buttons)

// Motor Deadzone - Prevents motor from running with small joystick drift
#define MOTOR_DEADZONE    5   // Values below this (%) are treated as zero

// Status LED and Buzzer
#define LED_STATUS        LED_BUILTIN  // Built-in LED (Pin 13)
#define LED_WIFI          LED_BUILTIN  // WiFi status
#define BUZZER_PIN        11           // Passive buzzer (moved from 3 to avoid PWM conflict)

// Battery Voltage (with voltage divider)
#define BATTERY_PIN       A0

// ============== OBJECTS ==============
Servo servoX;
Servo servoY;

// BLE Services and Characteristics
// Primary service (ArduinoBLE default)
BLEService primaryService(PRIMARY_SERVICE_UUID);
BLECharacteristic primaryTxChar(PRIMARY_CHAR_UUID_TX, BLERead | BLENotify, 20);
BLECharacteristic primaryRxChar(PRIMARY_CHAR_UUID_RX, BLEWrite | BLEWriteWithoutResponse, 20);

// HM-10 compatible service (fallback)
BLEService hm10Service(HM10_SERVICE_UUID);
BLECharacteristic hm10TxChar(HM10_CHAR_UUID_TX, BLERead | BLENotify, 20);
BLECharacteristic hm10RxChar(HM10_CHAR_UUID_RX, BLEWrite | BLEWriteWithoutResponse, 20);

// WiFi UDP
WiFiUDP udp;
bool wifiConnected = false;

// ============== STATE VARIABLES ==============
// Packet buffer
uint8_t packetBuffer[PACKET_SIZE];
uint8_t bufferIndex = 0;

// Timing
unsigned long lastPacketTime = 0;
unsigned long lastHeartbeatTime = 0;
unsigned long lastWifiCheckTime = 0;

// Control state (-100 to +100 range)
int8_t leftX = 0, leftY = 0;   // Drive joystick (Steering, Throttle)
int8_t rightX = 0, rightY = 0; // Servo control (X: A/L, Y: W/R)
uint8_t auxBits = 0;           // Auxiliary button states
bool emergencyStop = false;

// Telemetry
float batteryVoltage = 0.0;
uint8_t systemStatus = 0;

// Connection tracking
bool bleConnected = false;
String connectedDeviceAddress = "";

// ============== FUNCTION PROTOTYPES ==============
void processIncomingByte(uint8_t inByte);
bool validateChecksum();
void handlePacket();
void updateDrive();
void updateServos();
void setMotor(int speed);
void handleButton(uint8_t buttonId, uint8_t pressed);
void safetyStop();
void sendTelemetry(bool viaBle, bool viaWifi);
void sendHeartbeatAck(uint8_t seqHigh, uint8_t seqLow, bool viaBle, bool viaWifi);
void sendCapabilities(bool viaBle, bool viaWifi);
void setupWiFi();
void handleWiFiPackets();
void blinkLed(int times, int delayMs);

// ============== SETUP ==============
void setup() {
  Serial.begin(115200);
  while (!Serial && millis() < 3000); // Wait up to 3s for Serial
  
  Serial.println("====================================");
  Serial.println("Ardunakon R4 WiFi Controller v2.2");
  Serial.println("====================================");

  // Initialize GPIO pins - Single motor (ESC or DC motor driver)
  pinMode(MOTOR_PWM, OUTPUT);
  pinMode(MOTOR_DIR1, OUTPUT);
  pinMode(MOTOR_DIR2, OUTPUT);
  pinMode(LED_STATUS, OUTPUT);
  pinMode(BUZZER_PIN, OUTPUT);  // Buzzer on pin 11

  // Initialize motor to stopped
  safetyStop();

  // Initialize Servos (centered position)
  servoX.attach(SERVO_X_PIN);
  servoY.attach(SERVO_Y_PIN);
  servoX.write(90); // Center position
  servoY.write(90); // Center position
  Serial.println("[OK] Servos initialized (centered at 90°)");

  // Initialize BLE
  if (!BLE.begin()) {
    Serial.println("[ERROR] BLE initialization failed!");
    // Continue anyway - WiFi might work
  } else {
    // Set BLE device name (appears in scan results)
    BLE.setLocalName("ArdunakonR4");
    BLE.setDeviceName("ArdunakonR4");
    
    // Setup primary service (ArduinoBLE default)
    primaryService.addCharacteristic(primaryTxChar);
    primaryService.addCharacteristic(primaryRxChar);
    BLE.addService(primaryService);
    
    // Setup HM-10 compatible service (fallback for clone modules)
    hm10Service.addCharacteristic(hm10TxChar);
    hm10Service.addCharacteristic(hm10RxChar);
    BLE.addService(hm10Service);
    
    // Set advertised service
    BLE.setAdvertisedService(primaryService);
    
    // Start advertising
    BLE.advertise();
    
    Serial.println("[OK] BLE initialized - Advertising as 'ArdunakonR4'");
    Serial.print("     Primary Service: ");
    Serial.println(PRIMARY_SERVICE_UUID);
    Serial.print("     HM-10 Service:   ");
    Serial.println(HM10_SERVICE_UUID);
  }

  // Initialize WiFi (optional)
  #if WIFI_ENABLED
  setupWiFi();
  #else
  Serial.println("[INFO] WiFi disabled (set WIFI_ENABLED=true to enable)");
  #endif

  // Ready indication
  blinkLed(3, 200);
  tone(BUZZER_PIN, 1000, 100);
  
  Serial.println("====================================");
  Serial.println("Ready! Waiting for connection...");
  Serial.println("====================================");
}

// ============== MAIN LOOP ==============
void loop() {
  // -------- BLE Handling --------
  BLEDevice central = BLE.central();

  if (central) {
    if (!bleConnected) {
      // New connection
      bleConnected = true;
      connectedDeviceAddress = central.address();
      Serial.print("[BLE] Connected: ");
      Serial.println(connectedDeviceAddress);
      digitalWrite(LED_STATUS, HIGH);
      
      // Send capabilities on connect
      delay(100); // Small delay for stable connection
      sendCapabilities(true, false);
    }

    // Process incoming data from both characteristic pairs
    if (primaryRxChar.written()) {
      int len = primaryRxChar.valueLength();
      const uint8_t* data = primaryRxChar.value();
      for (int i = 0; i < len; i++) {
        processIncomingByte(data[i]);
      }
    }
    
    if (hm10RxChar.written()) {
      int len = hm10RxChar.valueLength();
      const uint8_t* data = hm10RxChar.value();
      for (int i = 0; i < len; i++) {
        processIncomingByte(data[i]);
      }
    }

  } else if (bleConnected) {
    // Disconnection detected
    bleConnected = false;
    Serial.println("[BLE] Disconnected");
    digitalWrite(LED_STATUS, LOW);
    safetyStop();
    connectedDeviceAddress = "";
  }

  // -------- WiFi Handling --------
  #if WIFI_ENABLED
  handleWiFiPackets();
  
  // Periodic WiFi reconnection check
  if (millis() - lastWifiCheckTime > 10000) {
    lastWifiCheckTime = millis();
    if (WiFi.status() != WL_CONNECTED) {
      Serial.println("[WiFi] Connection lost, reconnecting...");
      setupWiFi();
    }
  }
  #endif

  // -------- Periodic Telemetry --------
  if (millis() - lastHeartbeatTime > 4000) {
    sendTelemetry(bleConnected, wifiConnected);
    lastHeartbeatTime = millis();
  }

  // -------- Safety Timeout --------
  // If no packets received for 2 seconds, stop motors
  if (millis() - lastPacketTime > 2000 && lastPacketTime > 0) {
    safetyStop();
  }
}

// ============== PACKET PROCESSING ==============
void processIncomingByte(uint8_t inByte) {
  // Wait for START_BYTE
  if (bufferIndex == 0 && inByte != START_BYTE) {
    return;
  }
  
  packetBuffer[bufferIndex++] = inByte;

  // Check for complete packet
  if (bufferIndex >= PACKET_SIZE) {
    if (packetBuffer[PACKET_SIZE - 1] == END_BYTE && validateChecksum()) {
      handlePacket();
    } else {
      Serial.println("[WARN] Invalid packet (checksum/end byte)");
    }
    bufferIndex = 0; // Reset buffer
  }
}

bool validateChecksum() {
  uint8_t xorCheck = 0;
  for (int i = 1; i <= 7; i++) {
    xorCheck ^= packetBuffer[i];
  }
  return (xorCheck == packetBuffer[8]);
}

void handlePacket() {
  lastPacketTime = millis();
  uint8_t cmd = packetBuffer[2];

  switch (cmd) {
    case CMD_JOYSTICK:
      // Map 0-200 range to -100 to +100
      leftX = map(packetBuffer[3], 0, 200, -100, 100);  // Steering
      leftY = map(packetBuffer[4], 0, 200, -100, 100);  // Throttle
      rightX = map(packetBuffer[5], 0, 200, -100, 100); // Servo X (A/L)
      rightY = map(packetBuffer[6], 0, 200, -100, 100); // Servo Y (W/R)
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
      sendHeartbeatAck(packetBuffer[3], packetBuffer[4], bleConnected, wifiConnected);
      break;

    case CMD_ESTOP:
      emergencyStop = true;
      safetyStop();
      Serial.println("[E-STOP] ACTIVATED!");
      tone(BUZZER_PIN, 2000, 500);
      break;
  }
}

// ============== MOTOR CONTROL ==============
// Single motor/ESC controlled by left joystick Y-axis (throttle)
// IMPORTANT: Deadzone prevents motor from running due to joystick drift
void updateDrive() {
  // Apply deadzone - values below MOTOR_DEADZONE are treated as zero
  // This prevents the motor from running when joystick is at center
  int throttle = leftY;
  
  if (abs(throttle) < MOTOR_DEADZONE) {
    throttle = 0; // Kill small values to prevent unintended movement
  }

  // Clamp to valid range
  throttle = constrain(throttle, -100, 100);

  // Set motor speed
  setMotor(throttle);
}

void setMotor(int speed) {
  // Apply deadzone one more time for safety
  if (abs(speed) < MOTOR_DEADZONE) {
    speed = 0;
  }
  
  if (speed > 0) {
    // Forward
    digitalWrite(MOTOR_DIR1, HIGH);
    digitalWrite(MOTOR_DIR2, LOW);
    analogWrite(MOTOR_PWM, map(speed, 0, 100, 0, 255));
  } else if (speed < 0) {
    // Reverse
    digitalWrite(MOTOR_DIR1, LOW);
    digitalWrite(MOTOR_DIR2, HIGH);
    analogWrite(MOTOR_PWM, map(-speed, 0, 100, 0, 255));
  } else {
    // Stop (brake mode) - CRITICAL: Ensure motor is off
    digitalWrite(MOTOR_DIR1, LOW);
    digitalWrite(MOTOR_DIR2, LOW);
    analogWrite(MOTOR_PWM, 0);
  }
}

// ============== SERVO CONTROL ==============
void updateServos() {
  // Map -100..+100 to 0..180 degrees
  // Center (0) = 90 degrees
  int angleX = map(rightX, -100, 100, 0, 180);
  int angleY = map(rightY, -100, 100, 0, 180);
  
  servoX.write(angleX);
  servoY.write(angleY);
}

// ============== BUTTON HANDLING ==============
void handleButton(uint8_t buttonId, uint8_t pressed) {
  Serial.print("[BTN] Button ");
  Serial.print(buttonId);
  Serial.print(" = ");
  Serial.println(pressed ? "PRESSED" : "RELEASED");
  
  // Button 1 = Clear E-Stop
  if (buttonId == 1 && pressed == 1) {
    emergencyStop = false;
    Serial.println("[E-STOP] CLEARED");
    tone(BUZZER_PIN, 500, 200);
  }
  
  // Add custom button handling here
  // Example: buttonId 2 = Toggle LED
  // if (buttonId == 2 && pressed == 1) {
  //   digitalWrite(SOME_PIN, !digitalRead(SOME_PIN));
  // }
}

// ============== SAFETY ==============
void safetyStop() {
  // Stop motor immediately - CRITICAL for safety
  analogWrite(MOTOR_PWM, 0);
  digitalWrite(MOTOR_DIR1, LOW);
  digitalWrite(MOTOR_DIR2, LOW);
  
  // Reset ALL control state to neutral
  leftX = 0;
  leftY = 0;
  rightX = 0;
  rightY = 0;
  
  // Reset servos to center
  servoX.write(90);
  servoY.write(90);
}

// ============== TELEMETRY ==============
void sendTelemetry(bool viaBle, bool viaWifi) {
  // Read battery voltage (assuming 3:1 voltage divider for 12V batteries)
  int rawValue = analogRead(BATTERY_PIN);
  batteryVoltage = (rawValue / 1023.0) * 3.3 * 3.0; // R4 is 3.3V ADC

  // Build telemetry packet
  uint8_t telemetry[PACKET_SIZE];
  telemetry[0] = START_BYTE;
  telemetry[1] = 0x01; // Device ID
  telemetry[2] = CMD_HEARTBEAT;
  telemetry[3] = (uint8_t)(batteryVoltage * 10); // Battery in 0.1V units
  telemetry[4] = emergencyStop ? 1 : 0;
  telemetry[5] = WiFi.status() == WL_CONNECTED ? 1 : 0; // WiFi status
  telemetry[6] = 0; // Reserved
  telemetry[7] = 0; // Reserved
  
  // Calculate checksum
  uint8_t xorCheck = 0;
  for (int i = 1; i <= 7; i++) {
    xorCheck ^= telemetry[i];
  }
  telemetry[8] = xorCheck;
  telemetry[9] = END_BYTE;

  // Send via BLE
  if (viaBle) {
    primaryTxChar.writeValue(telemetry, PACKET_SIZE);
    hm10TxChar.writeValue(telemetry, PACKET_SIZE);
  }

  // Send via WiFi UDP
  #if WIFI_ENABLED
  if (viaWifi && wifiConnected) {
    // Broadcast telemetry
    udp.beginPacket(udp.remoteIP(), udp.remotePort());
    udp.write(telemetry, PACKET_SIZE);
    udp.endPacket();
  }
  #endif
}

void sendHeartbeatAck(uint8_t seqHigh, uint8_t seqLow, bool viaBle, bool viaWifi) {
  uint8_t ack[PACKET_SIZE];
  ack[0] = START_BYTE;
  ack[1] = 0x01;
  ack[2] = CMD_HEARTBEAT;
  ack[3] = seqHigh;
  ack[4] = seqLow;
  ack[5] = 0;
  ack[6] = 0;
  ack[7] = 0;
  
  uint8_t xorCheck = 0;
  for (int i = 1; i <= 7; i++) {
    xorCheck ^= ack[i];
  }
  ack[8] = xorCheck;
  ack[9] = END_BYTE;

  if (viaBle) {
    primaryTxChar.writeValue(ack, PACKET_SIZE);
    hm10TxChar.writeValue(ack, PACKET_SIZE);
  }

  #if WIFI_ENABLED
  if (viaWifi && wifiConnected) {
    udp.beginPacket(udp.remoteIP(), udp.remotePort());
    udp.write(ack, PACKET_SIZE);
    udp.endPacket();
  }
  #endif
}

void sendCapabilities(bool viaBle, bool viaWifi) {
  uint8_t packet[PACKET_SIZE];
  packet[0] = START_BYTE;
  packet[1] = 0x01;
  packet[2] = CMD_ANNOUNCE_CAPABILITIES;
  
  // Capability byte 1: Hardware features
  packet[3] = CAP1_SERVO_X | CAP1_SERVO_Y | CAP1_MOTOR | CAP1_BUZZER | CAP1_BLE | CAP1_WIFI | CAP1_BATTERY;
  packet[4] = 0x00; // Capability byte 2: Modulino (none)
  packet[5] = BOARD_TYPE_R4_WIFI;
  packet[6] = 0x02; // Firmware version major
  packet[7] = 0x02; // Firmware version minor (v2.2)
  
  uint8_t xorCheck = 0;
  for (int i = 1; i <= 7; i++) {
    xorCheck ^= packet[i];
  }
  packet[8] = xorCheck;
  packet[9] = END_BYTE;

  if (viaBle) {
    primaryTxChar.writeValue(packet, PACKET_SIZE);
    hm10TxChar.writeValue(packet, PACKET_SIZE);
  }

  #if WIFI_ENABLED
  if (viaWifi && wifiConnected) {
    udp.beginPacket(udp.remoteIP(), udp.remotePort());
    udp.write(packet, PACKET_SIZE);
    udp.endPacket();
  }
  #endif

  Serial.println("[OK] Capabilities announced");
}

// ============== WIFI FUNCTIONS ==============
void setupWiFi() {
  #if WIFI_ENABLED
  Serial.print("[WiFi] Connecting to ");
  Serial.println(WIFI_SSID);

  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  
  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 20) {
    delay(500);
    Serial.print(".");
    attempts++;
  }
  
  if (WiFi.status() == WL_CONNECTED) {
    wifiConnected = true;
    Serial.println();
    Serial.print("[WiFi] Connected! IP: ");
    Serial.println(WiFi.localIP());
    
    // Start UDP listener
    udp.begin(UDP_PORT);
    Serial.print("[WiFi] UDP listening on port ");
    Serial.println(UDP_PORT);
  } else {
    wifiConnected = false;
    Serial.println();
    Serial.println("[WiFi] Connection failed!");
  }
  #endif
}

void handleWiFiPackets() {
  #if WIFI_ENABLED
  int packetSize = udp.parsePacket();
  if (packetSize > 0) {
    uint8_t buffer[32];
    int bytesRead = udp.read(buffer, min(packetSize, 32));
    
    for (int i = 0; i < bytesRead; i++) {
      processIncomingByte(buffer[i]);
    }
  }
  #endif
}

// ============== UTILITY FUNCTIONS ==============
void blinkLed(int times, int delayMs) {
  for (int i = 0; i < times; i++) {
    digitalWrite(LED_STATUS, HIGH);
    delay(delayMs);
    digitalWrite(LED_STATUS, LOW);
    if (i < times - 1) delay(delayMs);
  }
}