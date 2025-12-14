# Ardunakon Onboarding UI Specifications

## Phase 1: Welcome Screen

### Layout Structure
```
┌─────────────────────────────────────┐
│                                     │
│        🚀 Ardunakon                 │
│                                     │
│    Arduino Controller App           │
│                                     │
│  ✨ Control with precision          │
│  📡 Bluetooth + WiFi support        │
│  🔧 Customizable profiles           │
│                                     │
│  We'll guide you through the        │
│  essentials in just 2 minutes       │
│                                     │
│  ┌─────────────┐ ┌─────────────┐   │
│  │  Get Started│ │   Skip Tour │   │
│  │   ▶️        │ │   ⏭️        │   │
│  └─────────────┘ └─────────────┘   │
│                                     │
│    📖 Access tutorial later in Help│
└─────────────────────────────────────┘
```

### Key Elements
- **App Icon**: Large, centered
- **Value Proposition**: 3 key benefits with icons
- **Time Estimate**: Manage expectations
- **Primary CTA**: "Get Started" (green button)
- **Secondary CTA**: "Skip Tour" with tooltip about accessing later
- **Footer**: Help menu integration hint

## Phase 2: Essential Interface Tour

### Step 1: Emergency Stop Highlight
```
┌─────────────────────────────────────┐
│ [BLE] [Status]        [STOP] [Menu] │
│                                     │
│         🔴 EMERGENCY STOP           │
│         This is the most            │
│         important button!           │
│         Tap to instantly stop       │
│         all motors.                 │
│                                     │
│              ┌─────┐                │
│              │  ▶️ │ Next          │
│              └─────┘                │
└─────────────────────────────────────┘
```

### Step 2: Connection Status
```
┌─────────────────────────────────────┐
│ [BLE] [📶Status]      [STOP] [Menu] │
│                                     │
│           📶 CONNECTION             │
│         Shows if you're             │
│         connected to your           │
│         Arduino device              │
│                                     │
│         🟢 Green = Connected        │
│         🟡 Yellow = Connecting      │
│         🔴 Red = Disconnected       │
│                                     │
│              ┌─────┐                │
│              │  ▶️ │ Next          │
│              └─────┘                │
└─────────────────────────────────────┘
```

### Step 3: Left Joystick
```
┌─────────────────────────────────────┐
│ [BLE] [Status]        [STOP] [Menu] │
│                                     │
│        [Debug Panel]                │
│                                     │
│              🎮 JOYSTICK            │
│         Controls movement           │
│         (left/right/forward)        │
│                                     │
│              ┌─────┐                │
│              │  ▶️ │ Next          │
│              └─────┘                │
└─────────────────────────────────────┘
```

### Step 4: Right Joystick
```
┌─────────────────────────────────────┐
│ [BLE] [Status]        [STOP] [Menu] │
│                                     │
│        [Debug Panel]                │
│                                     │
│                     [Servo] [🎮]    │
│                                     │
│              ⚙️ THROTTLE            │
│         Controls speed and          │
│         servo functions             │
│                                     │
│              ┌─────┐                │
│              │  ▶️ │ Next          │
│              └─────┘                │
└─────────────────────────────────────┘
```

### Step 5: Connection Mode Toggle
```
┌─────────────────────────────────────┐
│  [🔵BLE|WiFi📡] [Status] [STOP][Menu]│
│                                     │
│        🔄 CONNECTION MODE           │
│         Switch between              │
│         Bluetooth and WiFi          │
│                                     │
│         Bluetooth: Direct           │
│         connection (recommended)    │
│                                     │
│         WiFi: Network-based         │
│         (for advanced setups)       │
│                                     │
│              ┌─────┐                │
│              │  ▶️ │ Continue       │
│              └─────┘                │
└─────────────────────────────────────┘
```

## Phase 3: Connection Tutorial

### Step 1: Choose Your Arduino
```
┌─────────────────────────────────────┐
│           🔌 CONNECT                │
│                                     │
│     Choose your Arduino type:       │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  📱 Arduino UNO Q           │   │
│  │  Built-in Bluetooth 5.1     │   │
│  │  (Recommended for beginners)│   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  📶 Arduino UNO R4 WiFi     │   │
│  │  Built-in BLE + WiFi        │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  🔌 Classic Arduino +       │   │
│  │  HC-05/HC-06 module         │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  📡 Other/BLE Modules       │   │
│  │  HM-10, AT-09, etc.         │   │
│  └─────────────────────────────┘   │
│                                     │
│              ┌─────┐                │
│              │  ▶️ │ Next          │
│              └─────┘                │
└─────────────────────────────────────┘
```

### Step 2: Connection Mode Explanation
```
┌─────────────────────────────────────┐
│        📡 CONNECTION MODES          │
│                                     │
│  BLUETOOTH (Recommended)            │
│  • Direct phone-to-device connection│
│  • Lower latency, better control    │
│  • Works anywhere (no WiFi needed)  │
│                                     │
│  ─────────────────────────────────  │
│                                     │
│  WIFI (Advanced)                    │
│  • Network-based connection         │
│  • Longer range possible            │
│  • Requires same WiFi network       │
│                                     │
│  For your first connection,         │
│  we recommend Bluetooth.            │
│                                     │
│  ┌─────────┐ ┌─────────┐            │
│  │Bluetooth│ │  WiFi   │            │
│  │  📱    │ │  📡    │            │
│  └─────────┘ └─────────┘            │
└─────────────────────────────────────┘
```

### Step 3: Device Scanning
```
┌─────────────────────────────────────┐
│        🔍 SCANNING DEVICES          │
│                                     │
│  1. Tap "Dev 1" to scan             │
│  2. Select your Arduino from list   │
│  3. Wait for green "Connected"      │
│                                     │
│        [Dev 1 Status Card]          │
│        🟡 Scanning...               │
│        "Tap to select device"       │
│                                     │
│  📱 Looking for nearby devices...   │
│     Make sure your Arduino is       │
│     powered on and in pairing       │
│     mode.                           │
│                                     │
│              ┌─────┐                │
│              │  ▶️ │ Show me       │
│              └─────┘                │
└─────────────────────────────────────┘
```

### Step 4: Connection Success
```
┌─────────────────────────────────────┐
│        ✅ CONNECTED!                │
│                                     │
│        [Dev 1 Status Card]          │
│        🟢 Connected                 │
│        "ArdunakonQ"                 │
│        "Signal: Excellent"          │
│                                     │
│  🎉 Great job! You're now           │
│     connected and ready to          │
│     control your Arduino!           │
│                                     │
│  Try moving the left joystick       │
│  to test the connection.            │
│                                     │
│  ┌─────────┐ ┌─────────┐            │
│  │  Test   │ │ Continue│            │
│  │Joystick │ │   ▶️   │            │
│  └─────────┘ └─────────┘            │
└─────────────────────────────────────┘
```

## Phase 4: Advanced Features (Optional)

### Feature Discovery Cards
```
┌─────────────────────────────────────┐
│        🎯 OPTIONAL FEATURES         │
│                                     │
│  Interested in learning about       │
│  these advanced features?            │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  👤 Profile Management      │   │
│  │  Save your favorite settings│   │
│  │  and quickly switch between │   │
│  │  different projects         │   │
│  │                             │   │
│  │  [Explore] [Skip]           │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  📊 Debug Console           │   │
│  │  See connection logs and    │   │
│  │  troubleshoot issues        │   │
│  │                             │   │
│  │  [Explore] [Skip]           │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  📈 Telemetry & Monitoring  │   │
│  │  Battery voltage, signal    │   │
│  │  strength, and more!        │   │
│  │                             │   │
│  │  [Explore] [Skip]           │   │
│  └─────────────────────────────┘   │
│                                     │
│        [Skip All] [Finish Tutorial] │
└─────────────────────────────────────┘
```

## Phase 5: Completion
```
┌─────────────────────────────────────┐
│        🎉 YOU'RE READY!             │
│                                     │
│  ✅ Essential controls learned      │
│  ✅ Successfully connected          │
│  ✅ Ready to control your Arduino   │
│                                     │
│  🚀 What's Next?                    │
│                                     │
│  • Start with our example projects  │
│  • Explore settings and profiles    │
│  • Check Help for detailed guides   │
│                                     │
│  📖 Tutorial available anytime      │
│     in Help → "Take Tutorial"       │
│                                     │
│  ┌─────────────────────────────┐   │
│  │     Start Controlling!      │   │
│  │            ▶️               │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

## Component Specifications

### Overlay System
- **Background**: Semi-transparent black (70% opacity)
- **Highlight Animation**: Subtle pulse effect on highlighted elements
- **Arrow Pointers**: Animated lines pointing to target elements
- **Content Cards**: Rounded corners, subtle shadow, max-width 320dp

### Color Scheme
- **Primary**: App's green theme (#00FF00)
- **Warning**: Yellow (#FFD54F) 
- **Error**: Red (#FF5252)
- **Success**: Green (#4CAF50)
- **Background**: Semi-transparent black overlay

### Typography
- **Headers**: 18sp, Bold
- **Body**: 14sp, Regular
- **Captions**: 12sp, Regular
- **CTAs**: 16sp, Medium

### Animation Specifications
- **Fade In**: 300ms ease-out
- **Highlight Pulse**: 2s infinite, subtle scale (1.0 → 1.02 → 1.0)
- **Arrow Drawing**: 500ms stroke animation
- **Card Slide**: 400ms slide up from bottom

### Accessibility
- **Screen Reader**: Descriptive content for all tutorial steps
- **Keyboard Navigation**: Tab order through tutorial controls
- **Touch Targets**: Minimum 44dp for all interactive elements
- **High Contrast**: Support for system accessibility settings

### State Management
- **Current Step**: Track position in tutorial
- **Completed Steps**: Persist progress in SharedPreferences
- **Skip Status**: Remember if user skipped onboarding
- **Resume Position**: Allow tutorial resumption if interrupted

## Responsive Design

### Phone Portrait (Primary)
- Full-width content cards
- Centered alignment
- Adequate padding (16dp minimum)

### Phone Landscape
- Condensed layout where possible
- Horizontal arrangement of elements
- Maintain readability

### Tablet
- Larger content cards (max 400dp width)
- More generous spacing
- Consider split-screen layouts

## Integration Points

### Help Menu Integration
```
Help Menu:
├── 📖 User Guide
├── ❓ FAQ  
├── 🆘 Troubleshooting
├── 🎓 Take Tutorial  ← New
└── 📋 About

Tutorial Menu:
├── Welcome & Orientation
├── Interface Basics
├── Connecting Devices
├── Advanced Features
└── Hardware Setup Guide
```

### Settings Integration
```
Settings:
├── 🎓 Tutorial Options
│   ├── Reset Tutorial
│   ├── Auto-start Tutorial
│   └── Tutorial Access
└── Help & Support
```

### First-Run Detection Logic
```kotlin
class OnboardingManager {
    fun shouldShowOnboarding(): Boolean {
        return !preferences.isOnboardingCompleted() || 
               !preferences.isOnboardingVersionCurrent()
    }
    
    fun markOnboardingCompleted() {
        preferences.setOnboardingCompleted(true)
        preferences.setOnboardingVersion(currentVersion)
    }
}