/*
 * ArdunakonProtocol.h - Shared protocol library for Ardunakon
 * Handles packet parsing, validation, and telemetry formatting.
 */

#ifndef ArdunakonProtocol_h
#define ArdunakonProtocol_h

#include "Arduino.h"

class ArdunakonProtocol {
public:
    // Constants
    static const uint8_t START_BYTE = 0xAA;
    static const uint8_t END_BYTE = 0x55;
    static const uint8_t PACKET_SIZE = 10;
    
    // Commands
    static const uint8_t CMD_JOYSTICK  = 0x01;
    static const uint8_t CMD_BUTTON    = 0x02;
    static const uint8_t CMD_HEARTBEAT = 0x03;
    static const uint8_t CMD_ESTOP     = 0x04;
    static const uint8_t CMD_ANNOUNCE_CAPABILITIES = 0x05;
    static const uint8_t CMD_SERVO_Z  = 0x06;
    static const uint8_t CMD_TELEMETRY = 0x10; 

    // Aux Button Bits
    static const uint8_t AUX_W = 0x01; // Forward/Start
    static const uint8_t AUX_A = 0x02; // Alternate
    static const uint8_t AUX_L = 0x04; // Left
    static const uint8_t AUX_R = 0x08; // Right
    static const uint8_t AUX_B = 0x02; // Back (same as A on default layout)

    // Structures
    struct ControlPacket {
        uint8_t cmd;
        int8_t leftX;
        int8_t leftY;
        int8_t rightX;
        int8_t rightY;
        int8_t rightZ;
        uint8_t auxBits;
        bool valid;
    };

    // Methods
    ControlPacket parsePacket(const uint8_t* buffer);
    bool validateChecksum(const uint8_t* buffer);
    void formatTelemetry(uint8_t* buffer, uint8_t deviceId, float voltage, uint8_t statusFlags, uint32_t packetsReceived);
    void createChecksum(uint8_t* buffer);
    
    // Helper to map 0-200 range to -100 to 100
    int8_t mapJoystickValue(uint8_t val);
};

#endif
