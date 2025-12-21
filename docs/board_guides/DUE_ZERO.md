# Arduino Due / Zero Setup Guide

Setup for Arduino Due and Zero - **3.3V logic boards**.

## ⚠️ Critical Warning

| Board | Logic Level | HC-05/06 Compatible? |
|-------|-------------|---------------------|
| Due | **3.3V** | Direct connection OK |
| Zero | **3.3V** | Direct connection OK |

**No voltage divider needed** for TX→RX (both are 3.3V).

---

## Wiring Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                   Arduino Due / Zero                         │
│  ┌─────────────────────────────────────────────────────┐    │
│  │                                                     │    │
│  │  Serial1 TX (D18/D1) ●──────► RX (HC-05)           │    │
│  │  Serial1 RX (D19/D0) ●◄────── TX (HC-05)           │    │
│  │               3.3V ●◄──────── VCC                   │    │
│  │               GND ●◄───────── GND                   │    │
│  │                                                     │    │
│  │   D9 ●◄─── ENA (3.3V PWM)                          │    │
│  │   D8 ●◄─── IN1                                      │    │
│  │   D7 ●◄─── IN2                                      │    │
│  │   D6 ●◄─── ENB                                      │    │
│  │   D5 ●◄─── IN3                                      │    │
│  │   D4 ●◄─── IN4                                      │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

---

## Motor Driver Note

Some L298N modules need 5V logic. Options:
1. Use logic level converter
2. Use 3.3V compatible driver (TB6612FNG)

---

## Pin Reference

| Board | Serial1 TX | Serial1 RX |
|-------|-----------|-----------|
| Due | D18 | D19 |
| Zero | D1 | D0 |

---

## Sketches

- Due: `arduino_sketches/ArdunakonDue/ArdunakonDue.ino`
- Zero: `arduino_sketches/ArdunakonZero/ArdunakonZero.ino`

---

*Last updated: 2025-12-21*
