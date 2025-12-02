# Ardunakon Documentation Index

Complete guide to all documentation available for Ardunakon.

---

## 📚 Documentation Structure

### Quick Start & Overview
- **[README.md](README.md)** - Main project overview, features, and quick start guide
- **[QUICK_REFERENCE.md](QUICK_REFERENCE.md)** - One-page cheat sheet for troubleshooting, wiring, and commands

### Setup Guides
- **[arduino_sketches/SETUP_GUIDE.md](arduino_sketches/SETUP_GUIDE.md)** - Detailed Arduino setup for all boards (UNO Q, R4 WiFi, Classic UNO)
- **[arduino_sketches/COMPATIBILITY.md](arduino_sketches/COMPATIBILITY.md)** - Hardware compatibility matrix and module support

### Troubleshooting
- **[HC06_TROUBLESHOOTING.md](HC06_TROUBLESHOOTING.md)** - Comprehensive HC-06 clone troubleshooting (17 connection methods explained)
- **[HC08_TROUBLESHOOTING.md](HC08_TROUBLESHOOTING.md)** - Complete HC-08 BLE module guide (6 problems solved)
- **[HM10_TROUBLESHOOTING.md](HM10_TROUBLESHOOTING.md)** - HM-10 & all clones (7 UUID variants, clone identification)
- **[ANDROID_MANUFACTURERS_GUIDE.md](ANDROID_MANUFACTURERS_GUIDE.md)** - Samsung, Xiaomi, Huawei, OnePlus, Oppo, Vivo specific fixes

### In-App Documentation (Offline)
Located in `app/src/main/assets/docs/`:
- **setup_guide.txt** - Arduino setup for all boards
- **troubleshooting.txt** - Bluetooth troubleshooting (HC-05, HC-06, HC-08, HM-10)
- **compatibility.txt** - Maximum Bluetooth compatibility report
- **app_features.txt** - Complete app features guide (NEW!)

---

## 🎯 Documentation by User Need

### "I'm new to Ardunakon, where do I start?"
1. Start with [README.md](README.md) - Overview and features
2. Follow [arduino_sketches/SETUP_GUIDE.md](arduino_sketches/SETUP_GUIDE.md) - Step-by-step Arduino setup
3. Keep [QUICK_REFERENCE.md](QUICK_REFERENCE.md) handy - Troubleshooting cheat sheet

### "I can't connect to my HC-06 module"
1. Read [HC06_TROUBLESHOOTING.md](HC06_TROUBLESHOOTING.md) - 17 connection methods explained
2. Check [In-App Help → Troubleshooting Tab](#in-app-help) - Offline guide with all HC-06 solutions
3. Try [QUICK_REFERENCE.md](QUICK_REFERENCE.md) - Quick emergency fixes

### "I can't connect to my HC-08 module"
1. Read [HC08_TROUBLESHOOTING.md](HC08_TROUBLESHOOTING.md) - Complete HC-08 BLE guide
2. Verify HC-08 is in transparent mode (`AT+MODE0`)
3. Enable notifications (`AT+NOTI1`)
4. Check [In-App Help → Troubleshooting](#in-app-help) - Section 3: HC-08

### "I can't connect to my HM-10 / AT-09 / MLT-BT05"
1. Read [HM10_TROUBLESHOOTING.md](HM10_TROUBLESHOOTING.md) - All clones covered
2. Identify your clone variant (uses nRF Connect app)
3. Wait 15 seconds for all 7 UUID attempts
4. Check [In-App Help → Troubleshooting](#in-app-help) - Section 4: HM-10

### "My phone keeps killing the Bluetooth connection"
1. Read [ANDROID_MANUFACTURERS_GUIDE.md](ANDROID_MANUFACTURERS_GUIDE.md) - Find your phone manufacturer
2. Follow manufacturer-specific battery optimization steps
3. Check [In-App Help → Troubleshooting](#in-app-help) - Section 5: General Bluetooth Troubleshooting

### "I want to learn all app features"
1. Read [In-App Help → Features Tab](#in-app-help) - Complete app features guide
2. Explore app Settings - All features explained with tooltips
3. Check [README.md](README.md) - Feature overview

### "Which Bluetooth module should I buy?"
1. Read [arduino_sketches/COMPATIBILITY.md](arduino_sketches/COMPATIBILITY.md) - Module compatibility matrix
2. Check [In-App Help → Compatibility Tab](#in-app-help) - Success rates and tested modules
3. See [HC06_TROUBLESHOOTING.md](HC06_TROUBLESHOOTING.md) - HC-06 clone identification guide

### "I'm getting garbage data from Arduino"
1. Check [QUICK_REFERENCE.md](QUICK_REFERENCE.md) - Wiring diagram and baud rate guide
2. Read [arduino_sketches/SETUP_GUIDE.md](arduino_sketches/SETUP_GUIDE.md) - Troubleshooting section
3. Verify protocol in [README.md](README.md) - Protocol reference section

---

## 📖 In-App Help

Access via: **Ardunakon App → Menu → Help**

### Tab 1: Setup
Complete Arduino setup guide with wiring diagrams for:
- Arduino UNO Q (2025) - Built-in BLE
- Arduino UNO R4 WiFi - Built-in BLE
- Classic Arduino UNO - HC-05/HC-06/HM-10
- Pin configuration and customization

### Tab 2: Troubleshooting
Comprehensive troubleshooting for ALL Bluetooth modules:
- **Section 1**: HC-06 Classic Bluetooth (7 common problems)
- **Section 2**: HC-05 Classic Bluetooth (4 common problems)
- **Section 3**: HC-08 Dual Mode (3 common problems)
- **Section 4**: HM-10 BLE (5 common problems)
- **Section 5**: General Bluetooth (5 common problems)
- **Section 6**: Advanced debugging techniques

### Tab 3: Compatibility
Maximum Bluetooth compatibility report:
- HC-05/HC-06: 17 connection methods, 98-99% success rate
- HM-10 Clones: 7 UUID variants, 95% success rate
- Technical implementation details
- Edge cases handled
- Comparison to other apps

### Tab 4: Features (NEW!)
Complete app features guide:
- **Section 1**: Main Control Screen (joysticks, status cards)
- **Section 2**: Dual Device Slots (control 2 Arduinos)
- **Section 3**: Profile Management (save configurations)
- **Section 4**: Aux Button Configuration (4 customizable buttons)
- **Section 5**: Joystick Settings (sensitivity, deadzone)
- **Section 6**: Debug Console (live packet monitoring)
- **Section 7**: Embedded Terminal (send AT commands)
- **Section 8**: Status Indicators (LED meanings, colors)
- **Section 9**: Settings & Preferences (all app settings)

---

## 🔧 Technical Documentation

### Protocol Specification
**10-byte packet format** @ 20Hz:
```
[START, DEV_ID, CMD, D1, D2, D3, D4, D5, CHECKSUM, END]
  0xAA   0x01   0x0X ...  ...  ...  ...  ...    XOR    0x55
```

**Detailed in**:
- [README.md](README.md) - Protocol Overview section
- [arduino_sketches/SETUP_GUIDE.md](arduino_sketches/SETUP_GUIDE.md) - Protocol Reference section
- In-App Help → Setup Tab

### Bluetooth Implementation
**HC-05/HC-06 (Classic Bluetooth)**:
- 17 connection methods
- Insecure + Secure SPP
- Reflection API (ports 1-3)
- 12 manufacturer UUIDs

**Detailed in**:
- [HC06_TROUBLESHOOTING.md](HC06_TROUBLESHOOTING.md) - Connection strategy section
- [arduino_sketches/COMPATIBILITY.md](arduino_sketches/COMPATIBILITY.md) - Technical implementation
- In-App Help → Compatibility Tab

**HM-10 (BLE)**:
- 7 UUID variants
- Smart characteristic fallback
- AT-09 and MLT-BT05 detection

**Detailed in**:
- [arduino_sketches/COMPATIBILITY.md](arduino_sketches/COMPATIBILITY.md) - BLE detection strategy
- In-App Help → Troubleshooting Tab - Section 4
- In-App Help → Compatibility Tab

---

## 📱 Platform-Specific Guides

### Android 12+
- New Bluetooth permissions (BLUETOOTH_SCAN, BLUETOOTH_CONNECT)
- "Nearby Devices" permission requirement
- Location permission still required for BLE scanning

**Covered in**:
- [ANDROID_MANUFACTURERS_GUIDE.md](ANDROID_MANUFACTURERS_GUIDE.md) - General Android 12+ Issues section
- In-App Help → Troubleshooting - Section 5

### Android 13+
- Notification permission for background Bluetooth
- Predictive back gesture handling
- Runtime permission prompts

**Covered in**:
- [ANDROID_MANUFACTURERS_GUIDE.md](ANDROID_MANUFACTURERS_GUIDE.md) - General Android 13+ Issues section

### Manufacturer-Specific
Detailed guides for:
- Samsung (OneUI) - 5 solutions
- Xiaomi (MIUI) - 6 solutions
- Huawei (EMUI/HarmonyOS) - 5 solutions
- OnePlus (OxygenOS) - 5 solutions
- Oppo (ColorOS) - 5 solutions
- Vivo (FuntouchOS) - 4 solutions
- Realme (Realme UI) - 4 solutions
- Google Pixel - Minimal issues
- Motorola - Near-stock Android

**All covered in**:
- [ANDROID_MANUFACTURERS_GUIDE.md](ANDROID_MANUFACTURERS_GUIDE.md)

---

## 🆘 Getting Help

### Self-Service Resources
1. **Quick Issue**: Check [QUICK_REFERENCE.md](QUICK_REFERENCE.md)
2. **HC-06 Problem**: Read [HC06_TROUBLESHOOTING.md](HC06_TROUBLESHOOTING.md)
3. **Phone-Specific**: See [ANDROID_MANUFACTURERS_GUIDE.md](ANDROID_MANUFACTURERS_GUIDE.md)
4. **General Setup**: Follow [arduino_sketches/SETUP_GUIDE.md](arduino_sketches/SETUP_GUIDE.md)
5. **Offline Help**: Use In-App Help (Menu → Help)

### Debug Tools
1. **Debug Console** (App → Menu → Debug Console)
   - Live connection logs
   - Packet monitoring
   - Error diagnostics
   - Copy log for support

2. **Embedded Terminal** (App → Menu → Terminal)
   - Send AT commands
   - Test module configuration
   - Manual command entry

3. **Arduino Serial Monitor**
   - Verify packet reception
   - Check checksum validation
   - Monitor connection status

### Reporting Issues
If documentation doesn't help:
1. **Open GitHub Issue**: https://github.com/metelci/ardunakon/issues
2. **Include**:
   - Phone model & Android version
   - Bluetooth module model
   - Debug Console log (copy from app)
   - Arduino Serial Monitor output
   - Steps to reproduce

---

## 📊 Documentation Coverage

### ✅ Complete Coverage
- [x] Arduino setup (all boards)
- [x] HC-05/HC-06 troubleshooting (17 connection methods)
- [x] HC-08 troubleshooting (dedicated guide)
- [x] HM-10/BLE troubleshooting (7 UUID variants, all clones)
- [x] Android manufacturer-specific fixes (9 manufacturers)
- [x] Protocol specification
- [x] Wiring diagrams
- [x] AT commands reference (HC-05/06, HC-08, HM-10)
- [x] App features guide (9 sections)
- [x] Quick reference card
- [x] Offline in-app help (4 tabs)
- [x] Clone identification guides

### 📈 Documentation Stats
- **Total Pages**: 13+ comprehensive guides
- **Troubleshooting Solutions**: 150+ specific fixes
- **Supported Modules**: HC-05, HC-06 (17 methods), HC-08 (6 problems), HM-10 (7 variants)
- **Phone Manufacturers**: 9 detailed guides
- **Android Versions**: 10, 11, 12, 13, 14
- **In-App Tabs**: 4 (Setup, Troubleshooting, Compatibility, Features)

---

## 🔄 Documentation Updates

### Latest Additions (2025-12-02)
- ✨ **NEW**: App Features Guide (in-app documentation)
- ✨ **NEW**: Android Manufacturers Guide (Samsung, Xiaomi, Huawei, etc.)
- ✨ **NEW**: Quick Reference Card (one-page cheat sheet)
- ✨ **NEW**: Documentation Index (this file)
- ✅ Updated HelpDialog to include 4th tab (Features)

### Previous Updates
- **2025-11-30**: HC-06 Troubleshooting Guide (17 connection methods)
- **2025-11-30**: Compatibility Matrix (UUID variants documented)
- **2025-11-29**: Setup Guide for Arduino UNO Q and R4 WiFi
- **2025-11-28**: Initial documentation structure

---

## 📝 Contributing to Documentation

Found a typo or have a suggestion?
1. Open issue on GitHub
2. Submit pull request with fix
3. Include:
   - What was wrong
   - What should be correct
   - Which document

**Documentation style guide**:
- Use clear, concise language
- Include code examples where relevant
- Add step-by-step instructions for complex tasks
- Use emojis sparingly (only for visual organization)

---

## 📄 License

All documentation is part of the Ardunakon project.
See main [README.md](README.md) for license information.

---

**Last Updated**: 2025-12-02
**Documentation Version**: 2.0
**App Version**: v0.1.4-alpha

For the latest documentation, visit: https://github.com/metelci/ardunakon
