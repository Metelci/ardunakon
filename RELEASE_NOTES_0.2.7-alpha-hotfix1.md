# Ardunakon v0.2.7-alpha-hotfix1

## Fixes

- Servo Z (A/Z) is now controlled only via `auxBits` inside `CMD_JOYSTICK (0x01)`.
- `CMD_HEARTBEAT (0x03)` remains keepalive-only (no control logic).
- Control packets stop sending when all inputs are neutral (one final neutral packet is sent on release).

