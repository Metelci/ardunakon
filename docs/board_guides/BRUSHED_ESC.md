# Brushed ESC Wiring Guide

Alternative motor control using brushed Electronic Speed Controllers (ESCs) instead of H-bridge drivers like L298N.

## When to Use Brushed ESCs

| Use Case | Recommended Driver |
|----------|-------------------|
| Small hobby motors (&lt;2A) | L298N, TB6612 |
| RC car motors (10-30A) | **Brushed ESC (30A-60A)** |
| Large robot motors (30-120A) | **Brushed ESC (60A-120A)** |

**Advantages of ESC over L298N:**
- ✅ Higher current capacity (60A-120A vs 2A)
- ✅ Better efficiency (no heat issues)
- ✅ Built-in BEC (5V regulator)
- ✅ Simpler wiring (3 wires vs 6)

---

## Wiring Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         Arduino Board                            │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                                                         │    │
│  │   D9 ●──────────────────► ESC 1 Signal (White/Yellow)  │    │
│  │  D10 ●──────────────────► ESC 2 Signal (White/Yellow)  │    │
│  │  GND ●──────────────────► ESC 1 & 2 GND (Black)        │    │
│  │   5V ●◄─────────────────── BEC Output (Red) *optional  │    │
│  │                                                         │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘

        Brushed ESC #1                    Brushed ESC #2
       (60A-120A Rated)                  (60A-120A Rated)
┌───────────────────────┐          ┌───────────────────────┐
│                       │          │                       │
│  ┌─────────────────┐  │          │  ┌─────────────────┐  │
│  │ Signal  GND  5V │  │          │  │ Signal  GND  5V │  │
│  │   ●      ●    ● │  │          │  │   ●      ●    ● │  │
│  │   ▲      ▲    ▲ │  │          │  │   ▲      ▲    ▲ │  │
│  │   │      │    │ │  │          │  │   │      │    │ │  │
│  │  D9    GND  *5V │  │          │  │ D10    GND    │ │  │
│  └─────────────────┘  │          └─────────────────────┘  │
│                       │                                   │
│  ┌─────────────────┐  │          ┌─────────────────────┐  │
│  │ Motor+  Motor-  │  │          │ Motor+    Motor-    │  │
│  │   ●       ●     │  │          │   ●         ●       │  │
│  │   │       │     │  │          │   │         │       │  │
│  │   └───┬───┘     │  │          │   └────┬────┘       │  │
│  │       ▼         │  │          │        ▼            │  │
│  │   Left Motor    │  │          │   Right Motor       │  │
│  └─────────────────┘  │          └─────────────────────┘  │
│                       │                                   │
│  ┌─────────────────┐  │          ┌─────────────────────┐  │
│  │ Batt+   Batt-   │  │          │ Batt+     Batt-     │  │
│  │   ●       ●     │◄─┼──────────┼──►●         ●       │  │
│  │   │       │     │  │          │   │         │       │  │
│  └───┼───────┼─────┘  │          └───┼─────────┼───────┘  │
│      │       │        │              │         │          │
└──────┼───────┼────────┘              │         │          │
       │       │                       │         │
       └───────┼───────────────────────┘         │
               │                                 │
         ┌─────┴─────────────────────────────────┴─────┐
         │                                             │
         │              LiPo Battery                   │
         │              2S-6S (7.4V - 22.2V)          │
         │                                             │
         │    ┌─────────────────────────────────┐     │
         │    │  Red (+)            Black (-)   │     │
         │    │    ●                    ●       │     │
         │    └─────────────────────────────────┘     │
         │                                             │
         └─────────────────────────────────────────────┘
```

---

## Wire Color Reference

| Wire | Color | Function |
|------|-------|----------|
| Signal | White or Yellow | PWM input from Arduino |
| Ground | Black | Common ground |
| BEC 5V | Red (thin) | 5V output to power Arduino |
| Battery + | Red (thick) | Power input from LiPo |
| Battery - | Black (thick) | Power input ground |
| Motor + | Varies | Connects to motor + |
| Motor - | Varies | Connects to motor - |

---

## Arduino Code for ESC Control

ESCs use servo-style PWM (1000-2000μs pulses):

```cpp
#include <Servo.h>

Servo leftESC;
Servo rightESC;

void setup() {
    leftESC.attach(9);   // D9
    rightESC.attach(10); // D10
    
    // IMPORTANT: Arming sequence - center throttle at startup
    leftESC.writeMicroseconds(1500);
    rightESC.writeMicroseconds(1500);
    delay(2000); // Wait for ESC to arm
}

void loop() {
    // Example: Map joystick value (0-200) to ESC range (1000-2000)
    int joystickValue = 100; // Center = stopped
    
    // Convert: 0-200 → 1000-2000μs
    int pulseWidth = map(joystickValue, 0, 200, 1000, 2000);
    
    leftESC.writeMicroseconds(pulseWidth);
    rightESC.writeMicroseconds(pulseWidth);
}
```

---

## PWM Values Reference

| Pulse Width | Motor Action |
|-------------|--------------|
| 1000μs | Full Reverse |
| 1250μs | Half Reverse |
| 1500μs | **Stop (Neutral)** |
| 1750μs | Half Forward |
| 2000μs | Full Forward |

---

## ESC Calibration (Optional)

Some ESCs need calibration for the throttle range:

1. Disconnect battery from ESC
2. Set Arduino to output 2000μs (full throttle)
3. Connect battery → ESC beeps
4. Immediately change to 1000μs (full brake)
5. ESC beeps to confirm calibration

---

## Common Brushed ESC Models

| Model | Current | Voltage | BEC | Notes |
|-------|---------|---------|-----|-------|
| Hobbywing QuicRun | 60A | 2-3S | 3A | RC crawlers |
| Turnigy TrackStar | 120A | 2-4S | 5A | RC cars |
| Flycolor | 80A | 2-6S | 5A | Budget option |
| Castle Sidewinder | 60A | 2-3S | 3A | Premium |

---

## Troubleshooting

### ESC Not Responding

- **Cause**: ESC not armed
- **Solution**: Send 1500μs (neutral) for 2 seconds at startup

### Motor Runs in Wrong Direction

- **Cause**: Motor polarity reversed
- **Solution**: Swap motor + and - wires at ESC

### ESC Beeping Continuously

- **Cause**: Signal not received or low battery
- **Solution**: Check signal wire connection, charge battery

### Motor Stutters or Jerks

- **Cause**: PWM frequency incompatible
- **Solution**: Use `Servo` library (50Hz) instead of `analogWrite`

---

*Last updated: 2025-12-21*
