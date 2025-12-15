# Ardunakon v0.2.7-alpha

**Release Date:** December 15, 2025  
**Build Number:** 24

## 🎯 Highlights

This release focuses on **code quality**, **WiFi connectivity improvements**, and **enhanced user experience** with better testability and smarter Arduino WiFi handling.

---

## ✨ New Features

### Platform Abstraction Layer
Removed hardcoded Android dependencies from business logic for improved testability and maintainability:

- **`PlatformInfo` Interface** - Abstracts device/OS information (SDK version, manufacturer, model)
- **`SystemServices` Interface** - Abstracts Bluetooth adapter and system services
- **Test Fakes** - `FakePlatformInfo` and `FakeSystemServices` for unit testing without Android dependencies
- **Hilt Integration** - Automatic dependency injection via `PlatformModule`

**Benefits:**
- ✅ Unit tests run without Robolectric
- ✅ Easy mocking of platform behavior
- ✅ Business logic independent of Android framework
- ✅ Better separation of concerns

### WiFi Auto-Fallback (Arduino R4 WiFi)
Arduino sketch v3.3 now features **intelligent WiFi mode selection**:

```
Boot → Try Station Mode (connect to router)
     ↓
     ├─ SUCCESS → Connected to your WiFi network
     │
     └─ FAIL → Automatically create AP mode (ArdunakonR4)
```

**What This Means:**
- 📡 Upload sketch once, works in both modes
- 🏠 Connects to your home WiFi if available
- 📶 Falls back to its own WiFi network if router unavailable
- 🔄 No more manual mode switching or sketch re-uploads

**Configuration:**
```cpp
// Station Mode (tries first)
const char* sta_ssid = "YOUR_WIFI_NAME";
const char* sta_password = "YOUR_PASSWORD";

// AP Mode (automatic fallback)
const char* ap_ssid = "ArdunakonR4";
const char* ap_password = "";  // Open network
```

### Enhanced Help & Documentation Dialog
- Increased dialog size to **95% of screen width and height** in all orientations
- Removed platform default width constraints for better visibility
- Improved readability on all screen sizes

---

## 🐛 Bug Fixes

### UI Layout
- **Fixed header icon spacing** in portrait mode (icons no longer overlap)
- Increased minimum spacing from 0dp to **4dp** for symmetrical layout
- Better visual balance across different screen sizes

---

## 🔧 Improvements

### Connection Stability
- **Telemetry broadcast rate** increased from 4s to **2s** (prevents timeout issues)
- **Heartbeat ACK response** added to maintain connection stability
- Better handling of WiFi connection timeouts

### Arduino Sketch Updates
- **v3.3** with automatic WiFi mode fallback
- Improved Serial Monitor output with clear mode indication
- Better error handling and status reporting

---

## 📊 Technical Details

### Files Changed
- **39 files** modified
- **3,323 lines** added
- **699 lines** removed

### New Components
- Platform abstraction layer (6 files)
- Enhanced test suites (4 files)
- Architecture Decision Records (4 ADR documents)

### Test Coverage Improvements
- New platform abstraction test fakes
- Enhanced Bluetooth manager tests
- WiFi manager edge case tests
- Performance benchmark tests

---

## 📦 Downloads

### Android APK
- **Minimum SDK:** Android 8.0 (API 26)
- **Target SDK:** Android 15 (API 35)
- **Size:** ~8 MB

### Arduino Sketches
All sketches included in the repository:
- `ArdunakonR4WiFi.ino` - **v3.3** with auto-fallback
- `ArdunakonClassicUno.ino` - For HC-05/HC-06 modules
- `ArdunakonUnoQ.ino` - For Arduino UNO Q (2025)

---

## 🔄 Upgrade Notes

### From 0.2.6-alpha
- No breaking changes
- All existing features preserved
- Arduino sketches backward compatible

### Arduino Sketch Update
If using Arduino R4 WiFi, update to v3.3 for auto-fallback feature:
1. Open `arduino_sketches/ArdunakonR4WiFi/ArdunakonR4WiFi.ino`
2. Update WiFi credentials (lines 61-62)
3. Upload to your Arduino R4 WiFi
4. Enjoy automatic mode switching!

---

## 🐛 Known Issues

None reported for this release.

---

## 📚 Documentation

- [CHANGELOG.md](https://github.com/Metelci/ardunakon/blob/main/CHANGELOG.md) - Full changelog
- [README.md](https://github.com/Metelci/ardunakon/blob/main/README.md) - Getting started guide
- [Arduino Sketches](https://github.com/Metelci/ardunakon/tree/main/arduino_sketches) - All sketches with documentation

---

## 🙏 Acknowledgments

Thanks to all users who provided feedback and bug reports!

---

## 📝 Full Changelog

See [CHANGELOG.md](https://github.com/Metelci/ardunakon/blob/main/CHANGELOG.md) for complete details.

**Previous Release:** [v0.2.6-alpha](https://github.com/Metelci/ardunakon/releases/tag/v0.2.6-alpha)
