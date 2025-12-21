# Arduino UNO Q Setup Guide

Complete setup for the Arduino UNO Q (2025) with built-in Bluetooth 5.1 BLE.

## Overview

| Feature | Specification |
|---------|--------------|
| **Main MCU** | STM32U585 + Qualcomm QRB2210 |
| **Wireless** | Built-in Bluetooth 5.1 BLE |
| **Voltage** | 5V logic |
| **PWM Pins** | 3, 5, 6, 9, 10, 11 |

---

## What You Need

- Arduino UNO Q board ($44-$59)
- USB-C cable
- Motor driver (L298N)
- External power supply (7-12V)

---

## Wiring Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     Arduino UNO Q                            │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  [USB-C]                                    [RESET] │    │
│  │                                                     │    │
│  │   D9 ●◄───────────────────── ENA (Left PWM)        │    │
│  │   D8 ●◄───────────────────── IN1 (Left Dir 1)      │    │
│  │   D7 ●◄───────────────────── IN2 (Left Dir 2)      │    │
│  │   D6 ●◄───────────────────── ENB (Right PWM)       │    │
│  │   D5 ●◄───────────────────── IN3 (Right Dir 1)     │    │
│  │   D4 ●◄───────────────────── IN4 (Right Dir 2)     │    │
│  │   D2 ●◄───────────────────── Servo X               │    │
│  │  D12 ●◄───────────────────── Servo Y               │    │
│  │  D11 ●◄───────────────────── Servo Z               │    │
│  │   A0 ●◄───────────────────── Battery Voltage       │    │
│  │  GND ●◄───────────────────── Common Ground         │    │
│  │   5V ●◄───────────────────── Logic Power           │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

---

## Software Setup

1. Install **Arduino IDE 2.x** or **Arduino App Lab**
2. Install **ArduinoBLE library**
3. Open `arduino_sketches/ArdunakonUnoQ/ArdunakonUnoQ.ino`
4. Select Board: **Arduino UNO Q**
5. Upload

---

## Connecting to App

1. Upload sketch
2. Open Ardunakon app
3. Tap Bluetooth icon
4. Look for **"ArdunakonQ"**
5. Tap to connect

---

*Last updated: 2025-12-21*
