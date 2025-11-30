# Google Stitch Prompts for Ardunakon UI

Use these detailed prompts to generate the specific screens for the application.

## 1. Main Controller HUD (Landscape)
**Prompt:**
> A professional mobile app interface for an RC controller in landscape mode. The design style is "Cyber-Industrial" with a deep black background (`#000000`).
>
> **Visual Elements:**
> *   **Center:** Two large, minimalist virtual joysticks with glowing Electric Blue (`#00E5FF`) rings and a semi-transparent thumb puck.
> *   **Top Header:** A "Status Deck" containing two rectangular chips for "Dev 1" and "Dev 2". The active chip has a Neon Green (`#00FF9D`) glowing border and displays "12.4V" and a signal strength icon.
> *   **Bottom Control Deck:** Four hexagonal auxiliary buttons labeled A, B, C, D with a glass-like finish.
> *   **Emergency Control:** A prominent, caution-striped (Yellow/Black) "E-STOP" button located centrally at the top or bottom, designed to be distinct from other controls.
> *   **Typography:** Use a technical monospace font (like JetBrains Mono) for all data displays.

## 2. Bluetooth Scanner / Radar (Portrait)
**Prompt:**
> A high-tech Bluetooth scanning screen for a mobile app in portrait mode. Dark theme.
>
> **Visual Elements:**
> *   **Header:** A large, pulsing "Radar" animation at the top in Electric Blue.
> *   **List View:** A vertical list of detected devices. Each item is a card with a dark gray background (`#121212`) and rounded corners.
> *   **Card Content:** Device Name (e.g., "HC-06") in bold white text. A dynamic RSSI signal bar graph on the right side (Green for strong, Amber for weak). A "Connect" button that glows when tapped.
> *   **Footer:** A "Stop Scan" button with a red outline.

## 3. Profile & Settings Editor (Portrait)
**Prompt:**
> A clean, technical settings interface for configuring controller profiles. Dark mode.
>
> **Visual Elements:**
> *   **Profile Header:** A swipeable carousel of cards at the top representing different vehicles (e.g., "Drift Car", "Drone").
> *   **Sliders:** A "Sensitivity" slider (`10%` to `200%`) that looks like a precision instrument, with a live curve graph showing the response.
> *   **Toggles:** Chunky, segmented switches for selecting "Car Mode" (Bidirectional) vs "Drone Mode" (Unidirectional).
> *   **Input Fields:** Data entry fields for button commands styled like a code editor (dark background, monospace font).
