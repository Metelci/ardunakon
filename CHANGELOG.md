# Changelog

All notable changes in Ardunakon will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.2.10-alpha-hotfix4] - 2025-12-23

### Changed
- Custom command buttons now show 3 per side (6 total, up from 4)
- Electric yellow glowing border effect on all custom command buttons
- Enhanced visual prominence for custom command placeholders

## [0.2.10-alpha-hotfix3] - 2025-12-23

### Fixed
- Haptic feedback toggle now actually works (was being ignored)

### Changed
- Wider SCAN button for better visibility when disconnected
- Created HapticController singleton for centralized haptic management

## [0.2.10-alpha-hotfix2] - 2025-12-23

### Added
- Custom command buttons visible on main control screen (up to 4 commands)
- Keyboard shortcut support for custom commands (A-Z, excluding reserved keys)

### Changed
- Internal code improvements and refactoring

## [0.2.10-alpha-hotfix1] - 2025-12-22

### Changed
- Internal code improvements and refactoring
- Enhanced code organization and maintainability

## [0.2.10-alpha] - 2025-12-22

### Added
- **Custom Command Extensions**: Full framework for defining, saving, and sending custom 10-byte protocol commands (IDs 0x20-0x3F).
  - Includes hex payload editor, color picker, icon selector, and persistence via encrypted JSON.
- **Joystick Sensitivity Improvements**: Added real-time descriptive help text in Settings explaining the impact of the curve on control precision.
- **WiFi Encryption Transparency**: Added UI verification to ensure correctly omitted security icons for unencrypted connections.

### Fixed
- **Test Stability**: Resolved a project-wide unresolved reference bug for `assertDoesNotExist()` in Compose UI tests by implementing an `assertCountEquals(0)` workaround and explicitly adding `ui-test-core`.
- **Hilt Testing**: Fixed compilation errors in `ControlViewModelTest` by correctly mocking the new `CustomCommandRegistry`.

## [0.2.9-alpha] - 2025-12-21

### Added
- BLE throughput tuning: 2M PHY + 517 MTU negotiation when supported
- Bounded BLE write queue (150 packets) to handle bursts safely
- Foreground/background-aware monitoring cadence for reduced battery use
- RTT-adaptive WiFi heartbeat tuning (1.5s - 5s)
- Additional Compose UI tests for key dialogs (telemetry graphs + encryption errors)

### Changed
- Protocol transmission now suppresses duplicates and caps joystick/control sends to ~60fps
- WiFi discovery timeout increased to 4s; WiFi auto-reconnect now stops after 3 failed attempts
- Mode switching resets protocol caches/session state to prevent stale traffic

## [0.2.8-alpha] - 2025-12-20

### Added
- Formal protocol specification document (PROTOCOL_SPEC.md)
- ProGuard rule verification scripts for CI/CD
- ViewModel unit test suite (ControlViewModelTest)
- Combined StateFlow optimization reducing recompositions by 40%
- Joystick event throttling to 20Hz
- Debug log entry limit (500 maximum)
- Comprehensive KTLint code formatting standards implementation
- KTLint violation fixes for critical formatting issues

### Changed
- Arduino R4 WiFi sketch reorganization (snake game moved to examples)
- All Arduino comments translated to English
- Build verification includes ProGuard mapping check
- KTLint formatting standards are enforced in the project

### Added
- **Test Coverage Improvements**: Telemetry package coverage increased from 0% to 72%
- **UI Component Tests**: New ControlHeaderBarTest with Robolectric and Compose Test
- **Testability Refactor**: CryptoEngine and CoroutineScope injection for unit testing

### Fixed
- Jacoco report generation (removed incompatible doFirst block)
- TelemetryHistoryManagerTest now targets production class correctly

### Changed
- AppBluetoothManager refactored for dependency injection
- BluetoothScanner updated to accept injected CryptoEngine
- DI modules (AppModule, DataModule, BluetoothModule) updated for CryptoEngine

## [0.2.7-alpha-hotfix3] - 2025-12-19

### Added
- Extended Arduino board support:
  - **Arduino Mega 2560** (Hardware Serial 1)
  - **Arduino Leonardo / Micro** (Hardware Serial 1)
  - **Arduino Due** (3.3V Logic, Hardware Serial 1)
  - **Arduino Zero / M0** (3.3V Logic, Hardware Serial 1)
  - **Arduino GIGA R1 WiFi** (Dual Mode BLE/WiFi)

### Fixed
- Fixed critical corruption in `ArdunakonR4WiFi` sketch.
- Corrected servo pin documentation in setup guides.

## [0.2.7-alpha-hotfix2] - 2025-12-19

### Fixed
- Deprecated Gradle property assignments in build.gradle
- Build stability improvements

## [0.2.7-alpha-hotfix1] - 2025-12-17

### Fixed
- Servo Z reliability: A/Z controls are now encoded only in `CMD_JOYSTICK (0x01)` `auxBits` (no heartbeat control packets)

### Changed
- Joystick packets no longer send continuously when all inputs are neutral (one final neutral packet is sent on release)

## [0.2.7-alpha] - 2025-12-15

### Added
- **Platform Abstraction Layer:** Removed hardcoded Android dependencies from business logic
  - New `PlatformInfo` interface for device/OS information
  - New `SystemServices` interface for Bluetooth and system services
  - Improved testability with `FakePlatformInfo` and `FakeSystemServices`
- **WiFi Auto-Fallback:** Arduino R4 WiFi sketch now automatically switches modes
  - Tries Station mode (connect to router) first
  - Falls back to AP mode if router unavailable
  - Single sketch upload works in both scenarios
- Enhanced Help & Documentation dialog
  - Increased to 95% screen width/height in all orientations
  - Removed platform default width constraints for better visibility

### Fixed
- Header icon spacing in portrait mode (icons no longer overlap)
- Minimum spacing increased from 0dp to 4dp for symmetrical layout

### Changed
- Arduino R4 WiFi sketch v3.3 with intelligent WiFi mode selection
- Telemetry broadcast rate increased from 4s to 2s (prevents timeout issues)
- Heartbeat ACK response added to maintain connection stability


## [0.2.6-alpha] - 2025-12-14

### Fixed
- **CRITICAL:** Motor spin bug on BLE reconnect (Android 15/16)
- Stale joystick values causing unexpected motor activation on reconnect

### Changed
- Transmission loop now checks connection state before sending packets
- Joystick and servo values automatically reset to neutral on disconnect
- BLE write queue cleared on new connection to prevent stale packet bursts
- 200ms startup delay after BLE connection for stack stabilization

## [0.2.5-alpha] - 2025-12-13

### Removed
- 4 unused files (~450 lines): duplicate debug console, unused sensor dashboard
- Orphaned security components: HandshakeManager, TrustStore classes

### Changed
- Arduino sketches reorganized: moved examples to dedicated examples/ directory
- Comprehensive documentation added for all Arduino sketches
- Pin configuration reference and Bluetooth compatibility matrix updated

## [0.2.4-alpha] - 2025-12-12

### Changed
- ControlScreen.kt reduced from 2,033 to 1,030 lines (-49%)
- Extracted reusable components: ControlHeaderBar, JoystickPanel, ServoPanel
- Moved dialogs to dedicated folder
- Created ControlViewModel for state management

### Added
- Haptic feedback on joystick center deadzone and edge boundaries
- Connection quality ring around joystick (latency-based color: green/yellow/orange/red)

### Removed
- Profile management UI (app now uses single default car/rover profile)
- Light theme (dark theme only)

### Changed
- Updated to Gradle 8.11.1
- Resolved all Kotlin compilation warnings

## [0.2.3-alpha] - 2025-12-11

### Fixed
- Battery voltage and device status display on WiFi connections (R4 WiFi / Uno Q)
- WiFi telemetry data now plots on real-time graph
- Layout overflow in Telemetry Graph window (landscape mode)
- Help menu buttons accessibility on smaller screens

### Changed
- Throttled battery telemetry logs to once every 5 seconds (reduced console spam)

## [0.2.2-alpha] - 2025-12-10

### Fixed
- R4 WiFi boards now correctly use BLE connection mode
- Motor/servo control failures from incorrect Classic Bluetooth attempts

### Added
- Improved device detection: "ARDUNAKON", "ARDUINO", "R4", "UNO R4" to BLE-only list
- Servo commands via debug terminal (W/A/L/R/B)
- Toggle behavior for servo controls

### Changed
- System status bar visibility (correct light/dark contrast in portrait mode)
- Header button spacing improved (12-16dp touch targets)

### Fixed
- Compiler warnings (unused variables, name shadowing)
- CancellationException handling in coroutines

## [0.2.1-alpha] - 2025-12-09

### Changed
- R4 WiFi servo pins: Pin 6 (X-axis), Pin 7 (Y-axis)
- Added brushless ESC support on Pin 9

### Removed
- DC motor driver code for simplified servo + ESC setups

### Fixed
- Servo Library compatibility (requires v1.2.2+ for R4 WiFi PWM)

### Added
- Updated setup guide with new R4 WiFi wiring diagram
- ESC throttle control notes

## [0.2.0-alpha] - 2025-12-08

### Added
- **Arcade Drive:** Single joystick controls both throttle and steering
- **Dedicated Servo Control:** W/A/L/R buttons control two servos (Pins 6 & 7)
- Universal support across all boards: Classic UNO, UNO R4 WiFi, UNO Q

## [0.1.84-alpha] - 2025-12-07

### Changed
- Forced WRITE_NO_RESPONSE for BLE writes (HM-10/MLT-BT05)
- Improved reconnection handling on transient GATT failures

### Added
- Numeric axis labels on telemetry charts (dBm, volts, ms)

### Changed
- Restored embedded debug panel on right side
- Removed redundant keyboard state labels

## [0.1.75-alpha] - 2025-12-06

### Fixed
- HC-06 on Xiaomi/MIUI: Auto-enable reflection fallback
- Stream initialization delay (500ms) for Xiaomi devices
- "Read failed" errors on MIUI blocking

### Added
- Device diagnostics logging (manufacturer/model/Android version)
- Xiaomi-first reflection strategy (Port 1 before standard SPP)
- Connected status visual (green shadow on device buttons)
- Servo button logging to debug window

## [0.1.7-alpha] - 2025-12-05

### Added
- Dual BLE profile for R4 WiFi (ArduinoBLE default + HM-10 UUIDs)
- R4 WiFi clone support documentation

### Fixed
- HC-06 on Xiaomi/MIUI via automatic Reflection Port 1 fallback

### Changed
- Updated attribution in all Arduino sketches

## [0.1.6-alpha] - 2025-12-04

### Changed
- Left joystick → Throttle control (vertical-only, no horizontal drift)
- Right joystick → WASD discrete servo buttons
- Throttle behavior: non-centering Y-axis

### Added
- Auto-reconnect toggle buttons (per-device control)
- Persistent auto-reconnect preferences

### Fixed
- BLE GATT Error 147 handling with intelligent retry logic
- Transient error detection (8, 129, 147 vs permanent 133)
- Exponential backoff (2s → 4s → 6s)
- Device name resolution ("Connecting to null" logs)

### Changed
- Pre-flight Bluetooth adapter checks with 500ms settling delay
- Device names refreshed on every connection attempt
- Compact UI labels to accommodate debug panel

## [0.1.5-alpha] - 2025-12-03

### Added
- Bounded write queue (100-packet limit) for BLE connections
- 3-strike retry logic for Classic Bluetooth (tolerates transient interference)

### Changed
- BLE connection timeout extended to 15 seconds
- Joysticks auto-center on release
- Clear labeling: "Movement (Servos)" and "Throttle (Speed)"
- Labels beside joysticks for better visibility

### Fixed
- Joystick alignment (centered in screen halves)
- Enhanced write error handling triggers reconnection
- Circuit breaker prevents connection hammering (10 attempt limit)

### Changed
- Help icon: three-dot menu → question mark (?) icon

## [0.1.4-alpha] - 2025-12-02

### Fixed
- Layout overlapping with system notification bar and navigation buttons
- Joystick alignment at same height with consistent label positioning

### Changed
- Centered control layout (E-STOP, signal indicators, buttons)
- Electric yellow device buttons for instant visibility
- Theme toggle relocated to top bar
- Enabled edge-to-edge display with proper WindowInsets

### Removed
- Redundant Bluetooth icons

---

## Version Naming Convention

- **Alpha (0.x.x):** Experimental features, breaking changes possible
- **Beta (1.0.0-beta.x):** Feature-complete, stability testing
- **Stable (1.0.0+):** Production-ready releases

**Note:** Pre-1.0 versions follow incrementing minor/patch versions. Post-1.0 will follow strict semantic versioning.

---

## Contributors

- **Metelci** - Owner and Lead Contributor

**Last Updated:** December 17, 2025
