# Ardunakon
**Arduino Controller**

Ardunakon is an Android application designed to control Arduino RC cars and robots via Bluetooth Classic, BLE, or Wi-Fi UDP. It supports **Arduino UNO Q** (2025), **Arduino UNO R4 WiFi**, and classic Arduino UNO with HC-05/HC-06 or HM-10 modules. The app focuses on stability, clear telemetry, and a customizable interface.

## Release Info
* Current Alpha: **0.2.10-alpha-hotfix1** (build 31)
* Target SDK: 35, Min SDK: 26

### What's New in 0.2.10-alpha
*   **Custom Commands**: Create, save, and send up to 16 user-defined 10-byte protocol commands (0x20-0x3F) with a built-in hex editor and custom icons.
*   **Joystick Sensitivity Help**: Added dynamic help text in Settings to clarify how sensitivity curves impact control responsiveness.
*   **Quality**: Resolved complex Compose UI test compilation issues and added verification for unencrypted WiFi status icons.

### What's New in 0.2.9-alpha
*   **BLE Throughput**: 2M PHY + 517 MTU negotiation (when supported), balanced connection priority, and a 150-packet bounded write queue.
*   **Network Efficiency**: Duplicate suppression + ~60fps rate limiting for joystick/control packets to reduce saturation.
*   **Battery Optimization**: Foreground/background-aware monitoring cadence to reduce background power draw.
*   **Adaptive WiFi**: 4s discovery timeout, 3 max reconnect attempts, and RTT-adaptive heartbeat (1.5s - 5s).
*   **Docs & Tests**: Updated architecture diagrams and added Compose UI tests for key dialogs.

### What's New in 0.2.7-alpha-hotfix1
*   **Servo Z Fix (A/Z)**: A/Z are now encoded only in `auxBits` inside `CMD_JOYSTICK (0x01)` (no heartbeat control packets)
*   **No Idle Spam**: Joystick packets stop sending when all inputs are neutral (sends one final neutral packet on release)

### What's New in 0.2.7-alpha
*   **Platform Abstraction Layer**: Removed hardcoded Android dependencies for better testability
*   **WiFi Auto-Fallback**: Arduino R4 WiFi sketch now automatically tries router connection first, falls back to AP mode
*   **Larger Help Dialog**: Increased to 95% screen coverage in all orientations
*   **Better Layout**: Fixed header icon spacing in portrait mode (no more overlapping)

### What's New in 0.2.6-alpha
*   **Critical BLE Safety Fix**: Fixed motor unexpectedly spinning on BLE reconnect (Android 15/16)
*   **Connection State Check**: Transmission loop now only sends when actually connected
*   **State Reset on Disconnect**: Joystick/servo values reset to neutral on disconnect to prevent stale control values

## Key Features

### Precision Control
*   **RC Car Layout**: Left joystick for throttle with incremental speed control; WASD buttons for servo control (steering/pan-tilt).
*   **Throttle Joystick**: Vertical-only, non-centering for smooth speed changes.
*   **W/L/R/B Servo Buttons**: Tap to move, tap again to center (W forward, B backward, L left, R right).
*   **Smart Throttle Modes**: Car mode (-100% to +100%) and Drone/Boat mode (0% to 100%).
*   **Custom Sensitivity**: 10% to 200% response curve with real-time descriptive help text.

### Powerful Customization
*   **Aux Buttons**: 4 buttons with configurable labels, commands, and colors.
*   **Live Editing**: Change settings while connected.
*   **Haptics & Feedback**: Joystick haptics and connection quality ring.

### Connectivity
*   **Stability-focused reliability**: Bounded write queues, 3-strike retry logic, tuned timeouts.
*   **Bidirectional Data**: View telemetry in the Debug Console.
*   **Auto-Reconnect**: Restores connection when signal returns.
*   **Resilient Error Handling**: Tolerates transient interference without dropping immediately.

## Getting Started

### 1. Installation
1.  Download and install the APK on your Android device (Android 12+ recommended).
2.  Grant **Bluetooth** and **Location** permissions when prompted (required for scanning).

### 2. Arduino Setup

Choose your Arduino board and follow the setup guide:

#### Arduino UNO Q (2025 Latest Board)
The Arduino UNO Q features **built-in Bluetooth 5.1 BLE** - no external module needed!

1. Open `arduino_sketches/ArdunakonUnoQ/ArdunakonUnoQ.ino`
2. Install **ArduinoBLE** library
3. Upload to your Arduino UNO Q
4. Device will appear as **"ArdunakonQ"** in the app

**No external Bluetooth module required!**

---

#### Arduino UNO R4 WiFi
The R4 WiFi has **built-in BLE** via the ESP32-S3 module.

1. Open `arduino_sketches/ArdunakonR4WiFi/ArdunakonR4WiFi.ino`
2. Install **ArduinoBLE** library
3. **Important**: Update **Servo** library to v1.2.2+ (fixes R4 WiFi servo bug)
4. Upload to your Arduino R4 WiFi
5. Device will appear as **"ArdunakonR4"** in the app

**Wiring (v2.2+)**:
- Pin 6 -> Servo X (horizontal)
- Pin 7 -> Servo Y (vertical)
- Pin 9 -> Brushless ESC signal

**No external Bluetooth module required!** Works with official boards and BLE-capable clones; sketch advertises both ArduinoBLE default service (19B10000/19B10001) and HM-10-style UUIDs for maximum compatibility.

---

#### Classic Arduino UNO (with HC-05/HC-06 or HM-10)
Requires external Bluetooth module.

1. Open `arduino_sketches/ArdunakonClassicUno/ArdunakonClassicUno.ino`
2. Wire your HC-05/HC-06 or HM-10 module:
   - **VCC** -> 5V
   - **GND** -> GND
   - **TX** -> Arduino Pin 10 (RX)
   - **RX** -> Arduino Pin 11 (TX)
3. Upload the sketch
4. Device will appear as "HC-05", "HC-06", "HM-10", or custom name

**Full Setup Guide**: See `arduino_sketches/README.md` for detailed documentation or check the **built-in offline Help** in the app.

---

### 3. Connecting
1.  Open Ardunakon.
2.  Tap the connection status card to open the device list.
3.  Select your Arduino device.
4.  The status card will turn **Green** upon connection.

### 4. Wi-Fi Mode Quickstart
1. Connect your phone and Arduino/ESP running the Wi-Fi sketch to the same network (UDP 8888; replies with `ARDUNAKON_DEVICE:<name>`).
2. In the app, switch to Wi-Fi mode and open **Wi-Fi Config** to enter the target IP/port if discovery is blocked.
3. Allow Wi-Fi/Location permissions when prompted (Android 13+ may request Nearby Wi-Fi/Location for discovery).
4. 🔒 **Security**: Wi-Fi communications now use encrypted AES-GCM by default. Devices must support security handshake or connection will be blocked.


## Protocol Overview
Ardunakon sends a fixed 10-byte packet @ 20Hz:
`[START, DEV_ID, CMD, LX, LY, RX, RY, AUX, CS, END]`

*   **START**: `0xAA`
*   **LX/LY/RX/RY**: `0-200` (100 is center)
*   **END**: `0x55`

## Troubleshooting

### Quick Fixes
*   **"Permission Denied"**: Go to Android Settings > Apps > Ardunakon > Permissions and allow "Nearby Devices".
*   **No Data Received**: Check your Arduino TX/RX wiring (TX must go to RX).
*   **App Closes in Background**: Disable "Battery Optimization" for Ardunakon.
*   **HC-06 on Xiaomi/MIUI**: Automatically handled! The app now auto-enables reflection fallback and stream initialization delays for Xiaomi/Redmi/Poco devices.

### Comprehensive Guides
*   **[Quick Reference Card](QUICK_REFERENCE.md)** - One-page cheat sheet for common issues
*   **[HC-06 Clone Troubleshooting](HC06_TROUBLESHOOTING.md)** - 17 connection methods explained
*   **[HC-08 Troubleshooting](HC08_TROUBLESHOOTING.md)** - Complete HC-08 BLE module guide
*   **[HM-10 Troubleshooting](HM10_TROUBLESHOOTING.md)** - 7 UUID variants, all clones covered
*   **[Android Manufacturer Guide](ANDROID_MANUFACTURERS_GUIDE.md)** - Samsung, Xiaomi, Huawei-specific fixes
*   **[Dependency Analysis](DEPENDENCY_ANALYSIS.md)** - Zero dependency hell, full compatibility report
*   **In-App Help** - Tap Menu > Help for offline documentation with 4 detailed guides

---
Open source: https://github.com/metelci/ardunakon
