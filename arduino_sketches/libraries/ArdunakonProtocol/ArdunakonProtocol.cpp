/*
 * ArdunakonProtocol.cpp - Implementation
 */

#include "ArdunakonProtocol.h"

// Map 0-200 raw byte to -100 to 100 signed int
int8_t ArdunakonProtocol::mapJoystickValue(uint8_t val) {
    // constrain input just in case
    if (val > 200) val = 200;
    return map(val, 0, 200, -100, 100);
}

// Validate XOR checksum
bool ArdunakonProtocol::validateChecksum(const uint8_t* buffer) {
    if (buffer[0] != START_BYTE || buffer[9] != END_BYTE) return false;
    
    uint8_t xor_check = 0;
    for (int i = 1; i <= 7; i++) {
        xor_check ^= buffer[i];
    }
    return (xor_check == buffer[8]);
}

// Create XOR checksum and place it in buffer[8]
void ArdunakonProtocol::createChecksum(uint8_t* buffer) {
    uint8_t xor_check = 0;
    for (int i = 1; i <= 7; i++) {
        xor_check ^= buffer[i];
    }
    buffer[8] = xor_check;
}

// Parse a raw 10-byte buffer into a ControlPacket struct
ArdunakonProtocol::ControlPacket ArdunakonProtocol::parsePacket(const uint8_t* buffer) {
    ControlPacket packet;
    packet.valid = validateChecksum(buffer);
    packet.cmd = 0;
    packet.leftX = 0;
    packet.leftY = 0;
    packet.rightX = 0;
    packet.rightY = 0;
    packet.rightZ = 0;
    packet.auxBits = 0;
    
    if (!packet.valid) {
        return packet;
    }

    packet.cmd = buffer[2];

    if (packet.cmd == CMD_JOYSTICK) {
        packet.leftX  = mapJoystickValue(buffer[3]);
        packet.leftY  = mapJoystickValue(buffer[4]);
        packet.rightX = mapJoystickValue(buffer[5]);
        packet.rightY = mapJoystickValue(buffer[6]);
        packet.auxBits = buffer[7];
    } else if (packet.cmd == CMD_BUTTON) {
        // For button command, D1 (buffer[3]) is button ID, D2 (buffer[4]) is state
        // We can map this to our structure or handle separately. 
        // For consistency with existing code, we might just expose raw bytes 
        // but simpler to use specific parsers. 
        // For now, mapping logic is specific to command type.
        
        // Let's stick to raw-ish mapping relative to the protocol spec:
        // D1, D2, D3, D4, D5 are buffer[3]..buffer[7]
        packet.leftX = buffer[3]; // Overloaded use
        packet.leftY = buffer[4]; // Overloaded use
        packet.auxBits = buffer[3]; // Commonly used for button bits too
    } else if (packet.cmd == CMD_SERVO_Z) {
        packet.rightZ = mapJoystickValue(buffer[3]);
    }
    
    return packet;
}

// Format a standardised telemetry packet
void ArdunakonProtocol::formatTelemetry(uint8_t* buffer, uint8_t deviceId, float voltage, uint8_t statusFlags, uint32_t packetsReceived) {
    buffer[0] = START_BYTE;
    buffer[1] = deviceId;
    buffer[2] = CMD_TELEMETRY;
    
    // Voltage * 10 (e.g. 7.4V -> 74)
    if (voltage > 25.0) voltage = 25.0; // clamp
    buffer[3] = (uint8_t)(voltage * 10);
    
    buffer[4] = statusFlags;
    
    // Simple Packets Received Counter (wrapping byte or lower byte)
     buffer[5] = (uint8_t)(packetsReceived & 0xFF);
    
    // Reserved / Spare
    buffer[6] = 0x00;
    buffer[7] = 0x00;
    
    createChecksum(buffer);
    buffer[9] = END_BYTE;
}
