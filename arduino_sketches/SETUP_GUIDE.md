# Ardunakon - Arduino Setup Guide

Complete setup instructions for all supported Arduino boards.
# Ardunakon - Arduino Setup Guide

Complete setup instructions for all supported Arduino boards.

---

## Supported Hardware

### ✅ Fully Supported Boards
1. **Arduino UNO Q** (Qualcomm QRB2210 + STM32U585) - Built-in Bluetooth 5.1 BLE
2. **Arduino UNO R4 WiFi** (Renesas RA4M1 + ESP32-S3) - Built-in BLE
3. **Arduino GIGA R1 WiFi** (STM32H7) - Built-in BLE/WiFi
4. **Classic Arduino UNO / Nano** (ATmega328P) - External HC-05/06
5. **Arduino Mega 2560** - External HC-05/06 (Hardware Serial!)
6. **Arduino Leonardo / Micro** - External HC-05/06
7. **Arduino Due / Zero** - External HC-05/06 (3.3V Logic!)


### ✅ Fully Supported Bluetooth Modules
- **Bluetooth Classic**: HC-05, HC-06 (and all clones)
- **Bluetooth Low Energy (BLE)**: HM-10, AT-09, MLT-BT05, TI CC2540/CC2541 (and ALL clone variants)

**Note**: The Ardunakon app supports **7 HM-10 UUID variants** including all common clone modules. This was extensively tested and debugged to ensure seamless connectivity.

---

## Quick Start by Board

## Option 0: Arduino GIGA R1 WiFi

### What You Need
- GIGA R1 WiFi Board
- USB-C Cable

### Setup
1. Open `arduino_sketches/ArdunakonGiga/ArdunakonGiga.ino`
2. Select Board: **Arduino GIGA R1 WiFi** (Install mbed core if needed)
3. Upload!
4. Status LED: Blue = Init, Green = Ready/Connected, Red = E-Stop.

### Wiring
**Motor Driver (Example: L298N)**
```
GIGA R1 WiFi     →    L298N Motor Driver
─────────────────────────────────────────
Pin 9 (PWM)      →    ENA (Left Motor Speed)
Pin 8            →    IN1 (Left Motor Dir 1)
Pin 7            →    IN2 (Left Motor Dir 2)
Pin 6 (PWM)      →    ENB (Right Motor Speed)
Pin 5            →    IN3 (Right Motor Dir 1)
Pin 4            →    IN4 (Right Motor Dir 2)
GND              →    GND
```

**Servos**
- **X:** Pin 2
- **Y:** Pin 12
- **Z:** Pin 11

## Option 1: Arduino UNO Q (Latest 2025 Board)

### What You Need
- Arduino UNO Q board ($44-$59)
- USB-C cable
- Motor driver (L298N or similar)
- Power supply for motors

### Software Setup
1. Install **Arduino IDE 2.x** or **Arduino App Lab**
2. Install **ArduinoBLE library**:
   - Tools → Manage Libraries → Search "ArduinoBLE" → Install
3. Open `arduino_sketches/ArdunakonUnoQ/ArdunakonUnoQ.ino`
4. Select **Board**: Tools → Board → Arduino UNO Q
5. Click **Upload**

### Wiring
**Built-in Bluetooth** - No external module needed!
Works with official UNO R4 WiFi and BLE-capable clones (advertises both ArduinoBLE default service and HM-10-style fallback).

**Motor Driver (Example: L298N)**
```
Arduino UNO Q    →    L298N Motor Driver
─────────────────────────────────────────
Pin 9 (PWM)      →    ENA (Left Motor Speed)
Pin 8            →    IN1 (Left Motor Dir 1)
Pin 7            →    IN2 (Left Motor Dir 2)
Pin 6 (PWM)      →    ENB (Right Motor Speed)
Pin 5            →    IN3 (Right Motor Dir 1)
Pin 4            →    IN4 (Right Motor Dir 2)
GND              →    GND
```

**Battery Voltage Monitor (Optional)**
```
Arduino UNO Q    →    Voltage Divider
─────────────────────────────────────────
Pin A0           →    10kΩ → Battery+
                      30kΩ → GND
```

### Connecting to Ardunakon App
1. Upload sketch to Arduino UNO Q
2. Open Ardunakon app on Android
3. Tap **"Dev 1"**
4. Look for **"ArdunakonQ"** in the device list
5. Tap to connect - Status will turn **Green**

---

## Option 2: Arduino UNO R4 WiFi (Bluetooth BLE)

### What You Need
- Arduino UNO R4 WiFi board
- USB-C cable
- Motor driver (L298N or similar)
- Power supply for motors

### Software Setup
1. Install **Arduino IDE 2.x**
2. Install **UNO R4 WiFi board support**:
   - Tools → Board → Boards Manager → Search "Arduino UNO R4" → Install
3. Install **ArduinoBLE library**:
   - Tools → Manage Libraries → Search "ArduinoBLE" → Install
4. Open `arduino_sketches/ArdunakonR4WiFi/ArdunakonR4WiFi.ino`
5. Select **Board**: Tools → Board → Arduino UNO R4 WiFi
6. Click **Upload**

### Wiring
**Built-in Bluetooth** - No external module needed!

**Motor Driver (Example: L298N)**
```
Arduino R4 WiFi  →    L298N Motor Driver
─────────────────────────────────────────
Pin 9 (PWM)      →    ENA (Left Motor Speed)
Pin 8            →    IN1 (Left Motor Dir 1)
Pin 7            →    IN2 (Left Motor Dir 2)
Pin 6 (PWM)      →    ENB (Right Motor Speed)
Pin 5            →    IN3 (Right Motor Dir 1)
Pin 4            →    IN4 (Right Motor Dir 2)
GND              →    GND
```

**Battery Voltage Monitor (Optional)**
```
Arduino R4       →    Voltage Divider
─────────────────────────────────────────
Pin A0           →    10kΩ → Battery+
                      30kΩ → GND
```

### Connecting to Ardunakon App
1. Upload sketch to Arduino R4 WiFi
2. Open Ardunakon app on Android
3. Tap **"Dev 1"**
4. Look for **"ArdunakonR4"** in the device list
5. Tap to connect - Status will turn **Green**
- Motor driver (L298N or similar)
- Power supply for motors

### Software Setup
1. Install **Arduino IDE 2.x** (or 1.8.x)
2. Open `arduino_sketches/ArdunakonClassicUno/ArdunakonClassicUno.ino`
3. Select **Board**: Tools → Board → Arduino UNO
4. Click **Upload**

### Wiring

**HC-05/HC-06 Bluetooth Module**
```
Arduino UNO      →    HC-05/HC-06
─────────────────────────────────────
5V               →    VCC
GND              →    GND
Pin 10 (RX)      →    TX (Bluetooth TX)
Pin 11 (TX)      →    RX (Bluetooth RX)
```

**⚠️ IMPORTANT**: HC-05/HC-06 RX pin is usually 3.3V tolerant. If your module requires voltage divider:
```
Arduino Pin 11 → 1kΩ → HC-05 RX
                        2kΩ → GND
```

**Motor Driver (Example: L298N)**
```
Arduino UNO      →    L298N Motor Driver
─────────────────────────────────────────
Pin 9 (PWM)      →    ENA (Left Motor Speed)
Pin 8            →    IN1 (Left Motor Dir 1)
Pin 7            →    IN2 (Left Motor Dir 2)
Pin 6 (PWM)      →    ENB (Right Motor Speed)
Pin 5            →    IN3 (Right Motor Dir 1)
Pin 4            →    IN4 (Right Motor Dir 2)
GND              →    GND
```

**Battery Voltage Monitor (Optional)**
```
Arduino UNO      →    Voltage Divider
─────────────────────────────────────────
Pin A0           →    10kΩ → Battery+
                      30kΩ → GND
```

### HC-05/HC-06 Configuration (Optional)
Most HC-05/HC-06 modules work with default settings (9600 baud). If you need to configure:

**HC-06 AT Commands** (use before uploading sketch):
```cpp
// Temporary sketch to send AT commands
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

Common AT commands:
- `AT` - Test connection (should respond "OK")
- `AT+NAMEArdunakonUno` - Set device name
- `AT+BAUD4` - Set to 9600 baud (default)
- `AT+PIN1234` - Set pairing PIN

### Connecting to Ardunakon App
1. Upload sketch to Arduino UNO
2. Power on Arduino (HC-05/06 LED should blink rapidly)
3. Open Ardunakon app on Android
4. Tap **"Dev 1"**
5. Look for **"HC-05"**, **"HC-06"**, or custom name in the device list
6. Tap to connect - Status will turn **Green**

---

## Option 4: Using HM-10 BLE Module with Classic Arduino UNO

If you prefer BLE over Bluetooth Classic with your Arduino UNO:

### What You Need
- Classic Arduino UNO
- HM-10 BLE module (or any clone: AT-09, MLT-BT05, etc.)
- USB cable
- Motor driver

### Wiring
```
Arduino UNO      →    HM-10
─────────────────────────────────────
3.3V or 5V*      →    VCC
GND              →    GND
Pin 10 (RX)      →    TX
Pin 11 (TX)      →    RX**
```

*Check your HM-10 module datasheet - some are 5V tolerant, others need 3.3V
**Use voltage divider if module is 3.3V only:
```
Arduino Pin 11 → 1kΩ → HM-10 RX
                        2kΩ → GND
```

### Software
The Classic UNO sketch works the same way - just change the baud rate:
```cpp
BTSerial.begin(9600); // Most HM-10 clones use 9600 or 115200
```

### Connecting
The Ardunakon app will automatically detect HM-10 and all clone variants (AT-09, MLT-BT05, TI CC2541, etc.) using **7 different UUID combinations** tested in the firmware.

---

## Customizing the Sketch

### Pin Configuration
Edit these lines in any sketch to match your hardware:

```cpp
#define MOTOR_LEFT_PWM    9   // Left motor PWM
#define MOTOR_LEFT_DIR1   8   // Left motor direction 1
#define MOTOR_LEFT_DIR2   7   // Left motor direction 2
#define MOTOR_RIGHT_PWM   6   // Right motor PWM
#define MOTOR_RIGHT_DIR1  5   // Right motor direction 1
#define MOTOR_RIGHT_DIR2  4   // Right motor direction 2
#define LED_STATUS        LED_BUILTIN
#define BUZZER_PIN        3   // Optional buzzer
```

### Steering Modes

**Tank Steering (Default)**
```cpp
void updateMotors() {
  setMotor(MOTOR_LEFT_PWM, MOTOR_LEFT_DIR1, MOTOR_LEFT_DIR2, leftY);
  setMotor(MOTOR_RIGHT_PWM, MOTOR_RIGHT_DIR1, MOTOR_RIGHT_DIR2, rightY);
}
```

**Car Steering (Differential)**
```cpp
void updateMotors() {
  int8_t throttle = rightY;  // Forward/backward
  int8_t steering = leftX;   // Left/right turn

  int8_t leftSpeed = throttle + steering;
  int8_t rightSpeed = throttle - steering;

  leftSpeed = constrain(leftSpeed, -100, 100);
  rightSpeed = constrain(rightSpeed, -100, 100);

  setMotor(MOTOR_LEFT_PWM, MOTOR_LEFT_DIR1, MOTOR_LEFT_DIR2, leftSpeed);
  setMotor(MOTOR_RIGHT_PWM, MOTOR_RIGHT_DIR1, MOTOR_RIGHT_DIR2, rightSpeed);
}
```

### Adding Servo Control
```cpp
#include <Servo.h>

Servo servo1;
Servo servo2;

void setup() {
  // ... existing setup ...
  servo1.attach(2);
  servo2.attach(12);
}

void handleButton(uint8_t buttonId, uint8_t pressed) {
  if (buttonId == 1 && pressed == 1) {
    servo1.write(90);  // Center position
  }
  if (buttonId == 2 && pressed == 1) {
    servo2.write(180); // Max position
  }
}
```

---

## Troubleshooting

### Arduino UNO Q / R4 WiFi

**Problem**: BLE not starting
- **Solution**: Make sure ArduinoBLE library is installed
- Check Serial Monitor for "Starting BLE failed!" message

**Problem**: Can't see device in Ardunakon scan
- **Solution**:
  - Reset Arduino board
  - Check if device name appears in Android Bluetooth settings
  - Make sure Android Bluetooth is ON and Location permission granted

### Classic UNO with HC-05/HC-06

**Problem**: No connection
- **Solution**:
  - Check wiring (TX→RX, RX→TX)
  - Verify HC-05/06 LED is blinking (not solid)
  - Check baud rate matches (9600 in both sketch and module)

**Problem**: HC-06 won't connect on Xiaomi/Redmi/Poco phones (MIUI/Android 12+)
- **Solution**: ✅ **AUTOMATICALLY FIXED in v0.1.75-alpha!**
  - The app now auto-detects Xiaomi devices and applies special connection methods
  - No manual configuration needed - it just works!
  - What the app does automatically:
    - Enables reflection port fallback
    - Adds 500ms stream initialization delay
    - Tries Xiaomi-optimized connection sequence first
  - If you're still having issues, check the debug log for "Device: Xiaomi" message

**Problem**: Receiving garbage data
- **Solution**:
  - Baud rate mismatch - reconfigure HC-05/06 to 9600
  - Check if voltage divider is needed for RX pin

### HM-10 Clones

**Problem**: Device found but won't connect
- **Solution**:
  - The Ardunakon app tries 7 UUID variants automatically
  - Wait 15-20 seconds during connection attempts
  - Some clones need 115200 baud instead of 9600

**Problem**: Connects but no data
- **Solution**:
  - Check TX/RX wiring
  - Verify packet format in Serial Monitor
  - Make sure HM-10 is in transparent mode (not AT command mode)

---

## Protocol Reference

### Packet Structure
All packets are exactly 10 bytes:
```
[START, DEV_ID, CMD, D1, D2, D3, D4, D5, CHECKSUM, END]
```

- **START**: Always `0xAA`
- **DEV_ID**: Device ID (usually `0x01`)
- **CMD**: Command type
- **D1-D5**: Data payload (5 bytes)
- **CHECKSUM**: XOR of bytes 1-7
- **END**: Always `0x55`

### Commands

**CMD_JOYSTICK (0x01)** - Joystick data @ 20Hz
```
D1 = Left X  (0-200, 100 is center)
D2 = Left Y  (0-200, 100 is center)
D3 = Right X (0-200, 100 is center)
D4 = Right Y (0-200, 100 is center)
D5 = Aux bits (bitfield for buttons)
```

**CMD_BUTTON (0x02)** - Button press/release
```
D1 = Button ID (1-255)
D2 = State (1=pressed, 0=released)
D3-D5 = Unused (0)
```

**CMD_HEARTBEAT (0x03)** - Connection keep-alive
```
App → Arduino:
D1-D2 = Sequence number (for RTT)
D3-D4 = Uptime (milliseconds)

Arduino → App:
D1 = Battery voltage * 10 (e.g., 124 = 12.4V)
D2 = Status (0=Active, 1=Safe Mode)
```

**CMD_ESTOP (0x04)** - Emergency stop
```
D1-D5 = All zeros
Immediately stops all motors
```

---

## Safety Features

All sketches include:
- ✅ **Checksum validation** - Rejects corrupted packets
- ✅ **Timeout protection** - Stops motors if no data for 2 seconds
- ✅ **Emergency stop** - Instant motor cutoff
- ✅ **Heartbeat monitoring** - Detects dead connections

---

## Example Projects

### RC Car (Tank Steering)
- Left joystick Y → Left motor
- Right joystick Y → Right motor
- Button 1 → Headlights
- Button 2 → Horn/Buzzer

### Robot Arm
- Left joystick X/Y → Base rotation + Shoulder
- Right joystick X/Y → Elbow + Wrist
- Buttons → Gripper open/close

### Drone (Throttle Mode)
- Use "Drone/Boat Mode" profile in app
- Right joystick Y → Throttle (0-100%)
- Left joystick → Pitch/Roll

---

## Additional Resources

- **Ardunakon App**: [GitHub Repository]
- **Arduino UNO Q**: [Official Product Page](https://www.arduino.cc/product-uno-q)
- **Arduino UNO R4**: [Official Documentation](https://docs.arduino.cc/hardware/uno-r4-wifi)
- **HC-05/06 Datasheet**: Search "HC-05 datasheet" for AT commands
- **HM-10 Clones**: Compatible with all MLT-BT05, AT-09, CC2541 variants

---

**Happy Making! 🚀**

For issues or questions, open an issue on the GitHub repository.
