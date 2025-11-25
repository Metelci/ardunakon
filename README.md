# Ardunakon 🎮📡
**The Ultimate Arduino Bluetooth Controller**

Ardunakon is a professional-grade Android application designed to control Arduino projects (RC cars, drones, boats, robots) via Bluetooth Classic (HC-05/HC-06) or BLE (HM-10). It replaces generic controller apps with a robust, crash-proof, and highly customizable interface.

## 🚀 Key Features

### 🕹️ Precision Control
*   **Dual Joysticks**: Dedicated Left (Movement) and Right (Throttle) sticks.
*   **Smart Throttle Modes**:
    *   **Car Mode**: Bidirectional control (-100% to +100%) for forward/reverse.
    *   **Drone/Boat Mode**: Unidirectional control (0% to 100%) for ESCs.
*   **Custom Sensitivity**: Adjust joystick response from 10% to 200% to match your driving style.

### ⚙️ Powerful Customization
*   **Profile Manager**: Create unlimited profiles for different projects.
*   **Button Config**: 4 Aux buttons with customizable labels, commands, and colors.
*   **Live Editing**: Tweak settings instantly without disconnecting.

### 📡 Real-World Connectivity
*   **Bidirectional Data**: View real-time feedback from your Arduino in the Debug Console.
*   **Auto-Reconnect**: Automatically restores connection if signal is lost.
*   **Zero Fakes**: No mock data. Every signal strength bar and log entry is real.

## 🛠️ Getting Started

### 1. Installation
1.  Download and install the APK on your Android device (Android 12+ recommended).
2.  Grant **Bluetooth** and **Location** permissions when prompted (required for scanning).

### 2. Arduino Setup
Flash your Arduino with the provided sketch. The app uses a simple 10-byte binary protocol.

**Wiring (HC-05/06):**
*   **VCC** → 5V
*   **GND** → GND
*   **TX** → Arduino Pin 10 (RX)
*   **RX** → Arduino Pin 11 (TX)

### 3. Connecting
1.  Open Ardunakon.
2.  Tap **"Dev 1"** (Slot 1) or **"Dev 2"** (Slot 2).
3.  Select your Bluetooth module from the list.
4.  The status card will turn **Green** upon connection.

## 📝 Protocol Overview
Ardunakon sends a fixed 10-byte packet @ 20Hz:
`[START, DEV_ID, CMD, LX, LY, RX, RY, AUX, CS, END]`

*   **START**: `0xAA`
*   **LX/LY/RX/RY**: `0-200` (100 is center)
*   **END**: `0x55`

## 🐞 Troubleshooting
*   **"Permission Denied"**: Go to Android Settings -> Apps -> Ardunakon -> Permissions and allow "Nearby Devices".
*   **No Data Received**: Check your Arduino TX/RX wiring (TX must go to RX).
*   **App Closes in Background**: Disable "Battery Optimization" for Ardunakon.

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

*   **🕹️ Precision Dual Joysticks**: Dedicated sticks for Movement and Throttle. Choose between **Car Mode** (Forward/Reverse) or **Drone/ESC Mode** (0-100% Throttle) for perfect control.
*   **🔒 Secure & Private**: Your data is yours. All profiles and settings are **encrypted** using industry-standard AES-256 encryption.
*   **⚙️ Total Customization**: Create unlimited profiles for all your projects. Adjust joystick sensitivity (10% - 200%) and map custom buttons to your exact needs.
*   **📡 Real-Time Telemetry**: View live data from your Arduino in the built-in Debug Console. No more guessing—see exactly what your device is sending.
*   **⚡ Auto-Connect**: Seamlessly reconnects if signal is lost, keeping you in control.

**Compatible With:**
*   Bluetooth Classic (HC-05, HC-06)
*   Bluetooth LE (HM-10, AT-09)
*   Any Arduino, ESP32, or STM32 project!

**Get Started in Seconds:**
1.  Connect your Bluetooth module to your Arduino (RX/TX).
2.  Open Ardunakon and scan for devices.
3.  Connect and start driving!

*Download Ardunakon today and take control of your creations!*
