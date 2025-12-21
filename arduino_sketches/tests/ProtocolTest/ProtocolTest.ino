/*
 * ProtocolTest.ino - AUnit tests for ArdunakonProtocol Library
 * 
 * Verifies:
 * 1. Checksum generation and validation
 * 2. Packet parsing logic
 * 3. Joystick value mapping
 * 4. Telemetry formatting
 *
 * Requires AUnit library (install via Library Manager)
 */

#include <AUnit.h>
#include <ArdunakonProtocol.h>

using namespace aunit;

ArdunakonProtocol protocol;

test(Protocol, Checksum) {
  uint8_t buffer[10] = {0xAA, 0x01, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x55};
  
  // Create checksum
  protocol.createChecksum(buffer);
  
  // XOR of 0x01 ^ 0x03 = 0x02
  assertEqual(buffer[8], (uint8_t)0x02);
  
  // Validate correct checksum
  assertTrue(protocol.validateChecksum(buffer));
  
  // Invalidate and check failure
  buffer[8] = 0xFF;
  assertFalse(protocol.validateChecksum(buffer));
}

test(Protocol, JoystickMapping) {
  // 0 -> -100
  assertEqual(protocol.mapJoystickValue(0), (int8_t)-100);
  
  // 100 -> 0
  assertEqual(protocol.mapJoystickValue(100), (int8_t)0);
  
  // 200 -> 100
  assertEqual(protocol.mapJoystickValue(200), (int8_t)100);
  
  // Clamp test
  assertEqual(protocol.mapJoystickValue(255), (int8_t)100);
}

test(Protocol, ParseJoystickPacket) {
  // [START, DEV, CMD_JOY, L_X, L_Y, R_X, R_Y, AUX, CRC, END]
  // L_X=0(-100), L_Y=200(100), R_X=100(0), R_Y=50(-50), AUX=0x05
  uint8_t buffer[10] = {0xAA, 0x01, 0x01, 0, 200, 100, 50, 0x05, 0x00, 0x55};
  protocol.createChecksum(buffer);
  
  ArdunakonProtocol::ControlPacket packet = protocol.parsePacket(buffer);
  
  assertTrue(packet.valid);
  assertEqual(packet.cmd, ArdunakonProtocol::CMD_JOYSTICK);
  assertEqual(packet.leftX, (int8_t)-100);
  assertEqual(packet.leftY, (int8_t)100);
  assertEqual(packet.rightX, (int8_t)0);
  assertEqual(packet.rightY, (int8_t)-50);
  assertEqual(packet.auxBits, (uint8_t)0x05);
}

test(Protocol, FormatTelemetry) {
  uint8_t buffer[10];
  // 7.4V, Status 0x01 (E-Stop), 123 packets
  protocol.formatTelemetry(buffer, 0x01, 7.4, 0x01, 123);
  
  assertEqual(buffer[0], ArdunakonProtocol::START_BYTE);
  assertEqual(buffer[1], (uint8_t)0x01);
  assertEqual(buffer[2], ArdunakonProtocol::CMD_TELEMETRY);
  assertEqual(buffer[3], (uint8_t)74); // 7.4 * 10
  assertEqual(buffer[4], (uint8_t)0x01);
  assertEqual(buffer[5], (uint8_t)123); // Packet count LSB
  assertEqual(buffer[9], ArdunakonProtocol::END_BYTE);
  
  assertTrue(protocol.validateChecksum(buffer));
}

void setup() {
  Serial.begin(115200);
  while(!Serial); // Wait for Serial
  
  Serial.println("Ardunakon Protocol Tests");
}

void loop() {
  TestRunner::run();
}
