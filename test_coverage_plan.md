# Test Coverage Report & Implementation Plan

## Current Coverage Status

### Package Coverage Summary

**High Coverage (>= 70%)**:
- ui.utils: 100%
- utils: 99%  
- platform: 90%
- data: 86%
- protocol: 73%
- telemetry: 70%

**Medium Coverage (40-69%)**:
- model: 60%
- security: 42%

**Low Coverage (< 40%)**:
- **bluetooth: 26%** ❌ PRIORITY 1
- **ui: 26%** ❌ PRIORITY 2  
- **service: 0%** ❌ PRIORITY 3
- ui.accessibility: 0%
- util: 25%
- di: 0% (expected)

## Priority 1: Bluetooth Package (26% → 70%+)

### BluetoothManagerTest (15-20 tests needed):
- Device discovery & scanning
- Connection state transitions
- Auto-reconnection logic
- Classic vs BLE switching
- Permission handling
- Error recovery

### BleConnectionManagerTest (12-15 tests needed):
- GATT connection lifecycle
- Service/characteristic discovery
- Data transmission (write/notify)
- MTU negotiation
- Error handling

### ClassicBluetoothManagerTest (10-12 tests needed):
- RFCOMM socket management
- SPP profile handling
- Data streaming
- Connection stability

## Priority 2: UI Layer (26% → 60%+)

### ControlViewModelTest (20-25 tests needed):
- Screen state management
- Joystick/servo input handling
- Command transmission
- Telemetry display updates

### ScanViewModelTest (12-15 tests needed):
- Device list management
- Scan lifecycle
- Filter/sort functionality

### OnboardingViewModelTest (8-10 tests - expand existing):
- Tutorial flow navigation
- Permission requests
- First-time setup

## Priority 3: Service Layer (0% → 50%+)

### BluetoothServiceTest (8-10 tests needed):
- Service lifecycle
- Foreground notifications
- Background connection management
- System event handling

## Implementation Phases

**Phase 1 (Bluetooth)**: ~15-20 hours
**Phase 2 (UI)**: ~20-25 hours
**Phase 3 (Service)**: ~8-10 hours
**Phase 4 (Polish)**: ~10-12 hours

**Target**: 75%+ overall coverage
