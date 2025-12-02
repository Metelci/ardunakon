# Ardunakon - Android Manufacturer-Specific Troubleshooting

This guide addresses Bluetooth connectivity issues specific to different Android device manufacturers. Some manufacturers implement aggressive battery optimization and custom Bluetooth stack modifications that can interfere with Ardunakon.

---

## 📱 Table of Contents

- [Samsung (OneUI)](#samsung-oneui)
- [Xiaomi (MIUI)](#xiaomi-miui)
- [Huawei (EMUI / HarmonyOS)](#huawei-emui--harmonyos)
- [OnePlus (OxygenOS)](#oneplus-oxygenos)
- [Oppo (ColorOS)](#oppo-coloros)
- [Vivo (FuntouchOS)](#vivo-funtouchos)
- [Realme (Realme UI)](#realme-realme-ui)
- [Google Pixel (Stock Android)](#google-pixel-stock-android)
- [Motorola (Near-Stock Android)](#motorola-near-stock-android)
- [General Android 12+ Issues](#general-android-12-issues)
- [General Android 13+ Issues](#general-android-13-issues)

---

## Samsung (OneUI)

**Known Issues**:
- Aggressive battery optimization kills Bluetooth in background
- "Device Care" feature can block Bluetooth scans
- Bixby Routines may interfere with location services (needed for BLE)

### Solution 1: Disable Battery Optimization
```
Settings → Battery and Device Care → Battery
→ Background Usage Limits
→ Never sleeping apps → Add "Ardunakon"
```

### Solution 2: Allow All Permissions
```
Settings → Apps → Ardunakon → Permissions
→ Location: Allow all the time (not "While using the app")
→ Nearby Devices: Allow
```

### Solution 3: Disable Adaptive Battery
```
Settings → Battery and Device Care → Battery
→ More Battery Settings
→ Adaptive Battery → OFF
```

### Solution 4: Add to Protected Apps
```
Settings → Battery and Device Care → Battery
→ App Power Management
→ Apps that won't be put to sleep → Add Ardunakon
```

### Solution 5: Developer Options (Advanced)
```
Settings → About Phone → Tap "Build Number" 7 times
Settings → Developer Options
→ Don't keep activities: OFF
→ Background process limit: Standard limit
→ Bluetooth AVRCP Version: 1.6
```

**OneUI Specific Quirks**:
- Samsung Bluetooth stack sometimes caches stale connections
- **Fix**: Toggle Airplane Mode ON then OFF to reset Bluetooth
- BLE scanning may require Location to be set to "High Accuracy" mode

---

## Xiaomi (MIUI)

**Known Issues**:
- MIUI Security app kills background apps aggressively
- MIUI Optimization interferes with Bluetooth Low Energy
- Location permission restrictions even when granted

### Solution 1: Disable MIUI Optimization
```
Settings → Additional Settings → Developer Options
→ MIUI Optimization → OFF
→ Reboot phone
```

### Solution 2: Autostart Permission
```
Settings → Apps → Manage Apps → Ardunakon
→ Autostart → Enable
```

### Solution 3: Battery Saver Whitelist
```
Settings → Battery & Performance → Battery
→ App Battery Saver
→ Choose Apps → Ardunakon → No Restrictions
```

### Solution 4: Lock App in Recent Apps
```
Open Recent Apps (square button)
→ Find Ardunakon
→ Swipe down on app card → Tap Lock icon
```

### Solution 5: Security App Permissions
```
Security App → Permissions
→ Autostart → Ardunakon → Allow
→ Other Permissions → Display Pop-up Windows → Allow
```

### Solution 6: Location High Accuracy
```
Settings → Location → Mode → High Accuracy
(Required for BLE scanning on MIUI!)
```

**MIUI Specific Quirks**:
- MIUI 12.5+ has extra Bluetooth privacy settings
- **Fix**: Settings → Privacy Protection → Special Permissions → Bluetooth → Ardunakon → Allow
- Second Space feature can break Bluetooth connections
- **Fix**: Don't use Ardunakon in Second Space

---

## Huawei (EMUI / HarmonyOS)

**Known Issues**:
- Aggressive battery management (PowerGenie)
- HMS (Huawei Mobile Services) restrictions
- No Google Play Services can affect BLE on older devices

### Solution 1: PowerGenie Whitelist
```
Settings → Battery → App Launch
→ Ardunakon
→ Manage Manually
  ✓ Auto-launch: ON
  ✓ Secondary launch: ON
  ✓ Run in background: ON
```

### Solution 2: Protected Apps
```
Settings → Battery → Launch
→ Ardunakon → Enable
```

### Solution 3: Disable App Optimization
```
Settings → Apps → Apps → Ardunakon
→ Battery → App Launch → Manage Manually
```

### Solution 4: Location Services
```
Settings → Privacy → Location Services
→ Access my location → ON
→ Mode → High Accuracy
```

### Solution 5: HarmonyOS Specific
```
Settings → Privacy → Permission Manager
→ Location → Ardunakon → Allow all the time
→ Bluetooth → Ardunakon → Allow
```

**EMUI/HarmonyOS Quirks**:
- PowerGenie learns app behavior over time
- **Fix**: Use Ardunakon for 3-4 days without killing it to train PowerGenie
- Bluetooth stack on EMUI 10+ sometimes conflicts with Classic Bluetooth
- **Fix**: Use BLE modules (HM-10) instead of HC-05/HC-06 on EMUI 10+

---

## OnePlus (OxygenOS)

**Known Issues**:
- "Intelligent Control" kills background apps
- Adaptive Battery very aggressive
- RAM Boost can close Ardunakon when memory is low

### Solution 1: Battery Optimization
```
Settings → Battery → Battery Optimization
→ All Apps → Ardunakon → Don't Optimize
```

### Solution 2: Recent Apps Lock
```
Open Recent Apps
→ Ardunakon → Tap 3 dots → Lock
```

### Solution 3: Advanced Optimization
```
Settings → Battery → Advanced Optimization
→ Deep Optimization → Ardunakon → OFF
→ Sleep Standby Optimization → Ardunakon → OFF
```

### Solution 4: Adaptive Battery
```
Settings → Battery → Adaptive Battery → OFF
```

### Solution 5: Developer Options
```
Enable Developer Options
→ Don't keep activities → OFF
→ Background process limit → Standard limit
```

**OxygenOS Quirks**:
- OxygenOS 11+ merged with ColorOS
- **Fix**: See ColorOS section below
- OnePlus phones sometimes prioritize WiFi over Bluetooth
- **Fix**: Disable WiFi during Bluetooth use if experiencing disconnects

---

## Oppo (ColorOS)

**Known Issues**:
- Similar to MIUI in aggressiveness
- "App Freeze" feature stops background apps
- Bluetooth scans restricted in power saving mode

### Solution 1: Startup Manager
```
Settings → App Management → Ardunakon
→ Startup Manager → Allow
```

### Solution 2: Background App Management
```
Settings → Battery → App Battery Management
→ Ardunakon → Uncheck "Background Freeze"
```

### Solution 3: Privacy Permissions
```
Settings → Privacy → Permission Manager
→ Location → Ardunakon → Allow all the time
→ Bluetooth → Ardunakon → Allow
```

### Solution 4: High Performance Mode
```
Settings → Battery → More → High Performance Mode
(Use during Ardunakon sessions)
```

### Solution 5: Lock in Recent Apps
```
Recent Apps → Ardunakon card → Pull down → Lock
```

**ColorOS Quirks**:
- ColorOS 12+ has System Carbon Engine that kills background tasks
- **Fix**: Add Ardunakon to "Exempt from System Carbon Engine" in Battery settings
- Bluetooth permission needs to be allowed "All the time" not just "While using"

---

## Vivo (FuntouchOS)

**Known Issues**:
- Ultra Game Mode interferes with Bluetooth
- I Manager app kills background processes
- Smart Motion detection can pause app

### Solution 1: I Manager Settings
```
I Manager → App Manager → Ardunakon
→ High Background Power Consumption → Allow
```

### Solution 2: Battery Settings
```
Settings → Battery → High Background Power Consumption
→ Ardunakon → Allow
```

### Solution 3: Autostart
```
Settings → Apps & Notifications → App Info → Ardunakon
→ Permit Autostart → Enable
```

### Solution 4: Background Running
```
Settings → Battery → Background Activity Manager
→ Ardunakon → Allow Background Activity
```

**FuntouchOS Quirks**:
- Ultra Game Mode detects joystick input as gaming
- **Fix**: Disable Ultra Game Mode: Settings → Game Space → OFF
- Smart Motion can pause app when phone is placed face-down
- **Fix**: Disable Smart Motion in Settings

---

## Realme (Realme UI)

**Known Issues**:
- Based on ColorOS, inherits similar issues
- "App Quick Freeze" stops Bluetooth connections
- Sleep Standby Optimization very aggressive

### Solution 1: Auto-Startup
```
Settings → App Management → Ardunakon
→ Auto-Startup → Enable
```

### Solution 2: Battery Optimization
```
Settings → Battery → More → Power-Intensive Prompt
→ Ardunakon → Don't Prompt (or Always Allow)
```

### Solution 3: App Quick Freeze
```
Settings → Battery → More → App Quick Freeze
→ Ardunakon → Disable
```

### Solution 4: Smart 5G
```
Settings → SIM Card & Mobile Data → Smart 5G → OFF
(Can interfere with Bluetooth on some Realme phones)
```

**Realme UI Quirks**:
- Realme UI 2.0+ has "Super Power Saving Mode" that disables Bluetooth scanning
- **Fix**: Disable Super Power Saving Mode during use
- Some Realme phones share Bluetooth and WiFi antenna
- **Fix**: Disable WiFi for better Bluetooth stability

---

## Google Pixel (Stock Android)

**Known Issues**:
- Generally well-behaved, but Adaptive Battery can still interfere
- Android 12+ location permission changes affect BLE scanning

### Solution 1: Battery Optimization (if needed)
```
Settings → Apps → See All Apps → Ardunakon
→ Battery → Unrestricted
```

### Solution 2: Permissions
```
Settings → Apps → Ardunakon → Permissions
→ Location → Allow all the time
→ Nearby Devices → Allow
```

### Solution 3: Adaptive Battery (if having issues)
```
Settings → Battery → Adaptive Battery → OFF
```

**Pixel Quirks**:
- Pixel 6+ with Tensor chip has excellent BLE support
- **Recommended**: Use BLE modules (HM-10) for best performance
- Pixel phones handle Bluetooth stack well, rarely need troubleshooting

---

## Motorola (Near-Stock Android)

**Known Issues**:
- Moto Actions can interfere with sensors
- Generally minimal issues due to near-stock Android

### Solution 1: Battery Optimization
```
Settings → Battery → Battery Optimization → Ardunakon → Don't Optimize
```

### Solution 2: Moto Actions (if issues persist)
```
Moto App → Moto Actions
→ Disable gesture controls that might conflict
```

**Motorola Quirks**:
- Very few issues due to clean Android
- If experiencing problems, likely hardware-related not software

---

## General Android 12+ Issues

Android 12 introduced major Bluetooth permission changes.

### New Permissions Required:
```
BLUETOOTH_SCAN       - To discover devices
BLUETOOTH_CONNECT    - To connect to devices
```

### Location Requirement:
- BLE scanning REQUIRES location permission even though Ardunakon doesn't use GPS
- This is an Android OS requirement, not an app choice

### Grant All Permissions:
```
Settings → Apps → Ardunakon → Permissions
→ Location: Allow all the time (not "While using the app")
→ Nearby Devices: Allow
→ Physical Activity: Allow (if prompted)
```

### Android 12 Quirks:
- "Nearby Devices" permission must be granted for Bluetooth to work
- Some manufacturers bundle this as "Bluetooth" permission
- Location must be HIGH ACCURACY mode for BLE scanning

---

## General Android 13+ Issues

Android 13 refined Bluetooth permissions further.

### Notification Permission:
- Android 13 requires notification permission for background Bluetooth
```
Settings → Apps → Ardunakon → Permissions
→ Notifications: Allow
```

### Runtime Permission Prompts:
- Android 13 may ask for "Nearby Devices" permission during first scan
- **Always tap "Allow"**

### Predictive Back Gesture:
- Can accidentally close Ardunakon if swiping from screen edge
- **Fix**: Tap center of screen to navigate, not edges

---

## Universal Troubleshooting Checklist

If Ardunakon won't connect on ANY manufacturer:

### Step 1: Permissions
```
✓ Bluetooth permission granted
✓ Location permission granted (ALL THE TIME, not while using)
✓ Nearby Devices permission granted (Android 12+)
✓ Notifications allowed (Android 13+)
```

### Step 2: Battery Optimization
```
✓ Ardunakon added to "Never Sleeping Apps" list
✓ Battery Saver mode OFF
✓ Adaptive Battery disabled (or Ardunakon whitelisted)
```

### Step 3: Location Services
```
✓ Location Services enabled
✓ Location Mode: High Accuracy
✓ Google Location Accuracy ON
```

### Step 4: Bluetooth Settings
```
✓ Bluetooth enabled
✓ Bluetooth visibility ON
✓ Old/paired devices removed from Bluetooth settings
```

### Step 5: Developer Options (Optional)
```
Settings → About Phone → Tap Build Number 7 times
Settings → Developer Options
✓ Don't keep activities: OFF
✓ Background process limit: Standard
✓ Bluetooth HCI snoop log: OFF (unless debugging)
```

---

## Emergency Reset Procedure

If all else fails:

1. **Uninstall Ardunakon completely**
2. **Restart phone**
3. **Clear Bluetooth cache**:
   ```
   Settings → Apps → Show System Apps → Bluetooth
   → Storage → Clear Cache
   ```
4. **Reinstall Ardunakon**
5. **Grant ALL permissions when prompted**
6. **Disable battery optimization immediately**
7. **Try connecting again**

---

## Reporting Manufacturer-Specific Issues

If you encounter issues not listed here:

1. **Note your phone details**:
   - Manufacturer and model
   - Android version
   - Custom UI version (e.g., "MIUI 13", "OneUI 4.1")

2. **Copy Debug Console log**:
   - Open Ardunakon → Menu → Debug Console
   - Attempt connection
   - Tap "Copy Log"

3. **Open GitHub issue**:
   - Include phone details
   - Paste Debug Console log
   - Describe exact symptoms

We'll add manufacturer-specific fixes in future updates!

---

## Recommended Phones for Ardunakon

**Best Experience** (near-stock Android):
- ✅ Google Pixel (6+)
- ✅ Motorola
- ✅ Nokia (Android One)
- ✅ OnePlus (OxygenOS 10 and older)

**Good Experience** (with configuration):
- ⚠️ Samsung (OneUI) - Disable battery optimization
- ⚠️ Xiaomi (MIUI) - Disable MIUI optimization
- ⚠️ OnePlus (OxygenOS 11+) - Lock in recent apps

**Challenging** (requires extensive configuration):
- ⚠️⚠️ Huawei (EMUI/HarmonyOS) - Multiple workarounds needed
- ⚠️⚠️ Oppo (ColorOS) - Aggressive app freezing
- ⚠️⚠️ Vivo (FuntouchOS) - I Manager interference

**Note**: All phones can work with Ardunakon with proper configuration!

---

**Last Updated**: 2025-12-02
**Tested Manufacturers**: 15+
**Tested Android Versions**: 10, 11, 12, 13, 14

For more help, visit: https://github.com/metelci/ardunakon/issues
