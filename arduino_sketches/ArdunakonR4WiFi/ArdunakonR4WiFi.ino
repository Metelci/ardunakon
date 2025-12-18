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
/*
  Ardunakon UNO R4 WiFi - WiFi (UDP) + RC Brushed ESC + 3x Servo (6 button: A Z W B L R)
  ESC (RC PWM): D9  (white=signal, black=GND, red=BEC NOT connected to Arduino)
  Servos:
    Servo1: D2   (A / Z)
    Servo2: D11  (W / B)
    Servo3: D12  (L / R)
  Connectivity:
    - `USE_BLE_MODE=1`: BLE (UUIDs/characteristics unchanged)
    - `USE_BLE_MODE=0`: WiFi (tries Station first using `arduino_secrets.h`, then falls back to AP)
  WiFi:
    - If connected but IP is 0.0.0.0 -> fallback to AP mode "ArdunakonR4"
    - UDP port: 8888
*/
// ============================================
// CONNECTION MODE SELECTION
#define CAP1_BLE        0x40
#define CAP1_WIFI       0x80
// Pin Definitions
#define MOTOR_LEFT_PWM    9
#define MOTOR_LEFT_DIR1   8
#define MOTOR_LEFT_DIR2   7
#define MOTOR_RIGHT_PWM   6
#define MOTOR_RIGHT_DIR1  5
#define MOTOR_RIGHT_DIR2  4
// Servos UPDATED as requested:
#define SERVO_X_PIN       2
#define SERVO_Y_PIN       11
#define SERVO_Z_PIN       12
#define BATTERY_PIN       A0
#define LED_STATUS        13
#define BUZZER_PIN        3
#define BATTERY_DIVIDER_RATIO 3.0
// ============================================
// PINS
// ============================================
#define ESC_PIN          9
#define SERVO1_PIN       2
#define SERVO2_PIN       11
#define SERVO3_PIN       12
#define LED_STATUS       13
// Optional telemetry (battery monitor on A0 with divider if needed)
#define BATTERY_PIN      A0
#define BATTERY_DIVIDER_RATIO 3.0
// ============================================
// ESC SETTINGS
// ============================================
#define ESC_MIN 1000
#define ESC_MID 1500
#define ESC_MAX 2000
#define ESC_DEADBAND_US       60
#define ESC_NEUTRAL_ARM_MS    1500
#define ESC_DIR_CHANGE_MS     1200
#define ESC_RAMP_STEP_US      4
#define ESC_RAMP_INTERVAL_MS  20
// ============================================
// SERVO BUTTON SETTINGS
// ============================================
#define SERVO_STEP_DEG            2
#define SERVO_UPDATE_INTERVAL_MS  40
// ============================================
// AUX BIT MAPPING (A Z W B L R)
// Adjust these masks to match your app's auxBits encoding.
// ============================================
#define AUX_A_MASK  0x01
#define AUX_Z_MASK  0x02
#define AUX_W_MASK  0x04
#define AUX_B_MASK  0x08
#define AUX_L_MASK  0x10
#define AUX_R_MASK  0x20
// ============================================
// GLOBAL OBJECTS
// ============================================
Servo servoX;
Servo servoY;
Servo servoZ;
ArdunakonProtocol protocol;
Servo esc;
Servo servo1;
Servo servo2;
Servo servo3;
ArdunakonProtocol protocol;
#if USE_BLE_MODE
  BLEService uartService(SERVICE_UUID);
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
unsigned long lastPacketTime = 0;
unsigned long lastHeartbeatTime = 0;
bool emergencyStop = false;
// ESC state
bool escReady = false;
unsigned long escNeutralSince = 0;
int escLastDir = 0;
unsigned long escDirChangeAt = 0;
int escTargetUs = ESC_MID;
int escCurrentUs = ESC_MID;
unsigned long lastRampUpdate = 0;
// Servo state
int servo1Pos = 90;
int servo2Pos = 90;
int servo3Pos = 90;
unsigned long lastServoStepAt = 0;
float batteryVoltage = 0.0;
uint32_t packetsReceived = 0;
bool isConnected = false;
#define CONTROL_TIMEOUT_MS 1500
#define LOOP_INTERVAL_MS 5
unsigned long lastLoopTime = 0;
unsigned long lastLedUpdate = 0;
// ============================================
// SETUP
  // UNO R4 ADC handling:
  analogReadResolution(12); // 0..4095
  Serial.println("========================================");
  Serial.println("Arduino UNO R4 WiFi - Ardunakon v3.3");
  Serial.println("========================================");
  Serial.println("========================================");
  Serial.println("UNO R4 WiFi (UDP/BLE) + RC ESC + 3 Servos");
  Serial.println("========================================");
  initializeHardware();
  #else
    Serial.println("Mode: WiFi (Auto-fallback)");
    initializeWiFiWithFallback();
  #endif
  Serial.println("Ready for connections!");
  Serial.println("========================================");
}
  #endif
  // ESC arm: keep neutral for 2s
  unsigned long t0 = millis();
  while (millis() - t0 < 2000) {
    esc.writeMicroseconds(ESC_MID);
    delay(20);
  }
  Serial.println("ESC armed (neutral)");
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
void loop() {
  unsigned long now = millis();
  updateEscRamp(now);
  if (now - lastLoopTime < LOOP_INTERVAL_MS) return;
  lastLoopTime = now;
  #if USE_BLE_MODE
    handleBLEConnection(now);
  #else
    handleWiFiConnection(now);
    sendDiscoveryBroadcast(now);
  #endif
  #if USE_BLE_MODE
    handleBLEConnection(now);
  #else
    handleWiFiSerialCommands();
    handleWiFiConnection(now);
    sendDiscoveryBroadcast(now);
  #endif
  updateLED(now);
  sendTelemetryIfNeeded(now);
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
void initializeHardware() {
  pinMode(LED_STATUS, OUTPUT);
  esc.attach(ESC_PIN);
  esc.writeMicroseconds(ESC_MID);
  servo1.attach(SERVO1_PIN);
  servo2.attach(SERVO2_PIN);
  servo3.attach(SERVO3_PIN);
  servo1.write(90);
  servo2.write(90);
  servo3.write(90);
}
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
  BLEDevice central = BLE.central();
  if (central) {
    if (!isConnected) {
      isConnected = true;
      bufferIndex = 0;
      DEBUG_PRINTLN("BLE connected");
      sendCapabilities();
    }
    if (!isConnected) {
      isConnected = true;
      bufferIndex = 0;
      emergencyStop = false;
      safetyStop();
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
    if (rxCharacteristic.written()) {
      int len = rxCharacteristic.valueLength();
      const uint8_t* data = rxCharacteristic.value();
      for (int i = 0; i < len; i++) {
        processIncomingByte(data[i]);
      }
    }
  } else if (isConnected) {
    isConnected = false;
    DEBUG_PRINTLN("BLE disconnected");
  return apMode ? IPAddress(192, 168, 4, 255) : IPAddress(255, 255, 255, 255);
}
static void resetActiveClient() {
  hasActiveClient = false;
  clientPort = 0;
}
static void restartUdp() {
  udp.stop();
  udp.begin(localPort);
  resetActiveClient();
}
static bool tryStartStation(bool verbose) {
  if (verbose) {
    Serial.println("\n[STA] Connecting...");
    Serial.print("    SSID: ");
    Serial.println(sta_ssid);
  }
  WiFi.disconnect();
  delay(250);
  WiFi.begin(sta_ssid, sta_password);
  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 20) {
    delay(500);
    if (verbose) Serial.print(".");
    attempts++;
  }
  if (verbose) Serial.println();
  if (WiFi.status() != WL_CONNECTED) {
    if (verbose) Serial.println("    ERROR: Could not connect");
    return false;
  }
  IPAddress ip = WiFi.localIP();
  if (isZeroIP(ip)) {
    if (verbose) Serial.print("    Waiting for DHCP...");
    ip = waitForLocalIP(5000);
    if (verbose) Serial.println();
  }
  if (isZeroIP(ip)) {
    if (verbose) Serial.println("    ERROR: DHCP failed (IP 0.0.0.0)");
    return false;
  }
  isAPMode = false;
  isConnected = true;
  restartUdp();
  if (verbose) {
    Serial.println("    SUCCESS: Station connected");
    Serial.print("    IP: ");
    Serial.println(ip);
    Serial.print("    UDP: ");
    Serial.println(localPort);
  }
  return true;
}
static bool startApMode(bool verbose) {
  if (verbose) {
    Serial.println("\n[AP] Starting...");
    Serial.print("    SSID: ");
    Serial.println(ap_ssid);
  }
  WiFi.disconnect();
  delay(250);
  int status = strlen(ap_password) > 0
    ? WiFi.beginAP(ap_ssid, ap_password)
    : WiFi.beginAP(ap_ssid);
  if (status != WL_AP_LISTENING) {
    Serial.println("    ERROR: AP creation failed!");
    while (1) { digitalWrite(LED_STATUS, !digitalRead(LED_STATUS)); delay(500); }
  }
  isAPMode = true;
  isConnected = true;
  restartUdp();
  if (verbose) {
    Serial.println("    SUCCESS: AP created");
    Serial.print("    IP: ");
    Serial.println(waitForLocalIP(3000));
    Serial.print("    Connect to: ");
    Serial.println(ap_ssid);
  }
  return true;
}
static void handleWiFiSerialCommands() {
  static char line[32];
  static uint8_t idx = 0;
  while (Serial.available() > 0) {
    char c = (char)Serial.read();
    if (c == '\r' || c == '\n') {
      if (idx == 0) continue;
      line[idx] = '\0';
      idx = 0;
      for (char* p = line; *p; p++) {
        if (*p >= 'a' && *p <= 'z') *p = (char)(*p - 'a' + 'A');
      }
      if (strcmp(line, "STA") == 0 || strcmp(line, "STATION") == 0) {
        if (!tryStartStation(true)) {
          Serial.println("STA failed; staying/returning to AP");
          startApMode(true);
        }
      } else if (strcmp(line, "AP") == 0) {
        startApMode(true);
      } else if (strcmp(line, "AUTO") == 0) {
        Serial.println("AUTO: re-running Station->AP fallback");
        initializeWiFiWithFallback();
      } else if (strcmp(line, "STATUS") == 0) {
        Serial.print("Mode: ");
        Serial.println(isAPMode ? "AP" : "STA");
        Serial.print("IP: ");
        Serial.println(WiFi.localIP());
        Serial.print("UDP: ");
        Serial.println(localPort);
      } else {
        Serial.println("Commands: STA | AP | AUTO | STATUS");
      }
    } else if (idx < sizeof(line) - 1) {
      line[idx++] = c;
    }
  }
}
void initializeWiFiWithFallback() {
  // Step 1: Try Station mode (connect to router)
  Serial.println("\n[1] Trying Station mode...");
    Serial.print("\n    Station mode failed (");
    Serial.print(stationFailReason ? stationFailReason : "unknown");
    Serial.println("). Switching to AP mode...");
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
    Serial.println(waitForLocalIP(3000));
    Serial.print("    Connect your phone to '");
    Serial.print(ap_ssid);
    Serial.println("' WiFi");
    startApMode(false);
  }
  // Start UDP on both modes
  udp.begin(localPort);
  Serial.print("\nUDP listening on port ");
  Serial.println(localPort);
  // Start UDP on both modes
  if (stationOk) restartUdp();
  Serial.print("\nUDP listening on port ");
  Serial.println(localPort);
  successBeep();
}
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
void handlePacket(const ArdunakonProtocol::ControlPacket& packet) {
  switch (packet.cmd) {
    case ArdunakonProtocol::CMD_JOYSTICK:
      if (!emergencyStop) {
        unsigned long now = millis();
        // ESC control uses leftY
        int desiredUs = joyToEscUs(packet.leftY);
        if (desiredUs == ESC_MID) {
          if (!escReady) {
            if (escNeutralSince == 0) escNeutralSince = now;
            if (now - escNeutralSince >= ESC_NEUTRAL_ARM_MS) escReady = true;
          }
          escTargetUs = ESC_MID;
          escLastDir = 0;
          escDirChangeAt = 0;
        } else if (escReady) {
          escNeutralSince = 0;
          int dir = (desiredUs > ESC_MID) ? +1 : -1;
          if (dir != escLastDir && escLastDir != 0) {
            if (escDirChangeAt == 0) escDirChangeAt = now;
            if (now - escDirChangeAt < ESC_DIR_CHANGE_MS) {
              escTargetUs = ESC_MID;
            } else {
              escDirChangeAt = 0;
              escLastDir = dir;
              escTargetUs = desiredUs;
            }
          } else {
            escLastDir = dir;
            escTargetUs = desiredUs;
          }
        } else {
          escNeutralSince = 0;
          escTargetUs = ESC_MID;
        }
        applyServoButtons(packet.auxBits);
      }
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
int joyToEscUs(int8_t joy) {
  int clamped = constrain((int)joy, -100, 100);
  int us = map(clamped, -100, 100, ESC_MIN, ESC_MAX);
  if (abs(us - ESC_MID) < ESC_DEADBAND_US) us = ESC_MID;
  return constrain(us, ESC_MIN, ESC_MAX);
}
void updateEscRamp(unsigned long now) {
  if (now - lastRampUpdate < ESC_RAMP_INTERVAL_MS) return;
  lastRampUpdate = now;
  if (escCurrentUs < escTargetUs) {
    escCurrentUs += ESC_RAMP_STEP_US;
    if (escCurrentUs > escTargetUs) escCurrentUs = escTargetUs;
  } else if (escCurrentUs > escTargetUs) {
    escCurrentUs -= ESC_RAMP_STEP_US;
    if (escCurrentUs < escTargetUs) escCurrentUs = escTargetUs;
  }
  escCurrentUs = constrain(escCurrentUs, ESC_MIN, ESC_MAX);
  esc.writeMicroseconds(escCurrentUs);
}
void applyServoButtons(uint8_t auxBits) {
  unsigned long now = millis();
  if (now - lastServoStepAt < SERVO_UPDATE_INTERVAL_MS) return;
  lastServoStepAt = now;
  if (auxBits & AUX_A_MASK) servo1Pos -= SERVO_STEP_DEG;
  if (auxBits & AUX_Z_MASK) servo1Pos += SERVO_STEP_DEG;
  if (auxBits & AUX_W_MASK) servo2Pos -= SERVO_STEP_DEG;
  if (auxBits & AUX_B_MASK) servo2Pos += SERVO_STEP_DEG;
  if (auxBits & AUX_L_MASK) servo3Pos -= SERVO_STEP_DEG;
  if (auxBits & AUX_R_MASK) servo3Pos += SERVO_STEP_DEG;
  servo1Pos = constrain(servo1Pos, 0, 180);
  servo2Pos = constrain(servo2Pos, 0, 180);
  servo3Pos = constrain(servo3Pos, 0, 180);
  servo1.write(servo1Pos);
  servo2.write(servo2Pos);
  servo3.write(servo3Pos);
}
void safetyStop() {
  escReady = false;
  escNeutralSince = 0;
  escLastDir = 0;
  escDirChangeAt = 0;
  escTargetUs = ESC_MID;
  escCurrentUs = ESC_MID;
  esc.writeMicroseconds(ESC_MID);
}
void updateLED(unsigned long now) {
  if (now - lastLedUpdate > 100) {
  }
}
void checkSafetyTimeout(unsigned long now) {
  if (now - lastPacketTime > 2000) safetyStop();
}
void checkSafetyTimeout(unsigned long now) {
  if (now - lastPacketTime > CONTROL_TIMEOUT_MS) escTargetUs = ESC_MID;
}
void sendTelemetry() {
  // UNO R4 ADC is set to 12-bit in setup(): 0..4095
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
void successBeep() {
  for (int i = 0; i < 3; i++) {
    digitalWrite(LED_STATUS, HIGH);
    delay(200);
    digitalWrite(LED_STATUS, LOW);
    delay(200);
  }
}
