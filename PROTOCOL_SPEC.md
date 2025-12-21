# Ardunakon Communication Protocol Specification

This document defines the binary protocol used by the Ardunakon Android app to communicate with Arduino-based devices over Bluetooth (Classic/BLE) and WiFi (UDP).

## Packet Structure

All standard control packets are fixed at 10 bytes.

| Offset | Field | Value / Description |
| :--- | :--- | :--- |
| 0 | START_BYTE | `0xAA` |
| 1 | DEV_ID | Device Identifier (Default: `0x01`) |
| 2 | CMD | Command Identifier |
| 3-7 | DATA | Command-specific payload (5 bytes) |
| 8 | CHECKSUM | XOR checksum of bytes 1 through 7 |
| 9 | END_BYTE | `0x55` |

### Checksum Calculation
```kotlin
fun calculateChecksum(packet: ByteArray): Byte {
    var xor: Byte = 0
    for (i in 1..7) xor = xor xor packet[i]
    return xor
}
```

---

## Commands

### `0x01`: CMD_JOYSTICK
Sent at 20Hz during active input.

| Byte Offset | Field | Range | Description |
| :--- | :--- | :--- | :--- |
| 3 | LEFT_X | 0-200 | Normalized: 100 is center |
| 4 | LEFT_Y | 0-200 | Normalized: 100 is center |
| 5 | RIGHT_X | 0-200 | Normalized: 100 is center (Servo X) |
| 6 | RIGHT_Y | 0-200 | Normalized: 100 is center (Servo Y) |
| 7 | AUX_BITS | Flags | Bitfield for auxiliary inputs |

**AUX_BITS Flags:**
- `0x01`: Servo Z + (W button)
- `0x02`: Servo Z - (B button)
- `0x04`: Left (L button)
- `0x08`: Right (R button)

### `0x02`: CMD_BUTTON
Sent when an auxiliary UI button is pressed.

| Byte Offset | Field | Description |
| :--- | :--- | :--- |
| 3 | BUTTON_ID | 0-3 |
| 4 | STATE | 1: Pressed, 0: Released |

### `0x03`: CMD_HEARTBEAT
Sent at 4s intervals to maintain link health.

| Byte Offset | Field | Description |
| :--- | :--- | :--- |
| 3-4 | SEQUENCE | 16-bit sequence number (Big-Endian) |
| 5-6 | UPTIME | 16-bit uptime fragment (Optional) |

---

## WiFi Encryption Handshake (Extended Format)

WiFi (UDP) connections use an extended packet format for the AES-GCM handshake.

### `0x10`: CMD_HANDSHAKE_REQUEST
Payload: 16-byte App Nonce. Total packet size: 21 bytes.

### `0x11`: CMD_HANDSHAKE_RESPONSE
Payload: 16-byte Device Nonce + 32-byte HMAC signature. Total packet size: 53 bytes.

### `0x12`: CMD_HANDSHAKE_COMPLETE
Standard 10-byte packet acknowledging successful session key negotiation.

---

## Safety Mechanisms

### `0x04`: CMD_ESTOP
Emergency Stop command. When received, the device must immediately stop all motors and enter a failsafe state. Transmissions are blocked by the app until a reset is performed.
