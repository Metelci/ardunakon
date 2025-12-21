# Connection Flow Scenarios (Test Matrix)

These scenarios are intended for UI/instrumentation tests and high-level manual QA. They focus on the user-visible
connection flows (Bluetooth/BLE/WiFi) and mode switching behavior.

## Bluetooth / BLE

- Scan → select device → connect succeeds (status updates; actions enabled).
- Scan returns duplicates / same device multiple times (UI shows one entry; selection still works).
- Connect fails (timeout / error) (error surfaced; retry/scan still available).
- Auto-reconnect enabled: disconnect → reconnect attempts start; stop after max attempts.
- Auto-reconnect disabled: disconnect → no reconnect attempts; manual reconnect works.
- Reconnect with stale/queued packets: verify no stale controls are sent after reconnect.

## WiFi

- Discovery finds device → connect succeeds.
- Discovery timeout (4s) with no devices found → manual IP/port entry still works.
- Connect fails → retry policy stops after max attempts; UI remains responsive.
- Encrypted handshake success: lock indicator shown; traffic allowed.
- Encrypted handshake failure: error dialog shown; sensitive details not leaked; actions (Retry/Continue/Disconnect) work.
- Unencrypted mode (if allowed): lock indicator hidden; traffic still functional.

## Mode Switching / Session Cleanup

- Connected on Bluetooth → switch to WiFi: Bluetooth disconnects; protocol/session cache reset; no stale packets sent.
- Connected on WiFi → switch to Bluetooth: WiFi disconnects; protocol/session cache reset; no stale packets sent.
- Rapid switching (WiFi ↔ Bluetooth) does not crash and leaves a consistent UI state.

## App Lifecycle / Battery

- Foreground → background: monitoring cadence decreases; no excessive wakeups.
- Background → foreground: monitoring cadence returns to normal.
- Screen rotation or process recreation: connection mode and UI state restore cleanly.

