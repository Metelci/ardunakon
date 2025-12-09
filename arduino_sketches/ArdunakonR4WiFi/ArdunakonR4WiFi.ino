// Ardunakon - Arduino UNO R4 WiFi Sketch
// Full support for Arduino UNO R4 WiFi with built-in BLE and WiFi
// Designed for use with the Ardunakon Android Controller App
//
// Board: Arduino UNO R4 WiFi (Renesas RA4M1 + ESP32-S3)
// Connectivity: 
//   - Bluetooth Low Energy (BLE) via ESP32-S3 module
//   - WiFi (UDP) via ESP32-S3 module
//
// Protocol: 10-byte binary packets at 20Hz
// [START, DEV_ID, CMD, D1, D2, D3, D4, D5, CHECKSUM, END]
//
// v2.3 - WiFi Support + UDP Discovery + Servo + Brushless ESC

#include <ArduinoBLE.h>
#include <Servo.h>
#include <Arduino_LED_Matrix.h>
#include <WiFiS3.h>
#include <WiFiUdp.h>

// -----------------------------------------------------------------------
//  WIFI CONFIGURATION - UPDATE THESE CREDENTIALS!
// -----------------------------------------------------------------------
#define SECRET_SSID "METELCI"      // <--- CHANGE THIS
#define SECRET_PASS "hly55ne305"  // <--- CHANGE THIS

char ssid[] = SECRET_SSID;
char pass[] = SECRET_PASS;

WiFiUDP Udp;
unsigned int localPort = 8888;  // Local port to listen on

ArduinoLEDMatrix matrix;

// ---------------------------------------------------------
// NEW ANIMATION FRAMES (Verified 8x12 Bitmaps)
// ---------------------------------------------------------

// Frame 1: Rock Hand UP
uint8_t rockHand[8][12] = {
  { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, 
  { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, // Finger tips
  { 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 1, 0 }, 
  { 0, 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0 }, 
  { 0, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0 }, // Knuckles
  { 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 0 }, // Palm
  { 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0 }, // Wrist
  { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }
};

// Frame 2: Rock Hand DOWN (Headbang effect)
uint8_t rockHandDown[8][12] = {
  { 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0 },
  { 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0 }, // Fingers moved down
  { 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0 }, 
  { 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0 }, 
  { 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0 },
  { 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0 }, 
  { 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0 }, 
  { 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0 }  
};

void playRockAnimation() {
  // Play the animation loop 10 times
  for (int cycle = 0; cycle < 15; cycle++) {
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
#define CMD_ANNOUNCE_CAPABILITIES 0x05

// Board Type
#define BOARD_TYPE_UNO_R4_WIFI 0x02

// Capability Flags (Byte 1)
// Bit 0: ServoX, Bit 1: ServoY, Bit 2: Motor/ESC, Bit 3: LED Matrix
// Bit 4: Buzzer, Bit 5: WiFi, Bit 6: BLE
#define CAP1_SERVO_X    0x01
#define CAP1_SERVO_Y    0x02
#define CAP1_MOTOR      0x04
#define CAP1_LED_MATRIX 0x08
#define CAP1_BUZZER     0x10
#define CAP1_WIFI       0x20
#define CAP1_BLE        0x40

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
bool isWifiConnected = false;

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
void sendCapabilities();
void checkWifi();

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
  
  // Play rock animation on startup
  playRockAnimation();
  
  // Configure ADC for UNO R4 WiFi (14-bit)
  analogReadResolution(14);

  // -------------------------------------------------------------------
  // Initialize BLE
  // -------------------------------------------------------------------
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

  // -------------------------------------------------------------------
  // Initialize WiFi
  // -------------------------------------------------------------------
  if (WiFi.status() == WL_NO_MODULE) {
    Serial.println("Communication with WiFi module failed!");
  } else {
    // Check firmware version
    String fv = WiFi.firmwareVersion();
    Serial.print("WiFi Firmware: ");
    Serial.println(fv);
    
    // Attempt to connect to WiFi
    Serial.print("Connecting to WiFi SSID: ");
    Serial.println(ssid);
    
    int status = WL_IDLE_STATUS;
    int attempts = 0;
    
    // Try to connect (with retries)
    while (status != WL_CONNECTED && attempts < 3) {
      Serial.print("Attempt ");
      Serial.print(attempts + 1);
      Serial.println("...");
      
      status = WiFi.begin(ssid, pass);
      
      // Wait for connection (up to 10 seconds per attempt)
      int waitCount = 0;
      while (WiFi.status() != WL_CONNECTED && waitCount < 20) {
        delay(500);
        Serial.print(".");
        waitCount++;
      }
      Serial.println();
      
      status = WiFi.status();
      attempts++;
    }
    
    if (WiFi.status() == WL_CONNECTED) {
      // Wait for DHCP to assign a valid IP (not 0.0.0.0)
      Serial.println("Connected! Waiting for DHCP...");
      int dhcpWait = 0;
      while (WiFi.localIP() == IPAddress(0, 0, 0, 0) && dhcpWait < 20) {
        delay(500);
        Serial.print(".");
        dhcpWait++;
      }
      Serial.println();
      
      IPAddress ip = WiFi.localIP();
      if (ip != IPAddress(0, 0, 0, 0)) {
        Serial.println("WiFi Connected!");
        Serial.print("IP Address: ");
        Serial.println(ip);
        Serial.print("Signal Strength (RSSI): ");
        Serial.print(WiFi.RSSI());
        Serial.println(" dBm");
        Udp.begin(localPort);
        Serial.print("UDP Listening on port ");
        Serial.println(localPort);
        isWifiConnected = true;
      } else {
        Serial.println("DHCP Failed - No valid IP address");
        isWifiConnected = false;
      }
    } else {
      Serial.print("WiFi Connection Failed. Status: ");
      Serial.println(WiFi.status());
      Serial.println("Check SSID/Password and try again.");
    }
  }

  Serial.println("Arduino UNO R4 WiFi - Ardunakon Controller v2.3");
  Serial.println("Ready for BLE or WiFi connection...");
  digitalWrite(LED_STATUS, LOW);
  
  // Ready blink
  for (int i = 0; i < 3; i++) {
    digitalWrite(LED_STATUS, HIGH); delay(200);
    digitalWrite(LED_STATUS, LOW); delay(200);
  }
}

void loop() {
  BLEDevice central = BLE.central();
  
  // -------------------------------------------------------------------
  // 1. BLE Handling
  // -------------------------------------------------------------------
  static bool bleWasConnected = false;  // Moved outside to persist across loop iterations
  
  if (central && central.connected()) {
    // Just connected?
    if (!bleWasConnected) {
       Serial.print("BLE Connected: ");
       Serial.println(central.address());
       digitalWrite(LED_STATUS, HIGH);
       sendCapabilities();
       bleWasConnected = true;
    }

    if (rxCharacteristicHm10.written()) handleIncoming(rxCharacteristicHm10);
    if (txCharacteristicHm10.written()) handleIncoming(txCharacteristicHm10);
    if (rxCharacteristicArduino.written()) handleIncoming(rxCharacteristicArduino);
    if (txCharacteristicArduino.written()) handleIncoming(txCharacteristicArduino);
  } else {
    // Reset flag when disconnected so reconnect triggers announcement
    if (bleWasConnected) {
      Serial.println("BLE Disconnected");
      digitalWrite(LED_STATUS, LOW);
      bleWasConnected = false;
    }
  }

  // -------------------------------------------------------------------
  // 2. WiFi UDP Handling
  // -------------------------------------------------------------------
  if (isWifiConnected) {
    int packetSize = Udp.parsePacket();
    if (packetSize) {
      // Receive Incoming UDP Packet
      char packetData[255]; // buffer to hold incoming packet
      int len = Udp.read(packetData, 255);
      if (len > 0) {
        packetData[len] = 0;
      }
      
      // Check for Discovery Command "ARDUNAKON_DISCOVER"
      // If found, reply with "ARDUNAKON_DEVICE:Name|Nonce|Sig"
      // Detailed security/nonce logic can be added, for now simple discovery:
      String incoming = String(packetData);
      if (incoming.startsWith("ARDUNAKON_DISCOVER")) {
        Serial.println("Discovery received! Sending reply...");
        
        // Prepare reply: ARDUNAKON_DEVICE:MyName
        // If we had a secure session key we would append |Nonce|Sig
        char reply[] = "ARDUNAKON_DEVICE:ArdunakonR4";
        
        Udp.beginPacket(Udp.remoteIP(), Udp.remotePort());
        Udp.write(reply);
        Udp.endPacket();
      } else {
        // Assume control packet if size matches
        // Note: UDP packets might arrive in chunks or whole. 
        // For simplicity assuming complete 10-byte packets or stream of bytes.
        // Better to feed bytes into processIncomingByte
        for(int i=0; i<len; i++) {
          processIncomingByte((uint8_t)packetData[i]);
        }
      }
      lastPacketTime = millis();
    }
  }

  // -------------------------------------------------------------------
  // 3. Common Tasks (Telemetry, Safety)
  // -------------------------------------------------------------------
  BLE.poll();

  if (millis() - lastTelemetryTime > 4000) {
    sendTelemetry();
    lastTelemetryTime = millis();
  }

  // Safety timeout
  if (millis() - lastPacketTime > 2000) {
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

  // Send via BLE if connected
  if (BLE.central() && BLE.central().connected()) {
    txCharacteristicHm10.writeValue(telemetry, PACKET_SIZE);
    txCharacteristicArduino.writeValue(telemetry, PACKET_SIZE);
  }
  
  // Send via udp if connected (Remote IP must be known, handled by reply logic typically)
  // For now, UDP telemetry is only sent as response to heartbeat or if we track last sender.
  // Enhancing to send to last known sender:
  if (isWifiConnected && Udp.remoteIP() != IPAddress(0,0,0,0)) {
     Udp.beginPacket(Udp.remoteIP(), Udp.remotePort());
     Udp.write(telemetry, PACKET_SIZE);
     Udp.endPacket();
  }
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

  if (BLE.central() && BLE.central().connected()) {
      txCharacteristicHm10.writeValue(ack, PACKET_SIZE);
      txCharacteristicArduino.writeValue(ack, PACKET_SIZE);
  }
  
  if (isWifiConnected && Udp.remoteIP() != IPAddress(0,0,0,0)) {
     Udp.beginPacket(Udp.remoteIP(), Udp.remotePort());
     Udp.write(ack, PACKET_SIZE);
     Udp.endPacket();
  }
}

/**
 * Send device capabilities announcement packet
 * Called when a client connects to inform the app of available features
 */
void sendCapabilities() {
  uint8_t packet[PACKET_SIZE];
  packet[0] = START_BYTE;
  packet[1] = 0x01; // Device ID
  packet[2] = CMD_ANNOUNCE_CAPABILITIES;
  
  // Capability Byte 1: Core hardware
  // ServoX | ServoY | Motor | Matrix | Buzzer | WiFi | BLE
  packet[3] = CAP1_SERVO_X | CAP1_SERVO_Y | CAP1_MOTOR | CAP1_LED_MATRIX | CAP1_BUZZER | CAP1_WIFI | CAP1_BLE;
  
  // Capability Byte 2: Modulino modules (none for this board)
  packet[4] = 0x00;
  
  // Board Type
  packet[5] = BOARD_TYPE_UNO_R4_WIFI;
  
  // Reserved
  packet[6] = 0x00;
  packet[7] = 0x00;
  
  // Calculate checksum (XOR of bytes 1-7)
  uint8_t xor_check = 0;
  for (int i = 1; i <= 7; i++) xor_check ^= packet[i];
  packet[8] = xor_check;
  packet[9] = END_BYTE;
  
  // Send on both BLE services
  if (BLE.central() && BLE.central().connected()) {
      txCharacteristicHm10.writeValue(packet, PACKET_SIZE);
      txCharacteristicArduino.writeValue(packet, PACKET_SIZE);
  }
  
  if (isWifiConnected && Udp.remoteIP() != IPAddress(0,0,0,0)) {
     Udp.beginPacket(Udp.remoteIP(), Udp.remotePort());
     Udp.write(packet, PACKET_SIZE);
     Udp.endPacket();
  }
  Serial.println("Capabilities announced to connected device");
}