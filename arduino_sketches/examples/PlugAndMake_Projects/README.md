# PlugAndMake Example Projects

These are example Arduino sketches demonstrating advanced features with Arduino UNO R4 WiFi and Modulino accessories.

## ⚠️ Note

These are **optional example projects** and are **not required** for basic Ardunakon functionality. They showcase what's possible when combining the Ardunakon app with Arduino's Modulino ecosystem.

---

## Projects

### 📡 EnvMonitor - Environmental Monitoring
**Hardware Required:**
- Arduino UNO R4 WiFi
- Modulino Thermo

**Description:**  
Monitors temperature and humidity, sending real-time data to the Ardunakon app. Features scrolling temperature display on the LED matrix.

**Features:**
- BLE connectivity
- Temperature/humidity telemetry
- LED matrix scrolling display
- Graph visualization in app

---

### 💡 MoodLight - RGB Lighting Control
**Hardware Required:**
- Arduino UNO R4 WiFi
- Modulino Pixels (RGB LED module)

**Description:**  
Control RGB LED patterns and colors remotely through the Ardunakon app.

**Features:**
- Color pattern control
- Animation modes
- Remote brightness adjustment

---

### 🔒 SecuritySystem - Motion Detection
**Hardware Required:**
- Arduino UNO R4 WiFi
- Modulino Movement (PIR sensor)
- Optional: Modulino Buzzer

**Description:**  
A simple security system with motion detection and remote alerts.

**Features:**
- Motion detection
- Alert notifications
- Remote arming/disarming

---

## Getting Started

1. **Install Required Libraries:**
   - ArduinoBLE
   - Modulino
   - Arduino_LED_Matrix

2. **Upload Sketch:**
   - Open the desired `.ino` file
   - Select "Arduino UNO R4 WiFi" as board
   - Upload

3. **Connect Hardware:**
   - Attach the required Modulino modules
   - Power the Arduino

4. **Connect via Ardunakon App:**
   - Scan for BLE devices
   - Look for device name (e.g., "Ardunakon Env")
   - Connect and view data in terminal

---

## Modulino Information

Modulino is Arduino's plug-and-play accessory ecosystem for the UNO R4 WiFi.

**Available Modules:**
- Modulino Thermo (Temperature/Humidity)
- Modulino Distance (Ultrasonic sensor)
- Modulino Knob (Rotary encoder)
- Modulino Movement (PIR motion sensor)
- Modulino Pixels (RGB LEDs)
- Modulino Buzzer (Audio output)

**Learn More:** https://www.arduino.cc/pro/hardware-modulino

---

## Customization

These sketches are meant to be starting points. Feel free to:
- Combine multiple sensors
- Add custom control commands
- Modify telemetry data formats
- Create your own projects

---

## Returning to Core Ardunakon

To use standard motor/servo control, upload one of the core sketches:
- `ArdunakonR4WiFi` (for WiFi control)
- `ArdunakonClassicUno` (for Bluetooth with classic UNO)

---

## Support

These are community examples. For core Ardunakon support, see the main project documentation.
