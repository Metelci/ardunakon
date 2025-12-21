# Ardunakon Arduino Sketches

This directory contains Arduino sketches for use with the Ardunakon Android application.

## Core Sketches (Required)

These sketches are the main firmware options for controlling Arduino boards via the Ardunakon app:

### 1. ArdunakonClassicUno
**Target Hardware:** Arduino UNO (Classic) with HC-06/HC-05 Bluetooth module  
**Features:**
- Classic Bluetooth (SPP) support
- 2-channel motor control (PWM)
- 2-axis servo control (pins 2 and 12)
- Arcade drive mixing
- Battery telemetry
- Compatible with HC-06, HC-05, MLT-BT05 modules

**Use Case:** Standard Arduino UNO with external Bluetooth module

---

### 2. ArdunakonR4WiFi
**Target Hardware:** Arduino UNO R4 WiFi  
**Features:**
- WiFi connectivity (TCP/IP)
- WiFi discovery protocol
- 2-channel motor control (PWM)
- 2-axis servo control (pins 2 and 12)
- Arcade drive mixing
- Battery telemetry over WiFi
- Built-in LED matrix support

**Use Case:** Arduino UNO R4 WiFi for wireless control without Bluetooth

---

### 3. ArdunakonUnoQ
**Target Hardware:** Arduino UNO (with Bluetooth module)  
**Features:**
- Quick setup variant
- 2-channel motor control
- 3-axis servo control (pins 2, 12, A1)
- Simplified configuration

**Use Case:** Streamlined version for quick prototyping

---

### 4. ArdunakonOTA
**Target Hardware:** Arduino UNO R4 WiFi  
**Features:**
- Over-The-Air (OTA) firmware updates via WiFi
- WiFi access point mode for updates
- Firmware upload and verification
- Update history tracking

**Use Case:** Enables wireless firmware updates from the Ardunakon app

---

## Example Sketches (Optional)

Located in `examples/PlugAndMake_Projects/` - these are sample projects demonstrating advanced features:

### EnvMonitor
**Hardware:** Arduino UNO R4 WiFi + Modulino Thermo  
**Features:**
- Temperature and humidity monitoring
- BLE connectivity
- LED matrix scrolling display
- Telemetry graphing in app

### MoodLight
**Hardware:** Arduino UNO R4 WiFi + Modulino Pixels  
**Features:**
- RGB LED control
- Color patterns and animations
- Remote control via app

### SecuritySystem
**Hardware:** Arduino UNO R4 WiFi + Modulino sensors  
**Features:**
- Motion detection
- Alert system
- Remote monitoring

---

## Getting Started

1. **Choose your hardware:**
   - Arduino UNO Classic / Nano → Use `ArdunakonClassicUno`
   - Arduino UNO R4 WiFi → Use `ArdunakonR4WiFi`
   - Arduino Mega 2560 → Use `ArdunakonMega`
   - Arduino Leonardo / Micro → Use `ArdunakonLeonardo`
   - Arduino GIGA R1 → Use `ArdunakonGiga`
   - Arduino Due / Zero → Use `ArdunakonDue` / `ArdunakonZero`

2. **Upload the sketch:**
   - Open the `.ino` file in Arduino IDE
   - Select your board and port
   - Click Upload

3. **Configure Bluetooth/WiFi:**
   - **Bluetooth:** Pair your HC-06/HC-05 module (default PIN: 1234)
   - **WiFi:** Arduino will create an AP named "ARDUNAKON_XXXX"

4. **Connect from Ardunakon App:**
   - Open the app
   - Scan for devices
   - Connect and start controlling!

---

## Pin Configuration

### Motor Control (All Sketches)
- **Motor A:** Pins 5 (PWM), 4 (DIR)
- **Motor B:** Pins 6 (PWM), 7 (DIR)

### Servo Control
**Classic UNO & UNO Q:**
- **Servo X:** Pin 2
- **Servo Y:** Pin 12
- **Servo Z:** Pin A1

**UNO R4 WiFi:**
- **Servo X:** Pin 2
- **Servo Y:** Pin 11
- **Servo Z:** Pin 12

**Leonardo / Micro:**
- **Servo X:** Pin 2
- **Servo Y:** Pin 12
- **Servo Z:** Pin 10

**Mega 2560, Due, Zero, GIGA R1:**
- **Servo X:** Pin 2
- **Servo Y:** Pin 12
- **Servo Z:** Pin 11

### Battery Monitoring (Optional)
- **Analog Input:** A0 (voltage divider recommended)

---

## Bluetooth Module Compatibility

The Ardunakon app supports multiple Bluetooth modules:

- ✅ HC-06 (Classic Bluetooth SPP)
- ✅ HC-05 (Classic Bluetooth SPP)
- ✅ MLT-BT05 (HC-06 compatible)
- ✅ HM-10 (BLE - limited support)
- ✅ HC-08 (BLE)

**Note:** Make sure your sketch matches your module type (Classic BT vs BLE)

---

## WiFi Configuration (R4 WiFi only)

The ArdunakonR4WiFi sketch creates a WiFi access point:

- **SSID:** `ARDUNAKON_XXXX` (where XXXX is based on MAC address)
- **Password:** None (open network)
- **IP Address:** 192.168.4.1
- **Port:** 8888

---

## Troubleshooting

### Bluetooth Connection Issues
- Verify module is powered (LED blinking)
- Check baud rate matches (default: 9600)
- Ensure module is not already paired to another device
- Try resetting the Bluetooth module

### WiFi Connection Issues
- Ensure Arduino is powered and running
- Check that WiFi AP is visible in phone settings
- Verify you're within range (< 10 meters)
- Restart Arduino if AP doesn't appear

### Motor Not Responding
- Check motor driver connections
- Verify power supply is adequate
- Test with simple motor test sketch first

---

## Contributing

When creating new sketches:
1. Follow the naming convention: `Ardunakon[Variant]`
2. Include header comments explaining purpose and hardware
3. Document pin configurations
4. Test with the Ardunakon app before committing

---

## License

These sketches are part of the Ardunakon project.  
See the main project README for license information.
