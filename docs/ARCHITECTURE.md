# Ardunakon Architecture Documentation

This document provides component diagrams and data flow documentation for the Ardunakon Android application.

## System Overview

```mermaid
graph TB
    subgraph "Android Application"
        UI["UI Layer<br/>(Compose)"]
        VM["ViewModel Layer"]
        BM["BluetoothManager"]
        WM["WifiManager"]
        PM["PerformanceMonitor"]
        CB["CircuitBreaker"]
        RP["RetryPolicy"]
    end
    
    subgraph "External"
        BLE["BLE Device"]
        WIFI["WiFi Device"]
    end
    
    UI --> VM
    VM --> BM
    VM --> WM
    BM --> CB
    WM --> CB
    BM --> BLE
    WM --> WIFI
    BM --> PM
    WM --> PM
```

---

## Connection Flow

```mermaid
sequenceDiagram
    participant U as User
    participant UI as ControlScreen
    participant VM as ControlViewModel
    participant BM as BluetoothManager
    participant CB as CircuitBreaker
    participant D as Device

    U->>UI: Tap Connect
    UI->>VM: connectToDevice()
    VM->>BM: connectToDevice(model)
    BM->>CB: allowRequest()
    alt Circuit Open
        CB-->>BM: false
        BM-->>VM: Connection blocked
    else Circuit Closed/Half-Open
        CB-->>BM: true
        BM->>D: BLE/Classic connect
        D-->>BM: Connected
        BM->>CB: recordSuccess()
        BM-->>VM: State = CONNECTED
        VM-->>UI: Update UI
    end
```

---

## Error Recovery Flow

```mermaid
flowchart TD
    A[Operation Attempt] --> B{CircuitBreaker<br/>allowRequest?}
    B -->|No - Circuit Open| C[Fail Fast]
    B -->|Yes| D[Execute Operation]
    D --> E{Success?}
    E -->|Yes| F[recordSuccess]
    F --> G[Reset failures<br/>Close circuit]
    E -->|No| H[recordFailure]
    H --> I{Threshold<br/>exceeded?}
    I -->|No| J[Retry with backoff]
    J --> D
    I -->|Yes| K[Open Circuit]
    K --> L[Wait resetTimeout]
    L --> M[Half-Open State]
    M --> N[Allow one probe]
    N --> E
```

---

## Component Diagram

```mermaid
graph LR
    subgraph "UI Components"
        CS[ControlScreen]
        CHB[ControlHeaderBar]
        JC[JoystickControl]
        SC[ServoControl]
        PSD[PerformanceStatsDialog]
    end
    
    subgraph "Managers"
        ABM[AppBluetoothManager]
        WM[WifiManager]
        TM[TelemetryManager]
        PM[PerformanceMonitor]
    end
    
    subgraph "Utilities"
        CB[CircuitBreaker]
        RP[RetryPolicy]
        RM[RecoveryManager]
        EC[ErrorContext]
    end
    
    CS --> ABM
    CS --> WM
    ABM --> TM
    ABM --> CB
    WM --> CB
    ABM --> PM
    WM --> PM
    RM --> CB
    RM --> RP
```

---

## Data Flow Diagrams

### Telemetry Pipeline

```mermaid
flowchart LR
    D[Device] -->|Raw Bytes| BM[BluetoothManager]
    BM -->|onDataReceived| TM[TelemetryManager]
    TM -->|Parse| T[Telemetry]
    T -->|StateFlow| VM[ViewModel]
    VM -->|State| UI[Compose UI]
```

### Health Monitoring

```mermaid
flowchart TB
    subgraph "Metrics Sources"
        BM[BluetoothManager]
        WM[WifiManager]
        CH[CrashHandler]
    end
    
    subgraph "PerformanceMonitor"
        RC[recordCrash]
        RM[recordMetric]
        ST[stats StateFlow]
    end
    
    subgraph "UI"
        PSD[PerformanceStatsDialog]
    end
    
    BM --> RM
    WM --> RM
    CH --> RC
    RM --> ST
    RC --> ST
    ST --> PSD
```

---

## Algorithm Documentation

### Exponential Backoff (CircuitBreaker)

```kotlin
fun calculateBackoff(attempt: Int): Long {
    // Base formula: baseDelay * 2^attempt
    val exponentialDelay = baseDelayMs * 2.0.pow(attempt)
    
    // Cap at maximum delay
    val cappedDelay = min(exponentialDelay, maxDelayMs)
    
    // Add jitter (0-20% of delay) to prevent thundering herd
    val jitter = (cappedDelay * jitterFactor * random())
    
    return cappedDelay + jitter
}
```

| Attempt | Base Delay | Exponential | With 20% Jitter |
|---------|------------|-------------|-----------------|
| 0 | 1000ms | 1000ms | 1000-1200ms |
| 1 | 1000ms | 2000ms | 2000-2400ms |
| 2 | 1000ms | 4000ms | 4000-4800ms |
| 3 | 1000ms | 8000ms | 8000-9600ms |
| 4 | 1000ms | 16000ms | 16000-19200ms |

### Circuit Breaker State Machine

| Current State | Event | Next State | Action |
|---------------|-------|------------|--------|
| CLOSED | Failure count < threshold | CLOSED | Increment counter |
| CLOSED | Failure count ≥ threshold | OPEN | Block requests |
| OPEN | Request (timeout not elapsed) | OPEN | Fail fast |
| OPEN | Request (timeout elapsed) | HALF_OPEN | Allow probe |
| HALF_OPEN | Success | CLOSED | Reset counters |
| HALF_OPEN | Failure | OPEN | Restart timeout |

---

## Interface Contracts

### IBluetoothManager

Core connectivity contract with 21 members:
- **State Flows**: `connectionState`, `scannedDevices`, `rssiValue`, `health`, `telemetry`, `rttHistory`, `autoReconnectEnabled`, `isEmergencyStopActive`, `connectedDeviceInfo`
- **Operations**: `startScan()`, `stopScan()`, `connectToDevice()`, `disconnect()`, `sendData()`, `reconnectSavedDevice()`
- **Control**: `setEmergencyStop()`, `setAutoReconnectEnabled()`, `resetCircuitBreaker()`, `requestRssi()`

### IWifiManager

WiFi connectivity contract with 15 members:
- **State Flows**: `connectionState`, `scannedDevices`, `isScanning`, `rssi`, `rtt`, `telemetry`, `isEncrypted`
- **Operations**: `startDiscovery()`, `stopDiscovery()`, `connect()`, `disconnect()`, `sendData()`
- **Config**: `setAutoReconnectEnabled()`, `setRequireEncryption()`

### IPerformanceMonitor

Monitoring contract with 12 members:
- **Metrics**: `recordMetric()`, `recordStartupTime()`, `recordLatency()`, `recordMemoryUsage()`
- **Crashes**: `recordCrash()`, `getCrashHistory()`
- **Reporting**: `getStats()`, `generateDiagnosticReport()`
