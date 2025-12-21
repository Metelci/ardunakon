# ProGuard Verification

This directory contains scripts to verify that ProGuard rules are working correctly in release builds.

## Purpose

Release builds use ProGuard/R8 to minify and obfuscate code. Incorrect ProGuard rules can cause:
- Critical classes being stripped
- Runtime crashes that don't appear in debug builds
- Broken reflection-based features

## Scripts

### `verify-proguard.sh` / `verify-proguard.ps1`

Verifies that critical classes exist in the ProGuard mapping file after building a release APK.

**Usage (Linux/macOS):**
```bash
# Build release APK first
./gradlew assembleRelease

# Run verification
./scripts/verify-proguard.sh
```

**Usage (Windows):**
```powershell
# Build release APK first
.\gradlew assembleRelease

# Run verification
.\scripts\verify-proguard.ps1
```

**What it checks:**
- `AppBluetoothManager` - Core Bluetooth coordinator
- `BluetoothScanner` - Device discovery
- `BleConnectionManager` - BLE connection handling
- `ClassicConnectionManager` - Classic BT handling
- `ProtocolManager` - Protocol encoding/decoding
- `CryptoEngine` - Encryption operations
- `ProfileManager` - Profile persistence
- `WifiManager` - WiFi connectivity
- `TelemetryManager` - Telemetry parsing
- `ConnectionStateManager` - Connection state handling

**Exit codes:**
- `0` - All checks passed
- `1` - One or more classes missing (ProGuard rules broken)

## CI Integration

The verification runs automatically:
- **On PR:** When `proguard-rules.pro` or `build.gradle` changes
- **On main:** When ProGuard configuration changes
- **On release:** Before creating GitHub releases

See the `scripts/` folder for the ProGuard verification helpers.

## Troubleshooting

### "Mapping file not found"

**Cause:** Release APK hasn't been built yet.

**Fix:**
```bash
./gradlew assembleRelease
```

### "Class MISSING"

**Cause:** ProGuard rules in `app/proguard-rules.pro` are incorrect or incomplete.

**Fix:**
1. Identify the missing class from the script output
2. Add appropriate `-keep` rule to `proguard-rules.pro`
3. Rebuild and re-verify

**Example fix:**
```proguard
# If ProfileManager is missing, add:
-keep class com.metelci.ardunakon.data.ProfileManager { *; }
```

### Manual Inspection

You can also manually inspect the mapping file:
```bash
# View all classes from our package
grep "^com.metelci.ardunakon" app/build/outputs/mapping/release/mapping.txt

# Search for specific class
grep "ProfileManager" app/build/outputs/mapping/release/mapping.txt
```

## Maintenance

### Adding New Critical Classes

If you add new core functionality that should never be stripped:

1. Add the class name to the `CRITICAL_CLASSES` array in both scripts
2. Test with `./scripts/verify-proguard.sh`
3. If missing, add corresponding `-keep` rule to `proguard-rules.pro`

### Updating ProGuard Rules

When modifying `proguard-rules.pro`:
1. Make changes
2. Build release: `./gradlew assembleRelease`
3. Run verification: `./scripts/verify-proguard.sh`
4. Commit if verification passes

---

**Last Updated:** December 14, 2025
