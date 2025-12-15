/*
 * Ardunakon - Arduino UNO R4 WiFi Sketch (Dual Mode: BLE + WiFi)
 *
 * Supports both Bluetooth BLE and WiFi connectivity
 * WiFi supports AUTOMATIC mode switching:
 *   - First tries to connect to your home WiFi (Station mode)
 *   - If that fails, creates its own WiFi network (AP mode)
 *
 * Board: Arduino UNO R4 WiFi (Renesas RA4M1 + ESP32-S3)
 *
 * Pin Configuration:
 * - Motors: Pins 4-9 (L298N/BTS7960 compatible)
 * - Servos: Pin 2 (X-axis), Pin 12 (Y-axis), A1 (Z-axis)
 * - Battery Monitor: A0 (requires voltage divider for >5V batteries)
 * - Status LED: Pin 13 (built-in)
 * - Buzzer (optional): Pin 3
 *
 * v3.3 - Automatic WiFi mode fallback (Station → AP)
 */

// ============================================
// CONNECTION MODE SELECTION
// ============================================
#define USE_BLE_MODE 0  // 1 = BLE only, 0 = WiFi (with auto-fallback)

// Debug Configuration
#define DEBUG_SERIAL 1

#if DEBUG_SERIAL
  #define DEBUG_PRINT(x) Serial.print(x)
  #define DEBUG_PRINTLN(x) Serial.println(x)
#else
  #define DEBUG_PRINT(x)
  #define DEBUG_PRINTLN(x)
#endif

// ============================================
// INCLUDES
// ============================================
#if USE_BLE_MODE
  #include <ArduinoBLE.h>
  #define SERVICE_UUID        "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
  #define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
  #define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"
#else
  #include <WiFiS3.h>
  #include <WiFiUdp.h>
  
  // ============================================
  // WIFI CONFIGURATION - CHANGE THESE!
  // ============================================
  
  // Your home WiFi credentials (Station mode - primary)
  const char* sta_ssid = "METELCI";
  const char* sta_password = "hly55ne305";
  
  // Fallback AP settings (if router not available)
  const char* ap_ssid = "ArdunakonR4";
  const char* ap_password = "";  // Open network
  
  const unsigned int localPort = 8888;
#endif

#include <Servo.h>
#include <ArdunakonProtocol.h>

// ============================================
// BOARD CONFIGURATION
// ============================================
#define BOARD_TYPE_R4_WIFI 0x02

#define CAP1_SERVO_X    0x01
#define CAP1_SERVO_Y    0x02
#define CAP1_MOTOR      0x04
#define CAP1_BUZZER     0x10
#define CAP1_BLE        0x40
#define CAP1_WIFI       0x80

// Pin Definitions
#define MOTOR_LEFT_PWM    9
#define MOTOR_LEFT_DIR1   8
#define MOTOR_LEFT_DIR2   7
#define MOTOR_RIGHT_PWM   6
#define MOTOR_RIGHT_DIR1  5
#define MOTOR_RIGHT_DIR2  4

#define SERVO_X_PIN       2
#define SERVO_Y_PIN       12
#define SERVO_Z_PIN       A1

#define BATTERY_PIN       A0
#define LED_STATUS        13
#define BUZZER_PIN        3

#define BATTERY_DIVIDER_RATIO 3.0

// ============================================
// GLOBAL OBJECTS
// ============================================
Servo servoX;
Servo servoY;
Servo servoZ;
ArdunakonProtocol protocol;

#if USE_BLE_MODE
  BLEService uartService(SERVICE_UUID);
  BLECharacteristic rxCharacteristic(CHARACTERISTIC_UUID_RX, BLEWrite | BLEWriteWithoutResponse, 20);
  BLECharacteristic txCharacteristic(CHARACTERISTIC_UUID_TX, BLERead | BLENotify, 20);
#else
  WiFiUDP udp;
  IPAddress clientIP;
  unsigned int clientPort = 0;
  unsigned long lastDiscoveryBroadcast = 0;
  bool hasActiveClient = false;
  unsigned long lastClientActivity = 0;
  bool isAPMode = false;  // Track current WiFi mode
#endif

// State Variables
uint8_t packetBuffer[ArdunakonProtocol::PACKET_SIZE];
uint8_t bufferIndex = 0;
unsigned long lastPacketTime = 0;
unsigned long lastHeartbeatTime = 0;

int8_t leftX = 0, leftY = 0;
int8_t rightX = 0, rightY = 0, rightZ = 0;
uint8_t auxBits = 0;
bool emergencyStop = false;

float batteryVoltage = 0.0;
uint32_t packetsReceived = 0;
bool isConnected = false;

#define LOOP_INTERVAL_MS 5
unsigned long lastLoopTime = 0;
unsigned long lastLedUpdate = 0;

// ============================================
// SETUP
// ============================================
void setup() {
  Serial.begin(115200);
  delay(1000);

  Serial.println("========================================");
  Serial.println("Arduino UNO R4 WiFi - Ardunakon v3.3");
  Serial.println("========================================");

  initializeHardware();

  #if USE_BLE_MODE
    Serial.println("Mode: BLE");
    initializeBLE();
  #else
    Serial.println("Mode: WiFi (Auto-fallback)");
    initializeWiFiWithFallback();
  #endif

  Serial.println("Ready for connections!");
  Serial.println("========================================");
}

// ============================================
// MAIN LOOP
// ============================================
void loop() {
  unsigned long now = millis();
  
  if (now - lastLoopTime < LOOP_INTERVAL_MS) return;
  lastLoopTime = now;

  #if USE_BLE_MODE
    handleBLEConnection(now);
  #else
    handleWiFiConnection(now);
    sendDiscoveryBroadcast(now);
  #endif

  updateLED(now);
  sendTelemetryIfNeeded(now);
  checkSafetyTimeout(now);
}

// ============================================
// HARDWARE INITIALIZATION
// ============================================
void initializeHardware() {
  pinMode(MOTOR_LEFT_PWM, OUTPUT);
  pinMode(MOTOR_LEFT_DIR1, OUTPUT);
  pinMode(MOTOR_LEFT_DIR2, OUTPUT);
  pinMode(MOTOR_RIGHT_PWM, OUTPUT);
  pinMode(MOTOR_RIGHT_DIR1, OUTPUT);
  pinMode(MOTOR_RIGHT_DIR2, OUTPUT);
  pinMode(LED_STATUS, OUTPUT);
  pinMode(BUZZER_PIN, OUTPUT);

  servoX.attach(SERVO_X_PIN);
  servoY.attach(SERVO_Y_PIN);
  servoZ.attach(SERVO_Z_PIN);
  servoX.write(90);
  servoY.write(90);
  servoZ.write(90);
}

// ============================================
// BLE MODE FUNCTIONS
// ============================================
#if USE_BLE_MODE

void initializeBLE() {
  bool bleInitialized = false;
  for (int attempt = 1; attempt <= 3; attempt++) {
    DEBUG_PRINT("BLE init attempt ");
    DEBUG_PRINT(attempt);
    DEBUG_PRINTLN("/3...");
    delay(500 * attempt);
    
    if (BLE.begin()) {
      bleInitialized = true;
      DEBUG_PRINTLN("BLE started!");
      break;
    }
  }

  if (!bleInitialized) {
    Serial.println("ERROR: BLE failed!");
    while (1) { digitalWrite(LED_STATUS, !digitalRead(LED_STATUS)); delay(200); }
  }

  BLE.setLocalName("ArdunakonR4");
  BLE.setDeviceName("ArdunakonR4");
  BLE.setConnectable(true);

  uartService.addCharacteristic(rxCharacteristic);
  uartService.addCharacteristic(txCharacteristic);
  BLE.addService(uartService);
  BLE.setAdvertisedService(uartService);
  BLE.advertise();

  successBeep();
}

void handleBLEConnection(unsigned long now) {
  BLE.poll();
  BLEDevice central = BLE.central();

  if (central) {
    if (!isConnected) {
      isConnected = true;
      bufferIndex = 0;
      DEBUG_PRINTLN("BLE connected");
      sendCapabilities();
    }

    if (rxCharacteristic.written()) {
      int len = rxCharacteristic.valueLength();
      const uint8_t* data = rxCharacteristic.value();
      for (int i = 0; i < min(len, 10); i++) {
        processIncomingByte(data[i]);
      }
    }
  } else if (isConnected) {
    isConnected = false;
    DEBUG_PRINTLN("BLE disconnected");
    BLE.advertise();
    safetyStop();
  }
}

void sendDataBLE(const uint8_t* data, size_t len) {
  txCharacteristic.writeValue(data, len);
}

#endif

// ============================================
// WIFI MODE FUNCTIONS (WITH AUTO-FALLBACK)
// ============================================
#if !USE_BLE_MODE

void initializeWiFiWithFallback() {
  // Step 1: Try Station mode (connect to router)
  Serial.println("\n[1] Trying Station mode...");
  Serial.print("    SSID: ");
  Serial.println(sta_ssid);
  
  WiFi.begin(sta_ssid, sta_password);
  
  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 15) {
    delay(500);
    Serial.print(".");
    attempts++;
  }
  
  if (WiFi.status() == WL_CONNECTED) {
    // Station mode SUCCESS
    isAPMode = false;
    isConnected = true;
    Serial.println("\n    SUCCESS! Connected to router");
    Serial.print("    IP: ");
    Serial.println(WiFi.localIP());
  } else {
    // Step 2: Fallback to AP mode
    Serial.println("\n    Router not found. Switching to AP mode...");
    
    WiFi.disconnect();
    delay(500);
    
    Serial.println("\n[2] Creating Access Point...");
    Serial.print("    SSID: ");
    Serial.println(ap_ssid);
    
    int status = strlen(ap_password) > 0 
      ? WiFi.beginAP(ap_ssid, ap_password)
      : WiFi.beginAP(ap_ssid);
    
    if (status != WL_AP_LISTENING) {
      Serial.println("    ERROR: AP creation failed!");
      while (1) { digitalWrite(LED_STATUS, !digitalRead(LED_STATUS)); delay(500); }
    }
    
    isAPMode = true;
    isConnected = true;
    Serial.println("    SUCCESS! AP created");
    Serial.print("    IP: ");
    Serial.println(WiFi.localIP());
    Serial.println("    Connect your phone to 'ArdunakonR4' WiFi");
  }
  
  // Start UDP on both modes
  udp.begin(localPort);
  Serial.print("\nUDP listening on port ");
  Serial.println(localPort);
  
  successBeep();
}

void handleWiFiConnection(unsigned long now) {
  int packetSize = udp.parsePacket();
  if (packetSize > 0) {
    clientIP = udp.remoteIP();
    clientPort = udp.remotePort();
    hasActiveClient = true;
    lastClientActivity = now;
    
    int firstByte = udp.peek();

    // Text-based discovery request
    if (firstByte != ArdunakonProtocol::START_BYTE) {
      static char msgBuffer[128];
      int len = udp.read((uint8_t*)msgBuffer, min(packetSize, (int)sizeof(msgBuffer) - 1));
      if (len <= 0) return;
      msgBuffer[len] = '\0';

      String msg = String(msgBuffer);
      msg.trim();
      if (msg.startsWith("ARDUNAKON_DISCOVER")) {
        String response = isAPMode 
          ? "ARDUNAKON_DEVICE:ArdunakonR4 (AP)"
          : "ARDUNAKON_DEVICE:ArdunakonR4";
        udp.beginPacket(clientIP, clientPort);
        udp.print(response);
        udp.endPacket();

        DEBUG_PRINT("Discovery from ");
        DEBUG_PRINT(clientIP);
        DEBUG_PRINT(" -> ");
        DEBUG_PRINTLN(response);
      }
      return;
    }

    // Binary control packet
    int len = udp.read(packetBuffer, ArdunakonProtocol::PACKET_SIZE);
    for (int i = 0; i < len; i++) {
      processIncomingByte(packetBuffer[i]);
    }
  }
  
  // Client timeout
  if (hasActiveClient && (now - lastClientActivity > 30000)) {
    hasActiveClient = false;
    clientPort = 0;
    DEBUG_PRINTLN("Client timed out");
  }
}

void sendDataWiFi(const uint8_t* data, size_t len) {
  if (clientPort == 0) return;
  udp.beginPacket(clientIP, clientPort);
  udp.write(data, len);
  udp.endPacket();
}

void sendDiscoveryBroadcast(unsigned long now) {
  if (now - lastDiscoveryBroadcast < 2000) return;
  lastDiscoveryBroadcast = now;
  
  String discoveryMsg = isAPMode 
    ? "ARDUNAKON_DEVICE:ArdunakonR4 (AP)"
    : "ARDUNAKON_DEVICE:ArdunakonR4";
  
  IPAddress broadcastIP = isAPMode
    ? IPAddress(192, 168, 4, 255)
    : IPAddress(WiFi.localIP()[0], WiFi.localIP()[1], WiFi.localIP()[2], 255);
  
  udp.beginPacket(broadcastIP, 8888);
  udp.print(discoveryMsg);
  udp.endPacket();
  
  DEBUG_PRINT("Broadcast: ");
  DEBUG_PRINTLN(discoveryMsg);
}

#endif

// ============================================
// COMMON FUNCTIONS
// ============================================
void processIncomingByte(uint8_t byte) {
  if (bufferIndex == 0 && byte != ArdunakonProtocol::START_BYTE) return;
  packetBuffer[bufferIndex++] = byte;

  if (bufferIndex >= ArdunakonProtocol::PACKET_SIZE) {
    ArdunakonProtocol::ControlPacket packet = protocol.parsePacket(packetBuffer);
    if (packet.valid) {
      handlePacket(packet);
      packetsReceived++;
      lastPacketTime = millis();
    }
    bufferIndex = 0;
  }
}

void handlePacket(const ArdunakonProtocol::ControlPacket& packet) {
  switch (packet.cmd) {
    case ArdunakonProtocol::CMD_JOYSTICK:
      leftX = packet.leftX;
      leftY = packet.leftY;
      rightX = packet.rightX;
      rightY = packet.rightY;
      auxBits = packet.auxBits;
      
      if (auxBits & ArdunakonProtocol::AUX_W) rightZ = 127;
      else if (auxBits & ArdunakonProtocol::AUX_B) rightZ = -127;
      else rightZ = 0;
      
      if (!emergencyStop) { updateDrive(); updateServos(); }
      break;

    case ArdunakonProtocol::CMD_HEARTBEAT:
      sendHeartbeatAck();
      break;

    case ArdunakonProtocol::CMD_ESTOP:
      emergencyStop = !emergencyStop;
      if (emergencyStop) { safetyStop(); Serial.println("E-STOP!"); }
      else DEBUG_PRINTLN("E-STOP cleared");
      break;

    case ArdunakonProtocol::CMD_ANNOUNCE_CAPABILITIES:
      sendCapabilities();
      break;
  }
}

void updateDrive() {
  int leftSpeed = constrain(leftY + leftX, -127, 127);
  int rightSpeed = constrain(leftY - leftX, -127, 127);
  setMotor(MOTOR_LEFT_PWM, MOTOR_LEFT_DIR1, MOTOR_LEFT_DIR2, leftSpeed);
  setMotor(MOTOR_RIGHT_PWM, MOTOR_RIGHT_DIR1, MOTOR_RIGHT_DIR2, rightSpeed);
}

void updateServos() {
  servoX.write(map(rightX, -127, 127, 0, 180));
  servoY.write(map(rightY, -127, 127, 0, 180));
  servoZ.write(map(rightZ, -127, 127, 0, 180));
}

void setMotor(int pwmPin, int dir1Pin, int dir2Pin, int8_t speed) {
  if (speed > 0) { digitalWrite(dir1Pin, HIGH); digitalWrite(dir2Pin, LOW); }
  else if (speed < 0) { digitalWrite(dir1Pin, LOW); digitalWrite(dir2Pin, HIGH); }
  else { digitalWrite(dir1Pin, LOW); digitalWrite(dir2Pin, LOW); }
  analogWrite(pwmPin, abs(speed) * 2);
}

void safetyStop() {
  setMotor(MOTOR_LEFT_PWM, MOTOR_LEFT_DIR1, MOTOR_LEFT_DIR2, 0);
  setMotor(MOTOR_RIGHT_PWM, MOTOR_RIGHT_DIR1, MOTOR_RIGHT_DIR2, 0);
}

void updateLED(unsigned long now) {
  if (now - lastLedUpdate > 100) {
    #if !USE_BLE_MODE
      if (hasActiveClient) digitalWrite(LED_STATUS, (now - lastPacketTime < 500) ? HIGH : LOW);
      else digitalWrite(LED_STATUS, (now / 1000) % 2);
    #else
      digitalWrite(LED_STATUS, (now - lastPacketTime < 500) ? HIGH : LOW);
    #endif
    lastLedUpdate = now;
  }
}

void sendTelemetryIfNeeded(unsigned long now) {
  if (now - lastHeartbeatTime > 2000) {
    sendTelemetry();
    lastHeartbeatTime = now;
  }
}

void checkSafetyTimeout(unsigned long now) {
  if (now - lastPacketTime > 2000) safetyStop();
}

void sendTelemetry() {
  batteryVoltage = analogRead(BATTERY_PIN) * (5.0 / 1023.0) * BATTERY_DIVIDER_RATIO;
  uint8_t response[10];
  protocol.formatTelemetry(response, BOARD_TYPE_R4_WIFI, batteryVoltage, emergencyStop ? 0x01 : 0x00, packetsReceived);
  #if USE_BLE_MODE
    sendDataBLE(response, 10);
  #else
    sendDataWiFi(response, 10);
  #endif
}

void sendHeartbeatAck() {
  uint8_t response[10] = {ArdunakonProtocol::START_BYTE, BOARD_TYPE_R4_WIFI, ArdunakonProtocol::CMD_HEARTBEAT, 0, 0, 0, 0, 0, 0, ArdunakonProtocol::END_BYTE};
  protocol.createChecksum(response);
  #if USE_BLE_MODE
    sendDataBLE(response, 10);
  #else
    sendDataWiFi(response, 10);
  #endif
}

void sendCapabilities() {
  uint8_t response[10];
  uint8_t cap1 = CAP1_SERVO_X | CAP1_SERVO_Y | CAP1_MOTOR | CAP1_BUZZER | (USE_BLE_MODE ? CAP1_BLE : CAP1_WIFI);
  response[0] = ArdunakonProtocol::START_BYTE;
  response[1] = BOARD_TYPE_R4_WIFI;
  response[2] = ArdunakonProtocol::CMD_ANNOUNCE_CAPABILITIES;
  response[3] = cap1;
  response[4] = 0; response[5] = 0; response[6] = 0; response[7] = 0;
  protocol.createChecksum(response);
  response[9] = ArdunakonProtocol::END_BYTE;
  #if USE_BLE_MODE
    sendDataBLE(response, 10);
  #else
    sendDataWiFi(response, 10);
  #endif
}

void successBeep() {
  for (int i = 0; i < 3; i++) {
    digitalWrite(LED_STATUS, HIGH);
    tone(BUZZER_PIN, 1500, 100);
    delay(200);
    digitalWrite(LED_STATUS, LOW);
    delay(200);
  }
}
