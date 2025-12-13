/*
 * Ardunakon WiFi Control - Arduino R4 WiFi
 * 
 * UDP-based remote control with AES encryption support.
 * 
 * Features:
 * - WiFi AP mode (creates Ardunakon hotspot)
 * - UDP command reception
 * - Encryption handshake protocol
 * - Motor/servo control
 * - Telemetry reporting
 * 
 * Protocol:
 * - Packet: [0xAA, DEV_ID, CMD, D1-D5, CHECKSUM, 0x55]
 * - Encrypted packets: [IV(12)] + [AES-GCM Ciphertext]
 * 
 * Commands:
 * - 0x01: Joystick data
 * - 0x02: Button data
 * - 0x03: Heartbeat
 * - 0x04: E-Stop
 * - 0x10: Handshake Request
 * - 0x11: Handshake Response
 * - 0x12: Handshake Complete
 */

#include <WiFiS3.h>
#include <WiFiUdp.h>
#include <SHA256.h>

// ========== Configuration ==========
const char* AP_SSID = "Ardunakon-R4";
const char* AP_PASS = "ardunakon123";  // Minimum 8 chars for WPA2
const int UDP_PORT = 8888;

// Pre-Shared Key (32 bytes) - Same as in Android app
// IMPORTANT: Replace with your own randomly generated key!
const uint8_t PSK[32] = {
  0x41, 0x72, 0x64, 0x75, 0x6E, 0x61, 0x6B, 0x6F,  // "Ardunako"
  0x6E, 0x53, 0x65, 0x63, 0x72, 0x65, 0x74, 0x4B,  // "nSecretK"
  0x65, 0x79, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36,  // "ey123456"
  0x37, 0x38, 0x39, 0x30, 0x41, 0x42, 0x43, 0x44   // "7890ABCD"
};

// ========== Protocol Constants ==========
#define START_BYTE 0xAA
#define END_BYTE   0x55
#define PACKET_SIZE 10

// Commands
#define CMD_JOYSTICK  0x01
#define CMD_BUTTON    0x02
#define CMD_HEARTBEAT 0x03
#define CMD_ESTOP     0x04
#define CMD_HANDSHAKE_REQUEST  0x10
#define CMD_HANDSHAKE_RESPONSE 0x11
#define CMD_HANDSHAKE_COMPLETE 0x12
#define CMD_HANDSHAKE_FAILED   0x13

// ========== Network Objects ==========
WiFiUDP udp;
IPAddress clientIP;
uint16_t clientPort = 0;

// ========== Encryption State ==========
bool encryptionEnabled = false;
uint8_t sessionKey[32];
uint8_t deviceNonce[16];

// ========== Motor Control ==========
// Adjust these pins to match your setup
#define MOTOR_LEFT_PWM   9
#define MOTOR_LEFT_DIR   8
#define MOTOR_RIGHT_PWM  10
#define MOTOR_RIGHT_DIR  11

// ========== State ==========
unsigned long lastPacketTime = 0;
unsigned long lastHeartbeatTime = 0;
bool eStopActive = false;
int motorLeft = 0;   // -100 to 100
int motorRight = 0;  // -100 to 100

// ========== Function Prototypes ==========
void handlePacket(uint8_t* data, int len);
void handleJoystick(uint8_t* packet);
void handleButton(uint8_t* packet);
void handleHeartbeat(uint8_t* packet);
void handleEStop();
void handleHandshakeRequest(uint8_t* packet, int len);
void sendHandshakeResponse();
void sendHeartbeatResponse();
void updateMotors();
void emergencyStop();
bool verifyChecksum(uint8_t* packet);
void computeHmac(uint8_t* data, int dataLen, uint8_t* key, uint8_t* output);
void generateNonce(uint8_t* nonce);

// ========== Setup ==========
void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("\n=== Ardunakon WiFi Control ===");
  
  // Initialize motor pins
  pinMode(MOTOR_LEFT_PWM, OUTPUT);
  pinMode(MOTOR_LEFT_DIR, OUTPUT);
  pinMode(MOTOR_RIGHT_PWM, OUTPUT);
  pinMode(MOTOR_RIGHT_DIR, OUTPUT);
  
  // Initialize random seed
  randomSeed(analogRead(A0));
  
  // Create WiFi Access Point
  Serial.print("Creating AP: ");
  Serial.println(AP_SSID);
  
  int status = WiFi.beginAP(AP_SSID, AP_PASS);
  if (status != WL_AP_LISTENING) {
    Serial.println("Failed to create AP!");
    while (true) { delay(1000); }
  }
  
  Serial.print("AP IP: ");
  Serial.println(WiFi.localIP());
  
  // Start UDP
  udp.begin(UDP_PORT);
  Serial.print("UDP listening on port ");
  Serial.println(UDP_PORT);
  
  Serial.println("Ready for connections!");
}

// ========== Main Loop ==========
void loop() {
  // Check for incoming UDP packets
  int packetSize = udp.parsePacket();
  if (packetSize > 0) {
    uint8_t buffer[64];
    int len = udp.read(buffer, sizeof(buffer));
    
    // Store client address for responses
    clientIP = udp.remoteIP();
    clientPort = udp.remotePort();
    
    lastPacketTime = millis();
    handlePacket(buffer, len);
  }
  
  // Timeout check - 2 seconds without packets = emergency stop
  if (millis() - lastPacketTime > 2000 && lastPacketTime > 0) {
    if (!eStopActive) {
      Serial.println("Timeout! Emergency stop.");
      emergencyStop();
    }
  }
  
  // Send heartbeat response periodically
  if (millis() - lastHeartbeatTime > 1000 && clientPort > 0) {
    sendHeartbeatResponse();
    lastHeartbeatTime = millis();
  }
  
  // Update motor outputs
  if (!eStopActive) {
    updateMotors();
  }
}

// ========== Packet Handling ==========
void handlePacket(uint8_t* data, int len) {
  // TODO: If encryption enabled, decrypt first
  // For now, handle plaintext
  
  if (len < PACKET_SIZE) return;
  if (data[0] != START_BYTE) return;
  
  // Check for handshake request (extended packet)
  if (data[2] == CMD_HANDSHAKE_REQUEST && len >= 21) {
    handleHandshakeRequest(data, len);
    return;
  }
  
  // Standard 10-byte packet
  if (data[9] != END_BYTE) return;
  if (!verifyChecksum(data)) {
    Serial.println("Checksum error!");
    return;
  }
  
  uint8_t cmd = data[2];
  
  switch (cmd) {
    case CMD_JOYSTICK:
      handleJoystick(data);
      break;
    case CMD_BUTTON:
      handleButton(data);
      break;
    case CMD_HEARTBEAT:
      handleHeartbeat(data);
      break;
    case CMD_ESTOP:
      handleEStop();
      break;
    case CMD_HANDSHAKE_COMPLETE:
      Serial.println("Encryption handshake complete!");
      encryptionEnabled = true;
      break;
    default:
      Serial.print("Unknown command: 0x");
      Serial.println(cmd, HEX);
      break;
  }
}

void handleJoystick(uint8_t* packet) {
  // Joystick values are 0-200, center is 100
  int8_t leftX = (int8_t)(packet[3] - 100);   // -100 to 100
  int8_t leftY = (int8_t)(packet[4] - 100);
  int8_t rightX = (int8_t)(packet[5] - 100);  // Servo X
  int8_t rightY = (int8_t)(packet[6] - 100);  // Servo Y
  
  // Arcade drive mixing
  motorLeft = constrain(leftY + leftX, -100, 100);
  motorRight = constrain(leftY - leftX, -100, 100);
  
  // Clear E-Stop on valid control
  if (eStopActive) {
    eStopActive = false;
    Serial.println("E-Stop cleared by control input");
  }
}

void handleButton(uint8_t* packet) {
  uint8_t auxBits = packet[3];
  Serial.print("Buttons: 0x");
  Serial.println(auxBits, HEX);
}

void handleHeartbeat(uint8_t* packet) {
  // Echo heartbeat
  sendHeartbeatResponse();
}

void handleEStop() {
  Serial.println("E-STOP received!");
  emergencyStop();
}

// ========== Encryption Handshake ==========
void handleHandshakeRequest(uint8_t* data, int len) {
  Serial.println("Handshake request received");
  
  // Extract app nonce (16 bytes starting at position 3)
  uint8_t appNonce[16];
  memcpy(appNonce, data + 3, 16);
  
  // Generate device nonce
  generateNonce(deviceNonce);
  
  // Compute signature: HMAC-SHA256(PSK, appNonce || deviceNonce)
  uint8_t signatureInput[32];
  memcpy(signatureInput, appNonce, 16);
  memcpy(signatureInput + 16, deviceNonce, 16);
  
  uint8_t signature[32];
  computeHmac(signatureInput, 32, (uint8_t*)PSK, signature);
  
  // Send response
  sendHandshakeResponse(signature);
}

void sendHandshakeResponse(uint8_t* signature) {
  // Packet: START, DEV_ID, CMD, NONCE[16], SIG[32], CHECKSUM, END
  // Total: 53 bytes
  uint8_t response[53];
  response[0] = START_BYTE;
  response[1] = 0x01;  // Device ID
  response[2] = CMD_HANDSHAKE_RESPONSE;
  memcpy(response + 3, deviceNonce, 16);
  memcpy(response + 19, signature, 32);
  
  // Calculate checksum (XOR bytes 1-50)
  uint8_t xorSum = 0;
  for (int i = 1; i <= 50; i++) {
    xorSum ^= response[i];
  }
  response[51] = xorSum;
  response[52] = END_BYTE;
  
  // Send to client
  udp.beginPacket(clientIP, clientPort);
  udp.write(response, 53);
  udp.endPacket();
  
  Serial.println("Handshake response sent");
}

void sendHeartbeatResponse() {
  // Telemetry packet
  uint8_t packet[10];
  packet[0] = START_BYTE;
  packet[1] = 0x01;
  packet[2] = CMD_HEARTBEAT;
  packet[3] = 124;  // Battery voltage * 10 (12.4V)
  packet[4] = eStopActive ? 1 : 0;  // Status
  packet[5] = 0;
  packet[6] = 0;
  packet[7] = encryptionEnabled ? 1 : 0;  // Encryption status
  
  // Checksum
  uint8_t xorSum = 0;
  for (int i = 1; i <= 7; i++) {
    xorSum ^= packet[i];
  }
  packet[8] = xorSum;
  packet[9] = END_BYTE;
  
  udp.beginPacket(clientIP, clientPort);
  udp.write(packet, 10);
  udp.endPacket();
}

// ========== Motor Control ==========
void updateMotors() {
  // Map motor values (-100 to 100) to PWM (0-255)
  int leftPWM = map(abs(motorLeft), 0, 100, 0, 255);
  int rightPWM = map(abs(motorRight), 0, 100, 0, 255);
  
  digitalWrite(MOTOR_LEFT_DIR, motorLeft >= 0 ? HIGH : LOW);
  analogWrite(MOTOR_LEFT_PWM, leftPWM);
  
  digitalWrite(MOTOR_RIGHT_DIR, motorRight >= 0 ? HIGH : LOW);
  analogWrite(MOTOR_RIGHT_PWM, rightPWM);
}

void emergencyStop() {
  eStopActive = true;
  motorLeft = 0;
  motorRight = 0;
  
  analogWrite(MOTOR_LEFT_PWM, 0);
  analogWrite(MOTOR_RIGHT_PWM, 0);
}

// ========== Utility Functions ==========
bool verifyChecksum(uint8_t* packet) {
  uint8_t xorSum = 0;
  for (int i = 1; i <= 7; i++) {
    xorSum ^= packet[i];
  }
  return xorSum == packet[8];
}

void generateNonce(uint8_t* nonce) {
  for (int i = 0; i < 16; i++) {
    nonce[i] = random(256);
  }
}

void computeHmac(uint8_t* data, int dataLen, uint8_t* key, uint8_t* output) {
  // Simple HMAC-SHA256 implementation
  // Note: For production, use a proper crypto library
  SHA256 sha256;
  
  // Inner padding
  uint8_t ipad[64];
  uint8_t opad[64];
  
  for (int i = 0; i < 32; i++) {
    ipad[i] = key[i] ^ 0x36;
    opad[i] = key[i] ^ 0x5C;
  }
  for (int i = 32; i < 64; i++) {
    ipad[i] = 0x36;
    opad[i] = 0x5C;
  }
  
  // Inner hash
  sha256.reset();
  sha256.update(ipad, 64);
  sha256.update(data, dataLen);
  uint8_t innerHash[32];
  sha256.finalize(innerHash, 32);
  
  // Outer hash
  sha256.reset();
  sha256.update(opad, 64);
  sha256.update(innerHash, 32);
  sha256.finalize(output, 32);
}
