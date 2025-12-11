# Ardunakon 🎮📡
**The Ultimate Arduino Bluetooth Controller**

Ardunakon is a professional-grade Android application designed to control Arduino projects (RC cars, drones, boats, robots) via Bluetooth Classic or BLE. Fully supports **Arduino UNO Q** (2025), **Arduino UNO R4 WiFi**, and **classic Arduino UNO** with HC-05/HC-06 or HM-10 modules. It replaces generic controller apps with a robust, crash-proof, and highly customizable interface.

## Release Info
* Current Alpha: **0.2.5-alpha** (build 22)
* Target SDK: 35, Min SDK: 26

### What's New in 0.2.5-alpha
*   **Code Cleanup**: Removed 4 orphaned components (~450 lines) - duplicate debug console, unused sensor dashboard, and unused security classes
*   **Better Organization**: Relocated example Arduino sketches to `examples/` directory with dedicated README
*   **Comprehensive Docs**: Added detailed documentation for all Arduino sketches (pin mappings, Bluetooth compatibility matrix)

## 🚀 Key Features

### 🕹️ Precision Control
*   **RC Car Layout**: Left joystick for throttle (RC motor) with incremental speed control, WASD buttons for servo control (steering/pan-tilt).
*   **Throttle Joystick**: Vertical-only control with non-centering for smooth speed adjustments. Perfect for RC car motor control.
*   **W/L/R/B Servo Buttons**: Toggle-based directional control - tap to move servo, tap again to center. W (Forward), L (Left), R (Right), B (Backward).
*   **Smart Throttle Modes**:
    *   **Car Mode**: Bidirectional control (-100% to +100%) for forward/reverse.
    *   **Drone/Boat Mode**: Unidirectional control (0% to 100%) for ESCs.
*   **Custom Sensitivity**: Adjust control response from 10% to 200% to match your driving style.

### ⚙️ Powerful Customization
*   **Button Config**: 4 Aux buttons with customizable labels, commands, and colors.
*   **Live Editing**: Tweak settings instantly without disconnecting.
*   **Haptic Feedback**: Joystick vibrates at center deadzone and edge boundaries.
*   **Connection Quality Ring**: Visual indicator around joystick shows connection latency.

### 📡 Real-World Connectivity
*   **Military-Grade Stability**: 4 critical reliability improvements including bounded write queues, 3-strike retry logic, and optimized timeouts
*   **Bidirectional Data**: View real-time feedback from your Arduino in the Debug Console.
*   **Auto-Reconnect**: Automatically restores connection if signal is lost.
*   **Resilient Error Handling**: Tolerates transient interference and temporary signal issues without disconnecting
*   **Zero Fakes**: No mock data. Every signal strength bar and log entry is real.

## 🛠️ Getting Started

### 1. Installation
1.  Download and install the APK on your Android device (Android 12+ recommended).
2.  Grant **Bluetooth** and **Location** permissions when prompted (required for scanning).

### 2. Arduino Setup

Choose your Arduino board and follow the setup guide:

#### ✨ **Arduino UNO Q** (2025 Latest Board)
The Arduino UNO Q features **built-in Bluetooth 5.1 BLE** - no external module needed!

1. Open `arduino_sketches/ArdunakonUnoQ/ArdunakonUnoQ.ino`
2. Install **ArduinoBLE** library
3. Upload to your Arduino UNO Q
4. Device will appear as **"ArdunakonQ"** in the app

**No external Bluetooth module required!** ✅

---

#### 🔥 **Arduino UNO R4 WiFi**
The R4 WiFi has **built-in BLE** via the ESP32-S3 module.

1. Open `arduino_sketches/ArdunakonR4WiFi/ArdunakonR4WiFi.ino`
2. Install **ArduinoBLE** library
3. **Important**: Update **Servo** library to v1.2.2+ (fixes R4 WiFi servo bug)
4. Upload to your Arduino R4 WiFi
5. Device will appear as **"ArdunakonR4"** in the app

**Wiring (v2.2+)**:
- Pin 6 → Servo X (horizontal)
- Pin 7 → Servo Y (vertical)
- Pin 9 → Brushless ESC signal

**No external Bluetooth module required!** ✅ Works with official boards and BLE-capable clones; sketch advertises both ArduinoBLE default service (19B10000/19B10001) and HM-10-style UUIDs for maximum compatibility.

---

#### 🎛️ **Classic Arduino UNO** (with HC-05/HC-06 or HM-10)
Requires external Bluetooth module.

1. Open `arduino_sketches/ArdunakonClassicUno/ArdunakonClassicUno.ino`
2. Wire your HC-05/HC-06 or HM-10 module:
   - **VCC** → 5V
   - **GND** → GND
   - **TX** → Arduino Pin 10 (RX)
   - **RX** → Arduino Pin 11 (TX)
3. Upload the sketch
4. Device will appear as "HC-05", "HC-06", "HM-10", or custom name

**📖 Full Setup Guide**: See `arduino_sketches/README.md` for detailed documentation or check the **built-in offline Help** in the app.

---

### 3. Connecting
1.  Open Ardunakon.
2.  Tap **"Dev 1"** (Slot 1) or **"Dev 2"** (Slot 2).
3.  Select your Arduino from the device list.
4.  The status card will turn **Green** upon connection.

## 📝 Protocol Overview
Ardunakon sends a fixed 10-byte packet @ 20Hz:
`[START, DEV_ID, CMD, LX, LY, RX, RY, AUX, CS, END]`

*   **START**: `0xAA`
*   **LX/LY/RX/RY**: `0-200` (100 is center)
*   **END**: `0x55`

## 🐞 Troubleshooting

### Quick Fixes
*   **"Permission Denied"**: Go to Android Settings → Apps → Ardunakon → Permissions and allow "Nearby Devices".
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
*   **In-App Help** - Tap Menu → Help for offline documentation with 4 detailed guides

---
*Built for Makers, by Makers.*

## 📢 Play Store Listing

**Title**: Ardunakon: Pro Arduino Bluetooth Controller

**Short Description**:
Control Arduino RC cars, drones & robots with precision. Secure, customizable & crash-proof.

**Full Description**:
Unlock the full potential of your Arduino projects with **Ardunakon**, the professional-grade Bluetooth controller designed for makers, hobbyists, and engineers.

Whether you're driving an RC car, piloting a drone, or controlling a complex robot, Ardunakon delivers a crash-proof, high-performance interface that just works. Forget about generic, buggy controllers—experience precision, security, and total customization.

**🚀 Key Features:**

*   **🕹️ RC Car Control Layout**: Throttle joystick for smooth speed control + WASD buttons for precise servo positioning. Choose between **Car Mode** (Forward/Reverse) or **Drone/ESC Mode** (0-100% Throttle) for perfect control.
*   **🔒 Secure & Private**: Your data is yours. All profiles and settings are **encrypted** using industry-standard AES-256 encryption.
*   **⚙️ Total Customization**: Adjust joystick sensitivity (10% - 200%) and map custom Aux buttons to your exact needs.
*   **📡 Real-Time Telemetry**: View live data from your Arduino in the built-in Debug Console. No more guessing—see exactly what your device is sending.
*   **⚡ Auto-Connect**: Seamlessly reconnects if signal is lost, keeping you in control.

**Compatible With:**
*   **Latest Arduino Boards**: UNO Q (2025), UNO R4 WiFi (built-in BLE) - **Native Support Added!**
    * Official R4 WiFi and BLE-capable clones work out of the box (ArduinoBLE default profile + HM-10-compatible fallback).
*   **Bluetooth Classic**: HC-05, HC-06 (and all clones) - **Now with Military-Grade Stability**
*   **Bluetooth LE**: HM-10, AT-09, MLT-BT05, TI CC2541 (Generic Discovery Engine supports 99% of clones)
*   **Classic Arduino**: UNO, Nano, Mega with external modules
*   Any ESP32 or STM32 project!

**Get Started in Seconds:**
1.  Connect your Bluetooth module to your Arduino (RX/TX).
2.  Open Ardunakon and scan for devices.
3.  Connect and start driving!

*Download Ardunakon today and take control of your creations!*
