# Changelog

## 0.2.21-alpha (Build 52)
- **Debug Terminal Controls**: Added visible maximize and minimize buttons to terminal header for quick access.
- **Cleaner UI**: Removed auto-hide feature for simpler terminal management.
- **Better UX**: Direct button access to maximize (fullscreen) and minimize (hide) terminal functions.

## 0.2.20-alpha (Build 51)
- **Granular Connection Errors**: Added specific error types (Timeout, DeviceNotFound, ConnectionRejected, etc.).
- **UI Feedback**: Control screen now displays localized, precise error messages via Snackbars.
- **Manager Enhancements**: Bluetooth and WiFi managers now catch identifying exceptions and map them to user-friendly errors.

## 0.2.19-alpha (Build 50)
- **Visual Joystick Deadzone**: Added configurable deadzone with visible ring indicator (30% opacity).
- **Terminal Colors**: Implemented color-coded logs (TX=Green, RX=White, Error=Red).
- **Stability Fixes**: Fixed joystick crash on small screens/non-standard resolutions.

## 0.2.17-alpha-hotfix1 (Build 48)
- **Servo Z A/Z Fix**: Aligned AUX bits between app and Arduino so A/Z reliably drive Servo Z min/max.
- **Protocol Cleanup**: Dedicated AUX_Z bit added instead of reusing the A/B bit.

## 0.2.17-alpha (Build 47)
- **Docs Readability**: Setup and Compatibility pages use a darker gradient with black text for improved contrast.
- **Help & Docs Layout**: Help and documentation dialogs use 90% height in portrait mode.

## 0.2.16-alpha (Build 46)
- **Jetpack Navigation Compose**: Added type-safe navigation with `@Serializable` routes and deep link support (`ardunakon://control`, `ardunakon://onboarding`).
- **Detekt Static Analysis**: Integrated Detekt 1.23.7 for code quality checks with custom Android/Compose rules.
- **Architecture Improvements**: Extracted system dialogs to `ui/dialogs/` and created injectable `PermissionManager` for better testability.
- **Package Consolidation**: Merged duplicate `util/` and `utils/` directories for cleaner structure.

## 0.2.15-alpha (Build 45)
- **Test Infrastructure**: Migrated 10+ E2E tests to HiltTestActivity, eliminating `setContent` collisions and ensuring reliable UI automation.
- **Stability Fixes**: Resolved compilation issues in tutorial screens and fixed color calculation logic in latency tests.
- **Verification**: Validated exponential backoff strategies and interface adherence across all connection managers.

## 0.2.14-alpha (Build 44)
- **100% Test Coverage**: Achieved 100% unit test pass rate (1264+ tests) with comprehensive Bluetooth & WiFi coverage.
- **Robust WiFi Connectivity**: Enhanced auto-reconnect logic and state management for reliable UDP sessions.
- **Bluetooth Stability**: Improved Classic Bluetooth connection handling and state debouncing.
- **Quality Assurance**: Added detailed test suites for connection managers and internal state logic.

## 0.2.13-alpha (Build 43)
- **Custom Commands Landscape**: Two-column layouts for command list and editor dialogs.
- **Compact Settings Slider**: Joystick sensitivity control optimized for space.
- **Tutorial Fix**: Onboarding screens now respect system notification bar.
- Target SDK: 35, Min SDK: 26.

## 0.2.10-alpha-hotfix3
- **Custom Command Buttons**: First 4 commands visible on main control screen.
- **Keyboard Shortcuts**: Assign A-Z keys (except reserved) to custom commands.
- **Dynamic SCAN Button**: Shows "SCAN" when disconnected, RSSI/latency when connected.

## 0.2.10-alpha
- **Custom Commands**: Create, save, and send up to 16 user-defined 10-byte protocol commands (0x20-0x3F) with a built-in hex editor and custom icons.
- **Joystick Sensitivity Help**: Added dynamic help text in Settings to clarify how sensitivity curves impact control responsiveness.
- **Quality**: Resolved complex Compose UI test compilation issues and added verification for unencrypted WiFi status icons.

## 0.2.9-alpha
- **BLE Throughput**: 2M PHY + 517 MTU negotiation (when supported), balanced connection priority, and a 150-packet bounded write queue.
- **Network Efficiency**: Duplicate suppression + ~60fps rate limiting for joystick/control packets to reduce saturation.
- **Battery Optimization**: Foreground/background-aware monitoring cadence to reduce background power draw.
- **Adaptive WiFi**: 4s discovery timeout, 3 max reconnect attempts, and RTT-adaptive heartbeat (1.5s - 5s).
- **Docs & Tests**: Updated architecture diagrams and added Compose UI tests for key dialogs.

## 0.2.7-alpha-hotfix1
- **Servo Z Fix (A/Z)**: A/Z are now encoded only in `auxBits` inside `CMD_JOYSTICK (0x01)` (no heartbeat control packets).
- **No Idle Spam**: Joystick packets stop sending when all inputs are neutral (sends one final neutral packet on release).

## 0.2.7-alpha
- **Platform Abstraction Layer**: Removed hardcoded Android dependencies for better testability.
- **WiFi Auto-Fallback**: Arduino R4 WiFi sketch now automatically tries router connection first, falls back to AP mode.
- **Larger Help Dialog**: Increased to 95% screen coverage in all orientations.
- **Better Layout**: Fixed header icon spacing in portrait mode (no more overlapping).

## 0.2.6-alpha
- **Critical BLE Safety Fix**: Fixed motor unexpectedly spinning on BLE reconnect (Android 15/16).
- **Connection State Check**: Transmission loop now only sends when actually connected.
- **State Reset on Disconnect**: Joystick/servo values reset to neutral on disconnect to prevent stale control values.
