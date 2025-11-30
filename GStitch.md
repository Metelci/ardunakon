# GStitch Design Specification: Ardunakon

**Project**: Ardunakon - Professional Arduino Bluetooth Controller
**Design Goal**: Create a "Military-Grade" yet "Futuristic" interface that feels precise, responsive, and premium. The UI should inspire confidence and look great both indoors and outdoors (high contrast).

## 🎨 Aesthetic & Theme
*   **Style**: Cyber-Industrial / Modern Cockpit.
*   **Base Theme**: Deep Dark Mode (OLED Black `#000000` or Dark Gunmetal `#121212`).
*   **Accents**:
    *   **Primary**: Electric Blue (`#00E5FF`) for active elements and data.
    *   **Success**: Neon Green (`#00FF9D`) for connection status and good signal.
    *   **Danger**: Alert Red (`#FF3D00`) for E-STOP and critical errors.
    *   **Warning**: Amber (`#FFC107`) for weak signal or bonding prompts.
*   **Typography**: Technical, monospace for data (e.g., `JetBrains Mono`, `Roboto Mono`), clean sans-serif for labels (e.g., `Inter`, `Rajdhani`).

## 📱 Key Screens & Components

### 1. The HUD (Main Controller)
This is the primary screen. It must look like a pilot's heads-up display.

*   **Header Bar (Status Deck)**:
    *   **Device Slots**: Two distinct "cards" or "chips" for `Dev 1` and `Dev 2`.
        *   *State Disconnected*: Dimmed, "Connect" text.
        *   *State Connected*: Glowing Green border, Device Name, RSSI Icon (Signal bars), Battery Voltage (e.g., "12.4V").
    *   **Telemetry Strip**: Small scrolling text line showing the last received raw data command.
*   **Control Area (Center)**:
    *   **Dual Virtual Joysticks**:
        *   Left Stick: Omni-directional (Movement).
        *   Right Stick: Vertical constraint option (Throttle).
        *   *Visuals*: Minimalist rings. The "thumb" puck should glow when touched. Dynamic trail effect on movement.
*   **Command Deck (Bottom/Sides)**:
    *   **AUX Buttons**: 4 customizable buttons (`A`, `B`, `C`, `D`).
        *   *Style*: Hexagonal or rounded squares.
        *   *Interaction*: Press-down animation.
    *   **E-STOP (Emergency Stop)**:
        *   *Style*: Large, distinct, perhaps striped yellow/black border or pure red.
        *   *Placement*: Easy to hit but hard to hit accidentally (e.g., center bottom or top right).
*   **Drawer/Overlay**:
    *   **Debug Console**: A slide-up panel from the bottom showing the raw log. Matrix-style green text on black.

### 2. Connection Manager (Scanner)
A high-tech radar or list view for finding devices.

*   **Scan Button**: Large, pulsing "Radar" button.
*   **Device List Items**:
    *   **Icon**: Bluetooth Classic vs BLE symbol.
    *   **Name**: Bold text (e.g., **HC-06**, **ArdunakonQ**).
    *   **Signal**: Live RSSI bar graph.
    *   **Tags**: "Bonded", "New", "Weak Signal".
*   **Empty State**: A cool radar scanning animation.

### 3. Profile & Settings
Configuration without the clutter.

*   **Profile Cards**: Swipeable cards for different vehicles (e.g., "My Drone", "Drift Car").
    *   Each card shows a summary: "Car Mode, 100% Sensitivity".
*   **Edit Screen**:
    *   **Sensitivity Sliders**: 10% to 200% with live preview graph.
    *   **Mode Toggles**: Segmented switches for "Car Mode" vs "Drone Mode".
    *   **Button Mapping**: Simple input fields for button labels and command strings.

## 💡 Interaction Principles
*   **Haptics**: Every button press and joystick limit hit should have haptic feedback.
*   **Responsiveness**: Zero lag. Animations should be snappy (150ms-300ms).
*   **Feedback**: Immediate visual confirmation for every action (glows, ripples).

## 🛠️ Technical Constraints
*   **Orientation**: Landscape fixed for Controller. Portrait allowed for Settings/Scanning.
*   **Safe Areas**: Keep controls away from screen edges to prevent accidental gestures.
