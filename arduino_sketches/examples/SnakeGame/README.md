# Snake Game for Arduino R4 WiFi

A Pac-Man inspired snake game designed for Arduino UNO R4 WiFi with Modulino accessories.

## Features

- **Infinite wraparound screen** (Pac-Man mode)
- **Relative direction controls** (turn left/right instead of absolute directions)
- **Difficulty-based color changes** (green → yellow → red as snake grows)
- **Sound effects** via Modulino Buzzer
- **Dual control modes:**
  - Ardunakon Android App (BLE)
  - Physical Modulino Buttons

## Hardware Requirements

**Required:**
- Arduino UNO R4 WiFi
- Modulino Buzzer
- Modulino Pixels (8 RGB LEDs)

**Optional:**
- Modulino Buttons (for standalone play without app)

## Controls

### Via Ardunakon App (BLE)

**Start Game:**
- W button
- B button  
- Or move left joystick in any direction

**During Game:**
- Left Joystick X-axis: Turn left/right
- L button: Turn left
- R button: Turn right

### Via Modulino Buttons

**Start Game:**
- Press any button (left/center/right)

**During Game:**
- Left button (0): Turn left
- Right button (2): Turn right
- Center button (1): Start new game

## Game Mechanics

- Snake starts at length 3, moving right
- Food spawns randomly
- Eating food increases length by 1
- Game speeds up as score increases
- Collision with self ends game
- Screen wraps infinitely (no walls)

## LED Color Indicators

| Snake Length | Color  | Difficulty |
|--------------|--------|------------|
| 1-5          | Green  | Easy       |
| 6-11         | Yellow | Medium     |
| 12+          | Red    | Hard       |

## Installation

1. Open `SnakeGameR4.ino` in Arduino IDE
2. Install required libraries:
   - **ArduinoBLE** (for BLE connectivity)
   - **Modulino** (for Modulino accessories)
   - **ArdunakonProtocol** (from `arduino_sketches/libraries/`)
3. Select **Tools > Board > Arduino UNO R4 WiFi**
4. Upload to your R4 WiFi

## BLE Protocol

Uses Nordic UART Service (NUS) for compatibility:
- **Service UUID:** `6E400001-B5A3-F393-E0A9-E50E24DCCA9E`
- **RX (Phone → Arduino):** `6E400002-B5A3-F393-E0A9-E50E24DCCA9E`
- **TX (Arduino → Phone):** `6E400003-B5A3-F393-E0A9-E50E24DCCA9E`

Device advertises as: **SnakeR4**

## Troubleshooting

**BLE won't start:**
- Reset the board and try again
- Check that ArduinoBLE library is up to date
- The sketch retries BLE initialization 3 times automatically

**Modulino Buttons not detected:**
- Game will work fine with BLE-only control
- Check Modulino connections (Qwiic/I2C cable)
- Power cycle the Arduino

**Game too fast/slow:**
- Initial speed: 250ms per move
- Minimum speed: 80ms per move
- Speed decreases by 5ms with each food collected

## Notes

This is a demo/example sketch showing advanced Modulino integration with Ardunakon. For standard motor control applications, use the main `ArdunakonR4WiFi.ino` sketch located in `arduino_sketches/ArdunakonR4WiFi/`.

## Version History

- **v2.1** - Refactored to use ArdunakonProtocol library
- **v2.0** - Added BLE support for Ardunakon app
- **v1.0** - Initial Modulino standalone version
