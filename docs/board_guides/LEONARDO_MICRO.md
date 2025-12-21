# Arduino Leonardo / Micro Setup Guide

Setup for Arduino Leonardo and Micro with native USB and external Bluetooth.

## Overview

| Feature | Leonardo | Micro |
|---------|----------|-------|
| **MCU** | ATmega32U4 | ATmega32U4 |
| **USB** | Native USB | Native USB |
| **Voltage** | 5V | 5V |

---

## Key Difference: Native USB

Leonardo/Micro have native USB, so:
- `Serial` = USB connection (for debugging)
- `Serial1` = Hardware TX/RX pins (D1/D0) for Bluetooth

---

## Wiring Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                  Arduino Leonardo / Micro                    │
│  ┌─────────────────────────────────────────────────────┐    │
│  │                                                     │    │
│  │   D1 (TX) ●────────────────► RX (HC-05)             │    │
│  │   D0 (RX) ●◄─────────────── TX (HC-05)             │    │
│  │      GND ●◄─────────────── GND                     │    │
│  │       5V ●◄─────────────── VCC                     │    │
│  │                                                     │    │
│  │   D9 ●◄─── ENA (Left PWM)                          │    │
│  │   D8 ●◄─── IN1                                      │    │
│  │   D7 ●◄─── IN2                                      │    │
│  │   D6 ●◄─── ENB (Right PWM)                          │    │
│  │   D5 ●◄─── IN3                                      │    │
│  │   D4 ●◄─── IN4                                      │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

---

## Software Changes

Uses `Serial1` for Bluetooth:

```cpp
void setup() {
    Serial.begin(115200);   // USB debug
    Serial1.begin(9600);    // Bluetooth on D0/D1
}
```

---

## Sketch

Open `arduino_sketches/ArdunakonLeonardo/ArdunakonLeonardo.ino`

---

*Last updated: 2025-12-21*
