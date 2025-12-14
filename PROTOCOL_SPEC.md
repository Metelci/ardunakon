# Ardunakon Protocol Specification v2.1

**Last Updated:** December 14, 2025  
**Status:** Stable  
**Compatibility:** All Arduino sketches v2.0+

---

## Overview

The Ardunakon Protocol is a compact binary protocol for low-latency bidirectional communication between the Android controller app and Arduino boards. It supports both Bluetooth (Classic/BLE) and WiFi transports.

**Key Features:**
- 10-byte fixed packet size (standard commands)
- XOR checksum validation
- Command-based extensibility
- 20Hz transmission rate (50ms intervals)
- Support for joysticks, buttons, telemetry, and encryption handshake

---

## Packet Structure

###  Standard Packet (10 bytes)

```
┌──────┬────────┬─────┬────┬────┬────┬────┬────┬──────────┬─────┐
│START │ DEV_ID │ CMD │ D1 │ D2 │ D3 │ D4 │ D5 │ CHECKSUM │ END │
├──────┼────────┼─────┼────┼────┼────┼────┼────┼──────────┼─────┤
│ 0xAA │  0x01  │ ... │... │... │... │... │... │   XOR    │0x55 │
└──────┴────────┴─────┴────┴────┴────┴────┴────┴──────────┴─────┘
Byte:  0      1       2     3    4    5    6    7       8        9
```

**Field Descriptions:**

| Byte | Field | Type | Description |
|------|-------|------|-------------|
| 0 | START_BYTE | `0xAA` | Packet start marker |
| 1 | DEV_ID | `uint8` | Device ID (default: `0x01`) |
| 2 | CMD | `uint8` | Command code (see below) |
| 3-7 | DATA[0-4] | `uint8[5]` | Command-specific payload |
| 8 | CHECKSUM | `uint8` | XOR of bytes 1-7 |
| 9 | END_BYTE | `0x55` | Packet end marker |

---

## Commands

### 0x01 - Joystick Control

**Direction:** App → Arduino  
**Purpose:** Send joystick positions for motor/servo control

**Payload:**
```
D1 = leftX   (0-200, mapped from -1.0 to +1.0)
D2 = leftY   (0-200, mapped from -1.0 to +1.0)
D3 = rightX  (0-200, mapped from -1.0 to +1.0)
D4 = rightY  (0-200, mapped from -1.0 to +1.0)
D5 = auxBits (bitmask for auxiliary buttons)
```

**Value Mapping:**
- `0` = -1.0 (minimum)
- `100` = 0.0 (center)
- `200` = +1.0 (maximum)

**AuxBits Flags:**
```
Bit 0 (0x01) = W (Forward/Start)
Bit 1 (0x02) = A/B (Alternate/Back)
Bit 2 (0x04) = L (Left)
Bit 3 (0x08) = R (Right)
Bits 4-7     = Reserved
```

**Example:** Full forward throttle, half-left steering
```
AA 01 01 96 C8 64 64 00 XOR 55
       │  │  │  │  │  └─ auxBits = 0
       │  │  │  │  └──── rightY = 100 (center)
       │  │  │  └─────── rightX = 100 (center)
       │  │  └────────── leftY = 200 (max forward)
       │  └───────────── leftX = 150 (half left)
       └──────────────── CMD_JOYSTICK
```

---

### 0x06 - Servo Z Control

**Direction:** App  Arduino  
**Purpose:** Set the 3rd servo position (Z-axis / auxiliary servo)

**Payload:**
```
D1 = servoZ (0-200, mapped from -1.0 to +1.0)
D2-D5 = Reserved (0)
```

**Value Mapping:**
- `0` = -1.0 (minimum)
- `100` = 0.0 (center)
- `200` = +1.0 (maximum)

---

### 0x02 - Button Press

**Direction:** App → Arduino  
**Purpose:** Send discrete button events

**Payload:**
```
D1 = buttonId (1-255)
D2 = pressed  (0 = released, 1 = pressed)
D3 = Reserved (0)
D4 = Reserved (0)
D5 = Reserved (0)
```

**Example:** Button 1 pressed (emergency stop reset)
```
AA 01 02 01 01 00 00 00 XOR 55
```

---

### 0x03 - Heartbeat

**Direction:** Bidirectional  
**Purpose:** Keep-alive ping and connection quality monitoring

**Payload (App → Arduino):**
```
D1 = Sequence High Byte
D2 = Sequence Low Byte
D3 = Uptime High Byte (optional)
D4 = Uptime Low Byte (optional)
D5 = Reserved (0)
```

**Payload (Arduino → App):**
```
D1 = Echo Sequence High Byte
D2 = Echo Sequence Low Byte
D3-D5 = Reserved (0)
```

**Frequency:** Every 4 seconds  
**Timeout:** 2 seconds (no heartbeat = disconnect)  
**Missed ACK Threshold:**
- Classic BT: 3 missed ACKs
- BLE: 5 missed ACKs

---

### 0x04 - Emergency Stop

**Direction:** App → Arduino  
**Purpose:** Immediately halt all actuators

**Payload:**
```
D1-D5 = All zeros (0x00)
```

**Arduino Behavior:**
- Stop all motors (PWM = 0)
- Center all servos (90°)
- Set emergency stop flag
- Block further joystick commands until reset

**Reset Method:** Send Button command (ID=1, pressed=1)

---

### 0x05 - Announce Capabilities

**Direction:** Arduino → App  
**Purpose:** Announce board type and available hardware

**Payload:**
```
D1 = Capability Flags 1
D2 = Capability Flags 2 (Modulino)
D3 = Board Type
D4-D5 = Reserved (0)
```

**Capability Flags 1:**
```
Bit 0 (0x01) = Servo X supported
Bit 1 (0x02) = Servo Y supported
Bit 2 (0x04) = Motor driver supported
Bit 3 (0x08) = Reserved
Bit 4 (0x10) = Buzzer supported
Bit 5 (0x20) = Reserved
Bit 6 (0x40) = BLE supported
Bit 7 (0x80) = Reserved
```

**Board Types:**
```
0x00 = Unknown/Custom
0x01 = Arduino UNO (Classic)
0x02 = Arduino UNO R4 WiFi
0x03 = Arduino UNO Q
0x04 = Custom/DIY
```

**Example:** UNO R4 WiFi with servos, motors, buzzer, BLE
```
AA 01 05 57 00 02 00 00 XOR 55
       │  │  │  │
       │  │  │  └─ Board Type = 0x02 (R4 WiFi)
       │  │  └──── Modulino flags = 0x00
       │  └─────── Caps = 0x57 (0x01|0x02|0x04|0x10|0x40)
       └────────── CMD_ANNOUNCE_CAPABILITIES
```

---

### 0x10 - Telemetry (Arduino → App)

**Direction:** Arduino → App  
**Purpose:** Send battery voltage, status, and packet stats

**Payload:**
```
D1 = Device ID
D2 = Battery Voltage Integer Part
D3 = Battery Voltage Fraction (0-99)
D4 = Status Flags
D5 = Packets Received (low byte)
```

**Status Flags:**
```
Bit 0 (0x01) = Emergency Stop Active
Bits 1-7     = Reserved
```

**Voltage Encoding:**
```
voltage = D2 + (D3 / 100.0)
Example: D2=12, D3=50 → 12.50V
```

**Frequency:** Every 4 seconds

---

## Extended Packets (WiFi Encryption)

### 0x10 - Handshake Request (21 bytes)

**Direction:** App → Arduino  
**Purpose:** Initiate WiFi encryption handshake

**Structure:**
```
┌──────┬────────┬─────┬──────────────────┬──────────┬─────┐
│START │ DEV_ID │ CMD │   NONCE[16]      │ CHECKSUM │ END │
├──────┼────────┼─────┼──────────────────┼──────────┼─────┤
│ 0xAA │  0x01  │0x10 │  Random 16 bytes │   XOR    │0x55 │
└──────┴────────┴─────┴──────────────────┴──────────┴─────┘
Byte: 0      1      2    3-18              19        20
```

---

### 0x11 - Handshake Response (53 bytes)

**Direction:** Arduino → App  
**Purpose:** Respond with device nonce and signature

**Structure:**
```
┌──────┬────────┬─────┬──────────┬──────────────────┬──────────┬─────┐
│START │ DEV_ID │ CMD │ NONCE[16]│  SIGNATURE[32]   │ CHECKSUM │ END │
├──────┼────────┼─────┼──────────┼──────────────────┼──────────┼─────┤
│ 0xAA │  0x01  │0x11 │ Arduino  │ HMAC-SHA256      │   XOR    │0x55 │
│      │        │     │  Nonce   │  Signature       │          │     │
└──────┴────────┴─────┴──────────┴──────────────────┴──────────┴─────┘
Byte: 0      1      2    3-18       19-50            51        52
```

---

### 0x12 - Handshake Complete (10 bytes)

**Direction:** App → Arduino  
**Purpose:** Acknowledge successful handshake

**Payload:** All zeros (standard 10-byte packet)

---

## Checksum Calculation

**Algorithm:** Simple XOR

```c
uint8_t calculateChecksum(uint8_t* packet) {
    uint8_t xor = 0;
    for (int i = 1; i <= 7; i++) {  // XOR bytes 1-7
        xor ^= packet[i];
    }
    return xor;
}
```

**Validation:**
```c
bool validateChecksum(uint8_t* packet) {
    return packet[8] == calculateChecksum(packet);
}
```

---

## Transmission Parameters

| Parameter | Value | Notes |
|-----------|-------|-------|
| Packet Rate | 20Hz (50ms) | App → Arduino joystick data |
| Telemetry Rate | 0.25Hz (4s) | Arduino → App status |
| Heartbeat Interval | 4s | Bidirectional keep-alive |
| Connection Timeout | 2s | No packets = disconnect |
| Max Queue Size | 100 packets | BLE write queue limit |

---

## Error Handling

### Invalid Packets

**Detection:**
- START_BYTE ≠ 0xAA → Discard
- END_BYTE ≠ 0x55 → Discard
- Invalid checksum → Discard
- Packet size mismatch → Discard (extended packets)

**Recovery:**
- Reset packet buffer index
- Wait for next START_BYTE

### Connection Loss

**Symptoms:**
- No heartbeat ACK for 2 seconds
- Missed ACK count exceeds threshold

**Arduino Behavior:**
- Execute safety stop (motors off, servos center)
- Clear packet buffer
- Maintain E-Stop if active

**App Behavior:**
- Mark connection as ERROR state
- Trigger auto-reconnect if enabled
- Clear pending write queue

---

## Implementation Examples

### Arduino (C++)

```cpp
#include <ArdunakonProtocol.h>

ArdunakonProtocol protocol;
uint8_t buffer[10];

void loop() {
    if (Serial.available() >= 10) {
        Serial.readBytes(buffer, 10);
        
        if (protocol.validateChecksum(buffer)) {
            ControlPacket pkt = protocol.parsePacket(buffer);
            if (pkt.cmd == CMD_JOYSTICK) {
                updateMotors(pkt.leftX, pkt.leftY);
                updateServos(pkt.rightX, pkt.rightY);
            }
        }
    }
}
```

### Android (Kotlin)

```kotlin
fun sendJoystickData(leftX: Float, leftY: Float, rightX: Float, rightY: Float) {
    val packet = ProtocolManager.formatJoystickData(
        leftX, leftY, rightX, rightY, auxBits = 0
    )
    bluetoothManager.sendData(packet)
}
```

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 2.1 | 2025-12 | Added capability announcement, improved heartbeat |
| 2.0 | 2025-11 | Arcade drive mixing, WiFi encryption handshake |
| 1.0 | 2024-10 | Initial protocol definition |

---

## References

- **Arduino Library:** `arduino_sketches/libraries/ArdunakonProtocol/`
- **Android Implementation:** `app/src/main/java/com/metelci/ardunakon/protocol/ProtocolManager.kt`
- **Test Specification:** `app/src/test/java/com/metelci/ardunakon/protocol/ProtocolTest.kt`
