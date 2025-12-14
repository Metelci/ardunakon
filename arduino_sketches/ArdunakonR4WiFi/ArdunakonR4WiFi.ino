/*
 * Ardunakon - Arduino UNO R4 WiFi Sketch (Dual Mode: BLE + WiFi)
 *
 * Supports both Bluetooth BLE and WiFi connectivity
 * Designed for use with the Ardunakon Android Controller App
 *
 * Board: Arduino UNO R4 WiFi (Renesas RA4M1 + ESP32-S3)
 * Connectivity: BLE OR WiFi (compile-time selection)
 *
 * IMPORTANT: Set connection mode below before uploading!
 *
 * Pin Configuration:
 * - Motors: Pins 4-9 (L298N/BTS7960 compatible)
 * - Servos: Pin 2 (X-axis), Pin 12 (Y-axis)
 * - Battery Monitor: A0 (requires voltage divider for >5V batteries)
 * - Status LED: Pin 13 (built-in)
 * - Buzzer (optional): Pin 3
 *
 * v3.1 - Dual-mode support (BLE + WiFi)
 */

// ============================================
// CONNECTION MODE SELECTION
// ============================================
// Set to 1 for BLE mode, 0 for WiFi mode
#define USE_BLE_MODE 0  // CHANGE THIS: 1 = BLE, 0 = WiFi

// Debug Configuration (set to 0 for production builds)
#define DEBUG_SERIAL 1

#if DEBUG_SERIAL
  #define DEBUG_PRINT(x) Serial.print(x)
  #define DEBUG_PRINTLN(x) Serial.println(x)
#else
  #define DEBUG_PRINT(x)
  #define DEBUG_PRINTLN(x)
#endif

// ============================================
// INCLUDES (Mode-specific)
// ============================================
#if USE_BLE_MODE
  #include <ArduinoBLE.h>
  // BLE Service UUIDs (Nordic UART Service)
  #define SERVICE_UUID        "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
  #define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
  #define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"
#else
  #include <WiFiS3.h>
  #include <WiFiUdp.h>
  // WiFi Configuration - CHANGE THESE!
  const char* ssid = "METELCI";
  const char* password = "hly55ne305";
  const unsigned int localPort = 8888;
  const unsigned int discoveryPort = 8889;  // Discovery broadcast port
#endif

#include <Servo.h>
#include <ArdunakonProtocol.h>

// ============================================
// BOARD CONFIGURATION
// ============================================
#define BOARD_TYPE_R4_WIFI 0x02

// Capability Flags
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
  unsigned long lastDiscoveryBroadcast = 0;  // For WiFi discovery
#endif

// ============================================
// STATE VARIABLES
// ============================================
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

// Performance Optimization
#define LOOP_INTERVAL_MS 5  // 200Hz loop rate
unsigned long lastLoopTime = 0;
unsigned long lastLedUpdate = 0;

// ============================================
// SETUP
// ============================================
void setup() {
  Serial.begin(115200);
  delay(1000);

  #if USE_BLE_MODE
    Serial.println("Arduino UNO R4 WiFi - Ardunakon v3.1 (BLE Mode)");
  #else
    Serial.println("Arduino UNO R4 WiFi - Ardunakon v3.1 (WiFi Mode)");
  #endif

  // Initialize hardware
  initializeHardware();

  // Initialize connection (BLE or WiFi)
  #if USE_BLE_MODE
    initializeBLE();
  #else
    initializeWiFi();
  #endif

  DEBUG_PRINTLN("Ready for connections!");
}

// ============================================
// MAIN LOOP
// ============================================
void loop() {
  unsigned long now = millis();
  
  // Rate limit to 200Hz
  if (now - lastLoopTime < LOOP_INTERVAL_MS) {
    return;
  }
  lastLoopTime = now;

  #if USE_BLE_MODE
    handleBLEConnection(now);
  #else
    handleWiFiConnection(now);
    sendDiscoveryBroadcast(now);  // Broadcast presence for discovery
  #endif

  // Common tasks
  updateLED(now);
  sendTelemetryIfNeeded(now);
  checkSafetyTimeout(now);
}

// ============================================
// HARDWARE INITIALIZATION
// ============================================
void initializeHardware() {
  // Motor pins
  pinMode(MOTOR_LEFT_PWM, OUTPUT);
  pinMode(MOTOR_LEFT_DIR1, OUTPUT);
  pinMode(MOTOR_LEFT_DIR2, OUTPUT);
  pinMode(MOTOR_RIGHT_PWM, OUTPUT);
  pinMode(MOTOR_RIGHT_DIR1, OUTPUT);
  pinMode(MOTOR_RIGHT_DIR2, OUTPUT);
  pinMode(LED_STATUS, OUTPUT);
  pinMode(BUZZER_PIN, OUTPUT);

  // Servos
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
    DEBUG_PRINT("BLE initialization attempt ");
    DEBUG_PRINT(attempt);
    DEBUG_PRINTLN("/3...");
    
    delay(500 * attempt);
    
    if (BLE.begin()) {
      bleInitialized = true;
      DEBUG_PRINTLN("BLE started successfully!");
      break;
    }
    DEBUG_PRINTLN("BLE failed, retrying...");
  }

  if (!bleInitialized) {
    Serial.println("ERROR: BLE initialization failed!");
    Serial.println("Try updating Arduino R4 WiFi board package");
    while (1) {
      digitalWrite(LED_STATUS, !digitalRead(LED_STATUS));
      delay(200);
    }
  }

  BLE.setLocalName("ArdunakonR4");
  BLE.setDeviceName("ArdunakonR4");
  BLE.setConnectable(true);

  uartService.addCharacteristic(rxCharacteristic);
  uartService.addCharacteristic(txCharacteristic);
  BLE.addService(uartService);

  BLE.setAdvertisedServiceUuid(uartService.uuid());
  BLE.setAdvertisedService(uartService);
  
  if (BLE.advertise()) {
    DEBUG_PRINTLN("BLE advertising as 'ArdunakonR4'");
  } else {
    Serial.println("WARNING: BLE advertising failed!");
  }

  successBeep();
}

void handleBLEConnection(unsigned long now) {
  BLE.poll();
  BLEDevice central = BLE.central();

  if (central) {
    if (!isConnected) {
      isConnected = true;
      bufferIndex = 0;
      DEBUG_PRINT("BLE connected: ");
      DEBUG_PRINTLN(central.address());
      sendCapabilities();
    }

    if (rxCharacteristic.written()) {
      int len = rxCharacteristic.valueLength();
      const uint8_t* data = rxCharacteristic.value();
      
      int bytesToProcess = min(len, 10);
      for (int i = 0; i < bytesToProcess; i++) {
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

#endif // USE_BLE_MODE

// ============================================
// WIFI MODE FUNCTIONS
// ============================================
#if !USE_BLE_MODE

void initializeWiFi() {
  Serial.print("Connecting to WiFi: ");
  Serial.println(ssid);
  
  WiFi.begin(ssid, password);
  
  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 20) {
    delay(500);
    Serial.print(".");
    attempts++;
  }
  
  if (WiFi.status() == WL_CONNECTED) {
    isConnected = true;
    Serial.println("\nWiFi connected!");
    Serial.print("IP address: ");
    Serial.println(WiFi.localIP());
    
    udp.begin(localPort);
    Serial.print("UDP listening on port: ");
    Serial.println(localPort);
    
    successBeep();
  } else {
    Serial.println("\nERROR: WiFi connection failed!");
    Serial.println("Check SSID and password in sketch");
    while (1) {
      digitalWrite(LED_STATUS, !digitalRead(LED_STATUS));
      delay(500);
    }
  }
}

void handleWiFiConnection(unsigned long now) {
  int packetSize = udp.parsePacket();
  if (packetSize > 0) {
    clientIP = udp.remoteIP();
    clientPort = udp.remotePort();
    
    int firstByte = udp.peek();

    // Text-based discovery request from the Android app
    if (firstByte != ArdunakonProtocol::START_BYTE) {
      static char msgBuffer[128];
      int len = udp.read((uint8_t*)msgBuffer, min(packetSize, (int)sizeof(msgBuffer) - 1));
      if (len <= 0) return;
      msgBuffer[len] = '\0';

      String msg = String(msgBuffer);
      msg.trim();
      if (msg.startsWith("ARDUNAKON_DISCOVER")) {
        String response = "ARDUNAKON_DEVICE:ArdunakonR4";
        udp.beginPacket(clientIP, clientPort);
        udp.print(response);
        udp.endPacket();

        DEBUG_PRINT("Discovery request from ");
        DEBUG_PRINT(clientIP);
        DEBUG_PRINT(":");
        DEBUG_PRINT(clientPort);
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
}

void sendDataWiFi(const uint8_t* data, size_t len) {
  if (clientPort == 0) return;
  udp.beginPacket(clientIP, clientPort);
  udp.write(data, len);
  udp.endPacket();
}

void sendDiscoveryBroadcast(unsigned long now) {
  // Broadcast discovery packet every 2 seconds
  if (now - lastDiscoveryBroadcast < 2000) return;
  lastDiscoveryBroadcast = now;
  
  // Create discovery response matching Android app format: "ARDUNAKON_DEVICE:name"
  String discoveryMsg = "ARDUNAKON_DEVICE:ArdunakonR4";
  
  // Broadcast to subnet on port 8888 (same port Android sends discovery to)
  IPAddress broadcastIP = WiFi.localIP();
  broadcastIP[3] = 255;  // Set last octet to 255 for broadcast
  
  udp.beginPacket(broadcastIP, 8888);  // Changed from 8889 to 8888
  udp.print(discoveryMsg);
  udp.endPacket();
  
  DEBUG_PRINT("Discovery broadcast: ");
  DEBUG_PRINTLN(discoveryMsg);
}

#endif // !USE_BLE_MODE

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
      
      // Control Z-axis servo with auxBits (W=forward, B=backward)
      if (auxBits & ArdunakonProtocol::AUX_W) {
        rightZ = 127;  // Max forward
      } else if (auxBits & ArdunakonProtocol::AUX_B) {
        rightZ = -127;  // Max backward
      } else {
        rightZ = 0;  // Center
      }
      
      if (!emergencyStop) {
        updateDrive();
        updateServos();
      }
      break;

    case ArdunakonProtocol::CMD_BUTTON:
      // Button data would be in auxBits
      break;

    case ArdunakonProtocol::CMD_HEARTBEAT:
      // Heartbeat acknowledged
      break;

    case ArdunakonProtocol::CMD_ESTOP:
      emergencyStop = !emergencyStop;  // Toggle E-Stop
      if (emergencyStop) {
        safetyStop();
        Serial.println("EMERGENCY STOP ACTIVATED");
      } else {
        DEBUG_PRINTLN("E-STOP CLEARED");
      }
      break;

    case ArdunakonProtocol::CMD_ANNOUNCE_CAPABILITIES:
      sendCapabilities();
      break;
  }
}

void updateDrive() {
  int8_t throttle = leftY;
  int8_t steering = leftX;

  int leftSpeed = throttle + steering;
  int rightSpeed = throttle - steering;

  leftSpeed = constrain(leftSpeed, -127, 127);
  rightSpeed = constrain(rightSpeed, -127, 127);

  setMotor(MOTOR_LEFT_PWM, MOTOR_LEFT_DIR1, MOTOR_LEFT_DIR2, leftSpeed);
  setMotor(MOTOR_RIGHT_PWM, MOTOR_RIGHT_DIR1, MOTOR_RIGHT_DIR2, rightSpeed);
}

void updateServos() {
  int servoXPos = map(rightX, -127, 127, 0, 180);
  int servoYPos = map(rightY, -127, 127, 0, 180);
  int servoZPos = map(rightZ, -127, 127, 0, 180);
  
  servoX.write(servoXPos);
  servoY.write(servoYPos);
  servoZ.write(servoZPos);
}

void setMotor(int pwmPin, int dir1Pin, int dir2Pin, int8_t speed) {
  if (speed > 0) {
    digitalWrite(dir1Pin, HIGH);
    digitalWrite(dir2Pin, LOW);
    analogWrite(pwmPin, abs(speed) * 2);
  } else if (speed < 0) {
    digitalWrite(dir1Pin, LOW);
    digitalWrite(dir2Pin, HIGH);
    analogWrite(pwmPin, abs(speed) * 2);
  } else {
    digitalWrite(dir1Pin, LOW);
    digitalWrite(dir2Pin, LOW);
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
  setMotor(MOTOR_LEFT_PWM, MOTOR_LEFT_DIR1, MOTOR_LEFT_DIR2, 0);
  setMotor(MOTOR_RIGHT_PWM, MOTOR_RIGHT_DIR1, MOTOR_RIGHT_DIR2, 0);
}

void updateLED(unsigned long now) {
  if (now - lastLedUpdate > 100) {
    digitalWrite(LED_STATUS, (now - lastPacketTime < 500) ? HIGH : LOW);
    lastLedUpdate = now;
  }
}

void sendTelemetryIfNeeded(unsigned long now) {
  if (now - lastHeartbeatTime > 4000) {
    sendTelemetry();
    lastHeartbeatTime = now;
  }
}

void checkSafetyTimeout(unsigned long now) {
  if (now - lastPacketTime > 2000) {
    safetyStop();
  }
}

void sendTelemetry() {
  batteryVoltage = analogRead(BATTERY_PIN) * (5.0 / 1023.0) * BATTERY_DIVIDER_RATIO;

  uint8_t response[10];
  uint8_t statusFlags = emergencyStop ? 0x01 : 0x00;
  protocol.formatTelemetry(
    response,
    BOARD_TYPE_R4_WIFI,
    batteryVoltage,
    statusFlags,
    packetsReceived
  );

  #if USE_BLE_MODE
    sendDataBLE(response, 10);
  #else
    sendDataWiFi(response, 10);
  #endif
}

void sendHeartbeatAck() {
  uint8_t response[10];
  // Create heartbeat response
  response[0] = ArdunakonProtocol::START_BYTE;
  response[1] = BOARD_TYPE_R4_WIFI;
  response[2] = ArdunakonProtocol::CMD_HEARTBEAT;
  response[3] = 0;
  response[4] = 0;
  response[5] = 0;
  response[6] = 0;
  response[7] = 0;
  protocol.createChecksum(response);
  response[9] = ArdunakonProtocol::END_BYTE;

  #if USE_BLE_MODE
    sendDataBLE(response, 10);
  #else
    sendDataWiFi(response, 10);
  #endif
}

void sendCapabilities() {
  uint8_t response[10];
  
  #if USE_BLE_MODE
    uint8_t cap1 = CAP1_SERVO_X | CAP1_SERVO_Y | CAP1_MOTOR | CAP1_BUZZER | CAP1_BLE;
  #else
    uint8_t cap1 = CAP1_SERVO_X | CAP1_SERVO_Y | CAP1_MOTOR | CAP1_BUZZER | CAP1_WIFI;
  #endif
  
  // Create capabilities announcement
  response[0] = ArdunakonProtocol::START_BYTE;
  response[1] = BOARD_TYPE_R4_WIFI;
  response[2] = ArdunakonProtocol::CMD_ANNOUNCE_CAPABILITIES;
  response[3] = cap1;
  response[4] = 0;  // cap2
  response[5] = 0;  // cap3
  response[6] = 0;  // cap4
  response[7] = 0;  // reserved
  protocol.createChecksum(response);
  response[9] = ArdunakonProtocol::END_BYTE;

  #if USE_BLE_MODE
    sendDataBLE(response, 10);
  #else
    sendDataWiFi(response, 10);
  #endif
  
  DEBUG_PRINTLN("Capabilities sent");
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
