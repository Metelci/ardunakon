# Classic Arduino UNO / Nano Setup Guide

Setup instructions for Arduino UNO (Classic) and Nano with external Bluetooth modules.

## Overview

| Feature | UNO | Nano |
|---------|-----|------|
| **MCU** | ATmega328P | ATmega328P |
| **Voltage** | 5V | 5V |
| **PWM Pins** | 3, 5, 6, 9, 10, 11 | 3, 5, 6, 9, 10, 11 |
| **Bluetooth** | External required | External required |

---

## What You Need

- Arduino UNO or Nano
- Bluetooth module: **HC-05**, **HC-06**, or **HM-10**
- USB cable
- Motor driver (L298N)
- External power supply (7-12V)

---

## Software Setup

1. Install **Arduino IDE**
2. Open `arduino_sketches/ArdunakonClassicUno/ArdunakonClassicUno.ino`
3. Select Board: **Arduino UNO** or **Arduino Nano**
4. Select Processor: **ATmega328P** (or Old Bootloader for Nano clones)
5. Click **Upload**

---

## Wiring Diagram

### HC-05/HC-06 Bluetooth Classic

```
┌─────────────────────────────────────────────┐
│          Arduino UNO / Nano                  │
│  ┌─────────────────────────────────────┐    │
│  │                                     │    │
│  │   5V ●◄────────────────── VCC       │    │
│  │  GND ●◄────────────────── GND       │    │   HC-05/HC-06
│  │  D10 ●◄────────────────── TX  ─────►│    │   ┌──────────┐
│  │  D11 ●────────────────── RX*        │    │   │ STATE ●  │
│  │                                     │    │   │ RXD   ●◄─┼── D11 (via divider)
│  │   D9 ●◄─── ENA (Left PWM)           │    │   │ TXD   ●──┼─► D10
│  │   D8 ●◄─── IN1                      │    │   │ GND   ●◄─┼── GND
│  │   D7 ●◄─── IN2                      │    │   │ VCC   ●◄─┼── 5V
│  │   D6 ●◄─── ENB (Right PWM)          │    │   │ EN    ●  │
│  │   D5 ●◄─── IN3                      │    │   └──────────┘
│  │   D4 ●◄─── IN4                      │    │
│  │                                     │    │
│  │   A0 ●◄─── Battery (via divider)    │    │
│  │                                     │    │
│  └─────────────────────────────────────┘    │
└─────────────────────────────────────────────┘
```

### HC-05/HC-06 RX Voltage Divider (Required!)

The HC-05/06 RX pin is 3.3V. Use a voltage divider:

```
Arduino D11 ────┬─── 1kΩ ───┬─── HC-05/06 RX
                │           │
                └─── 2kΩ ───┴─── GND
```

---

### HM-10 BLE Module

```
┌─────────────────────────────────────────────┐
│             Arduino UNO / Nano               │
│  ┌─────────────────────────────────────┐    │
│  │                                     │    │   HM-10 BLE
│  │  3.3V ●◄───────────────── VCC*      │    │   ┌──────────┐
│  │   GND ●◄────────────────── GND      │    │   │ STATE ●  │
│  │   D10 ●◄────────────────── TX  ────►│    │   │ RXD   ●◄─┼── D11 (via divider)
│  │   D11 ●─────────────────► RX**      │    │   │ TXD   ●──┼─► D10
│  │                                     │    │   │ GND   ●◄─┼── GND
│  │  (Motor pins same as HC-05/06)      │    │   │ VCC   ●◄─┼── 3.3V or 5V***
│  │                                     │    │   │ BRK   ●  │
│  └─────────────────────────────────────┘    │   └──────────┘
└─────────────────────────────────────────────┘

* Some HM-10 modules are 5V tolerant - check datasheet
** Use voltage divider if module is 3.3V only
*** Check your specific module voltage requirements
```

---

### Motor Driver (L298N)

```
                    L298N Motor Driver
┌────────────────────────────────────────────────┐
│                                                │
│  ┌────────┐           ┌────────┐              │
│  │ Motor  │           │ Motor  │              │
│  │  Left  │           │ Right  │              │
│  └───┬────┘           └───┬────┘              │
│      │                    │                    │
│  OUT1● OUT2●          OUT3● OUT4●             │
│                                                │
│  ┌────────────────────────────────────────┐   │
│  │  IN1  IN2  ENA   IN3  IN4  ENB        │   │
│  │   ●    ●    ●     ●    ●    ●         │   │
│  │   ▲    ▲    ▲     ▲    ▲    ▲         │   │
│  │  D8   D7   D9    D5   D4   D6         │   │
│  │                                        │   │
│  │  +5V   GND    +12V                    │   │
│  │   ●     ●      ●                      │   │
│  │   ▲     ▲      ▲                      │   │
│  │Arduino Arduino External               │   │
│  │  5V    GND    Power                   │   │
│  └────────────────────────────────────────┘   │
└────────────────────────────────────────────────┘
```

---

## Module Configuration

### HC-06 AT Commands

Before uploading main sketch, use this to configure:

```cpp
#include <SoftwareSerial.h>
SoftwareSerial BT(10, 11);

void setup() {
    Serial.begin(9600);
    BT.begin(9600);
}

void loop() {
    if (BT.available()) Serial.write(BT.read());
    if (Serial.available()) BT.write(Serial.read());
}
```

**Common Commands**:
| Command | Response | Description |
|---------|----------|-------------|
| `AT` | `OK` | Test |
| `AT+NAMEArdunakon` | `OKsetname` | Set name |
| `AT+PIN1234` | `OKsetpin` | Set PIN |
| `AT+BAUD4` | `OK9600` | Set 9600 baud |

---

## Troubleshooting

### No Connection

- Check TX→RX and RX→TX crossed
- Verify voltage divider on RX pin
- HC-05/06 should blink rapidly when not paired

### Garbage Data

- Baud rate mismatch - ensure 9600 in sketch and module
- Missing voltage divider causing signal corruption

### HM-10 Clone Issues

The app auto-detects 7 UUID variants. Wait 15-20 seconds during connection.

---

## Pin Reference

| Function | UNO Pin | Nano Pin |
|----------|---------|----------|
| Bluetooth RX | D10 | D10 |
| Bluetooth TX | D11 | D11 |
| Left Motor PWM | D9 | D9 |
| Left Motor Dir1 | D8 | D8 |
| Left Motor Dir2 | D7 | D7 |
| Right Motor PWM | D6 | D6 |
| Right Motor Dir1 | D5 | D5 |
| Right Motor Dir2 | D4 | D4 |
| Battery Monitor | A0 | A0 |

---

*Last updated: 2025-12-21*
