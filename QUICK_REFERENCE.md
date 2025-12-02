# Ardunakon Quick Reference Card

## 🚀 Quick Start (60 seconds)

1. **Upload Arduino Sketch** → Choose your board:
   - UNO Q / R4 WiFi: Built-in BLE (no module needed)
   - Classic UNO: Wire HC-05/HC-06 to pins 10 (RX) & 11 (TX)

2. **Open Ardunakon App** → Grant Bluetooth + Location permissions

3. **Connect** → Tap "Dev 1" → Select your device → Wait for GREEN

4. **Control** → Use joysticks to move!

---

## 📡 Bluetooth Module LED Codes

| LED Pattern | Meaning |
|-------------|---------|
| **Blinking fast (2x/sec)** | Not connected, ready to pair |
| **Solid ON** | Connected to phone |
| **Blinking slow (2sec)** | HC-05 AT mode (disconnect KEY pin!) |

---

## 🎨 App Status Colors

| Color | Status |
|-------|--------|
| 🔴 **RED** | Disconnected |
| 🟡 **YELLOW** | Connecting (wait 30 sec) |
| 🟢 **GREEN** | Connected & Active |

---

## 🕹️ Joystick Ranges

**Car Mode** (default):
- Center = 100 (neutral)
- Range: 0-200 (-100% to +100%)
- Use for: RC cars, robots with reverse

**Drone/Boat Mode**:
- Bottom = 0 (off)
- Range: 0-200 (0% to 100%)
- Use for: ESCs, drones, boats

---

## ⚡ Emergency Troubleshooting

### ❌ Can't find device in scan
```
→ Grant "Nearby Devices" + "Location" permissions
→ Unpair from Android Bluetooth settings
→ Restart Bluetooth on phone
```

### ❌ Connection fails
```
→ Wait 30 seconds (app tries 17 methods!)
→ Check module LED is blinking
→ Power cycle the Bluetooth module
```

### ❌ No data / garbage characters
```
→ Check wiring: TX→RX, RX→TX (don't cross TX→TX!)
→ Verify baud rate: Both Arduino & module = 9600
→ Add 100µF capacitor to module VCC/GND
```

### ❌ Connects then disconnects
```
→ Android Settings → Apps → Ardunakon → Battery → Unrestricted
→ Check signal strength (move closer)
→ Verify HC-06 baud rate: AT+BAUD4 (9600)
```

---

## 🔧 Essential AT Commands

### HC-05 / HC-06
```
AT              → Test (should reply "OK")
AT+VERSION      → Firmware version
AT+NAMEMyBot    → Set name
AT+BAUD4        → Set 9600 baud (RECOMMENDED)
AT+RESET        → Restart module
```

### HM-10 / HC-08 (BLE)
```
AT              → Test (no line ending!)
AT+VERS?        → Firmware version
AT+NAMEDrone1   → Set name
AT+BAUD0        → Set 9600 baud
AT+RESET        → Restart
```

**⚠️ Important**:
- HC-05/HC-06 need CR+LF line ending
- HM-10/HC-08 need NO line ending

---

## 📐 Standard Wiring (Classic UNO)

```
Bluetooth Module     Arduino UNO
────────────────     ───────────
VCC            →     5V
GND            →     GND
TX             →     Pin 10 (Software Serial RX)
RX             →     Pin 11 (Software Serial TX)
KEY/EN         →     NOT CONNECTED (leave floating)
```

**Motor Driver (L298N)**:
```
Arduino          L298N
───────          ─────
Pin 9 (PWM)  →   ENA (Left Speed)
Pin 8        →   IN1 (Left Dir 1)
Pin 7        →   IN2 (Left Dir 2)
Pin 6 (PWM)  →   ENB (Right Speed)
Pin 5        →   IN3 (Right Dir 1)
Pin 4        →   IN4 (Right Dir 2)
GND          →   GND
```

---

## 📊 Signal Strength Guide

| RSSI (dBm) | Quality | Action |
|------------|---------|--------|
| -40 to -60 | ⭐⭐⭐ Excellent | Perfect |
| -60 to -75 | ⭐⭐ Good | OK for most use |
| -75 to -85 | ⭐ Fair | Move closer |
| -85 to -100 | ❌ Poor | Too far / interference |

---

## 🧩 Module Compatibility Cheat Sheet

### ✅ Bluetooth Classic (HC-05/HC-06)
- **Success Rate**: 98-99% (17 connection methods)
- **Range**: 10-30 meters
- **Connection Time**: 2-20 seconds
- **Best For**: RC cars, robots, high-speed control

### ✅ BLE (HM-10 clones)
- **Success Rate**: 95% (7 UUID variants)
- **Range**: 5-15 meters
- **Connection Time**: 2-15 seconds
- **Best For**: Drones, low-power projects, latest phones

### 🆕 Arduino UNO Q / R4 WiFi
- **Success Rate**: 100% (native BLE)
- **Range**: 10-20 meters
- **Connection Time**: 2-5 seconds
- **Best For**: New projects, no external module

---

## 🎯 Protocol Packet Format

**10 bytes total**:
```
[START] [DEV_ID] [CMD] [D1] [D2] [D3] [D4] [D5] [CHECKSUM] [END]
  0xAA    0x01    0x0X  ...  ...  ...  ...  ...    XOR      0x55
```

**Commands**:
- `0x01` = Joystick data (20Hz)
- `0x02` = Button press/release
- `0x03` = Heartbeat (keepalive)
- `0x04` = Emergency stop

---

## 🔐 Security & Privacy

✅ **AES-256 Encrypted Profiles** - Your settings are secure
✅ **Zero Telemetry** - No tracking, no ads, 100% offline
✅ **Open Source** - Audit the code yourself on GitHub

---

## 💡 Pro Tips

1. **Save to Profile** → Auto-reconnect to your device instantly
2. **Lower Packet Rate (10Hz)** → Better reliability on weak signal
3. **Use Drone Mode** → For ESCs that don't support reverse
4. **Disable Battery Optimization** → Prevents Android from killing connection
5. **Name Your Modules** → `AT+NAMERoboCar1` makes scanning easier

---

## 🆘 Still Stuck?

1. **Check Debug Console** → Menu → Debug Console → See live connection attempts
2. **Test with Terminal** → Menu → Terminal → Send AT commands manually
3. **Read Full Guide** → Help → Troubleshooting tab (17+ solutions!)
4. **GitHub Issues** → https://github.com/metelci/ardunakon/issues

---

## 📱 App Shortcuts

| Action | Shortcut |
|--------|----------|
| Emergency Stop | Tap both joysticks |
| Reconnect | Swipe down on status card |
| Switch Profile | Top-left dropdown |
| Debug Console | Menu → Debug |
| Help | Menu → Help |

---

## ⚙️ Optimal Settings

**For RC Cars**:
- Joystick Mode: Car (-100% to +100%)
- Sensitivity: 100-150%
- Packet Rate: 20Hz

**For Drones**:
- Joystick Mode: Drone (0% to 100%)
- Sensitivity: 80-100%
- Packet Rate: 30Hz

**For Weak Bluetooth**:
- Packet Rate: 10Hz
- Lower sensitivity: 70%
- Enable auto-reconnect

---

## 📈 Version Info

**Current Version**: v0.1.4-alpha
**Tested Modules**: 50+ HC-06 clones, 30+ HM-10 variants
**Last Updated**: 2025-12-02

---

**Made with ❤️ for Arduino Makers**

For full documentation, visit: [GitHub Repository](https://github.com/metelci/ardunakon)
