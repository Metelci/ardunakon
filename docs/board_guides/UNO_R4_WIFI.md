# Arduino UNO R4 WiFi Setup Guide

Complete setup instructions for the Arduino UNO R4 WiFi board with built-in BLE via ESP32-S3.

## Overview

| Feature | Specification |
|---------|--------------|
| **Main MCU** | Renesas RA4M1 (32-bit ARM Cortex-M4) |
| **Wireless** | ESP32-S3 (BLE + WiFi) |
| **Voltage** | 5V logic (3.3V on ESP32) |
| **PWM Pins** | 3, 5, 6, 9, 10, 11 |
| **Analog Inputs** | A0-A5 |

---

## What You Need

- Arduino UNO R4 WiFi board
- USB-C cable
- Motor driver (L298N or similar)
- External power supply for motors (7-12V)
- Servos (optional)

---

## Software Setup

1. Install **Arduino IDE 2.x** or later
2. Install board support:
   - Tools → Board → Boards Manager
   - Search "Arduino UNO R4" → Install
3. Install ArduinoBLE library:
   - Tools → Manage Libraries
   - Search "ArduinoBLE" → Install
4. Open `arduino_sketches/ArdunakonR4WiFi/ArdunakonR4WiFi.ino`
5. Select Board: **Arduino UNO R4 WiFi**
6. Click **Upload**

---

## Wiring Diagram

### Motor Driver (L298N)

```
┌─────────────────────────────────────────────────────────────┐
│                    Arduino UNO R4 WiFi                       │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  [USB-C]                                    [RESET] │    │
│  │                                                     │    │
│  │  D13 ●                                         ● Vin│    │
│  │  D12 ●                                         ● GND│◄───┼─── Motor GND
│  │  D11 ●                                         ● GND│    │
│  │  D10 ●                                         ● 5V │◄───┼─── Motor VCC (Logic)
│  │   D9 ●◄─────────────────── ENA (Left PWM)      ● 3V3│    │
│  │   D8 ●◄─────────────────── IN1 (Left Dir 1)   ● RST│    │
│  │   D7 ●◄─────────────────── IN2 (Left Dir 2)  ● IOREF│    │
│  │   D6 ●◄─────────────────── ENB (Right PWM)         │    │
│  │   D5 ●◄─────────────────── IN3 (Right Dir 1)       │    │
│  │   D4 ●◄─────────────────── IN4 (Right Dir 2)       │    │
│  │   D3 ●                                              │    │
│  │   D2 ●◄─────────────────── Servo X                 │    │
│  │   D1 ●                                              │    │
│  │   D0 ●                                    ● A0◄────┼─── Battery Voltage
│  │                                           ● A1 │    │
│  │                                           ● A2 │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘

           L298N Motor Driver
┌────────────────────────────────────────┐
│                                        │
│  ┌────┐  ┌────┐                       │
│  │ M1 │  │ M2 │   ← Connect Motors    │
│  └────┘  └────┘                       │
│                                        │
│  ┌─────────────────────────────────┐  │
│  │ OUT1 OUT2    OUT3 OUT4          │  │
│  │  ●     ●      ●     ●           │  │
│  │                                  │  │
│  │ IN1  IN2  ENA  IN3  IN4  ENB   │  │
│  │  ●    ●    ●    ●    ●    ●    │  │
│  │  ▲    ▲    ▲    ▲    ▲    ▲    │  │
│  │  D8   D7   D9   D5   D4   D6   │  │
│  │                                  │  │
│  │  5V   GND   12V                 │  │
│  │  ●     ●     ●  ← External PSU  │  │
│  └─────────────────────────────────┘  │
└────────────────────────────────────────┘
```

### Alternative: Brushed ESC (60A-120A)

For higher power applications (RC cars, larger robots), use brushed ESCs instead of L298N:

```
┌─────────────────────────────────────────────────────────────────┐
│                      Arduino UNO R4 WiFi                         │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                                                         │    │
│  │   D9 ●──────────────────► ESC 1 Signal (White/Yellow)  │    │
│  │  D10 ●──────────────────► ESC 2 Signal (White/Yellow)  │    │
│  │  GND ●──────────────────► ESC 1 & 2 GND (Black)        │    │
│  │   5V ●◄─────────────────── BEC Output (Red) *optional  │    │
│  │                                                         │    │
│  │   D2 ●──────────────────► Servo X Signal               │    │
│  │  D12 ●──────────────────► Servo Y Signal               │    │
│  │  D11 ●──────────────────► Servo Z Signal               │    │
│  │   A0 ●◄─────────────────── Battery Voltage             │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘

        Brushed ESC #1                    Brushed ESC #2
       (60A-120A Rated)                  (60A-120A Rated)
┌───────────────────────┐          ┌───────────────────────┐
│                       │          │                       │
│  ┌─────────────────┐  │          │  ┌─────────────────┐  │
│  │ Signal  GND  5V │  │          │  │ Signal  GND  5V │  │
│  │   ●      ●    ● │  │          │  │   ●      ●    ● │  │
│  │   │      │    │ │  │          │  │   │      │    │ │  │
│  │   │      │    └─┼──┼──────────┼──┼───┴──────│────┴─│  │
│  │   │      └──────┼──┼──────────┼──┼──────────┴──────│  │
│  │   └─────────────┼──┼─► D9     │  └─────────────────│  │
│  │                 │  │          │         D10 ◄──────│  │
│  └─────────────────┘  │          └─────────────────────┘  │
│                       │                                   │
│  Motor+ ● ────────────┼─► Left Motor                      │
│  Motor- ●             │                                   │
│                       │          Motor+ ● ───► Right Motor│
│  Batt+ ● ◄────────────┼─┬────────Batt+ ●                  │
│  Batt- ● ◄────────────┼─┼────────Batt- ●                  │
│                       │ │                                 │
└───────────────────────┘ │        └───────────────────────┘
                          │
                    ┌─────┴─────┐
                    │  LiPo     │
                    │  Battery  │
                    │  2S-6S    │
                    │  (7.4V-   │
                    │   22.2V)  │
                    └───────────┘
```

**ESC Wiring Notes:**

| Wire Color | Function | Connect To |
|------------|----------|------------|
| White/Yellow | Signal | Arduino PWM pin (D9, D10) |
| Black | Ground | Arduino GND |
| Red | BEC 5V Output | Arduino 5V (optional power) |
| Thick Red | Battery + | LiPo + |
| Thick Black | Battery - | LiPo - |
| Motor wires | Motor power | Brushed DC motor |

**Important:**
- ⚠️ ESC signal uses **PWM** (1000-2000μs pulse width, like servos)
- Use `Servo` library with `servo.writeMicroseconds(1500)` for center/stop
- 1000μs = full reverse, 1500μs = stop, 2000μs = full forward
- Most brushed ESCs require **arming sequence** (center throttle at power-on)

### Servo Connections

| Servo | Arduino Pin | Wire Color |
|-------|-------------|------------|
| X-axis | D2 | Orange = Signal, Red = 5V, Brown = GND |
| Y-axis | D12 | Orange = Signal, Red = 5V, Brown = GND |
| Z-axis | D11 | Orange = Signal, Red = 5V, Brown = GND |

### Battery Voltage Monitor (Optional)

```
Battery+ ───┬─── 10kΩ ───┬─── A0
            │            │
            └─── 30kΩ ───┴─── GND
```

**Formula**: `Voltage = (A0 reading / 1023) * 3.3 * 4`

---

## Connecting to Ardunakon App

1. Upload sketch to Arduino
2. Open serial monitor to verify "ArdunakonR4" appears
3. Open Ardunakon app on Android
4. Tap Bluetooth icon in header
5. Look for "ArdunakonR4" in device list
6. Tap to connect

**Status Colors**:
- 🔴 **Red** = Disconnected
- 🟡 **Yellow** = Connecting
- 🟢 **Green** = Connected

---

## WiFi Mode (Alternative)

The R4 WiFi also supports UDP WiFi control:

1. Load `ArdunakonWiFiEncrypted/` sketch instead
2. Configure WiFi credentials in sketch
3. In Ardunakon app, switch to WiFi mode
4. Enter Arduino's IP address
5. Connect

---

## Troubleshooting

### BLE Not Starting

- **Cause**: ArduinoBLE library not installed
- **Solution**: Install ArduinoBLE via Library Manager
- **Check**: Serial monitor shows "Starting BLE failed!"

### Device Not Found in Scan

- **Cause**: Bluetooth permissions not granted
- **Solution**: Grant Location + Bluetooth permissions in app
- **Check**: Reset Arduino and try again

### Connection Drops

- **Cause**: Power supply insufficient
- **Solution**: Use external 12V supply for L298N
- **Check**: Don't power motors from Arduino 5V pin

### Motors Not Moving

- **Cause**: Wrong pin connections or baud mismatch
- **Solution**: Verify wiring matches diagram above
- **Check**: Serial monitor shows joystick values (0-200)

---

## Pin Reference

| Function | Pin | Notes |
|----------|-----|-------|
| Left Motor PWM | D9 | ENA on L298N |
| Left Motor Dir1 | D8 | IN1 on L298N |
| Left Motor Dir2 | D7 | IN2 on L298N |
| Right Motor PWM | D6 | ENB on L298N |
| Right Motor Dir1 | D5 | IN3 on L298N |
| Right Motor Dir2 | D4 | IN4 on L298N |
| Servo X | D2 | Pan servo |
| Servo Y | D12 | Tilt servo |
| Servo Z | D11 | Aux servo |
| Battery Monitor | A0 | Via voltage divider |
| Status LED | D13 | Built-in LED |

---

*Last updated: 2025-12-21*
