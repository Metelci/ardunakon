/*
 * Ardunakon Project 2: Remote Environmental Monitor
 * Hardware: Arduino UNO R4 WiFi + Modulino Thermo
 * 
 * Description:
 * Monitors Temperature and Humidity and sends it to the Ardunakon App.
 * - Periodic Updates (every 2 seconds) displayed in App Terminal
 * - LED Matrix SCROLLS the Temperature value using manual bitmap rendering.
 * 
 * Libraries:
 * - ArduinoBLE
 * - Modulino
 * - Arduino_LED_Matrix
 */

#include <ArduinoBLE.h>
#include <Modulino.h>
#include <Arduino_LED_Matrix.h>

ModulinoThermo thermo;
ArduinoLEDMatrix matrix;

// --------------------------------------------------------------------------
// GRAPHICS & FONT
// --------------------------------------------------------------------------
// Frame buffer 8x12
byte frame[8][12];

// Tiny 3x5 Font for Digits 0-9, space, C, dot
const byte tinyFont[][3] = {
  {0x1F, 0x11, 0x1F}, // 0
  {0x00, 0x1F, 0x00}, // 1
  {0x1D, 0x15, 0x17}, // 2
  {0x15, 0x15, 0x1F}, // 3
  {0x07, 0x04, 0x1F}, // 4
  {0x17, 0x15, 0x1D}, // 5
  {0x1F, 0x15, 0x1D}, // 6
  {0x01, 0x01, 0x1F}, // 7
  {0x1F, 0x15, 0x1F}, // 8
  {0x17, 0x15, 0x1F}, // 9
  {0x00, 0x00, 0x00}, // Space (Index 10)
  {0x0E, 0x11, 0x11}, // C (Index 11)
  {0x00, 0x10, 0x00}, // . (Index 12)
  {0x13, 0x08, 0x19}, // % (Index 13) - approx 3x5 check
};

// Map char to index
int getCharIndex(char c) {
  if (c >= '0' && c <= '9') return c - '0';
  if (c == ' ') return 10;
  if (c == 'C') return 11;
  if (c == '.') return 12;
  if (c == '%') return 13;
  return 10; // Default space
}

char scrollText[32] = "      "; // Buffer for scrolling text
int scrollPos = -12; // Start off screen right
unsigned long lastScrollTime = 0;
// --------------------------------------------------------------------------

#define SERVICE_UUID           "0000ffe0-0000-1000-8000-00805f9b34fb"
#define CHARACTERISTIC_UUID_TX "0000ffe1-0000-1000-8000-00805f9b34fb"
#define CHARACTERISTIC_UUID_RX "0000ffe2-0000-1000-8000-00805f9b34fb"

BLEService bleService(SERVICE_UUID);
BLECharacteristic txCharacteristic(CHARACTERISTIC_UUID_TX, BLERead | BLENotify, 20);
BLECharacteristic rxCharacteristic(CHARACTERISTIC_UUID_RX, BLEWrite | BLEWriteWithoutResponse, 20);

unsigned long lastTelemetry = 0;
float currentTemp = 0.0;

void setup() {
  Serial.begin(115200);
  
  Modulino.begin();
  
  if (!thermo.begin()) {
    Serial.println("Failed to start Modulino Thermo!");
  }
  
  matrix.begin();
  
  if (!BLE.begin()) while (1);

  BLE.setLocalName("Ardunakon Env");
  BLE.setAdvertisedService(bleService);
  bleService.addCharacteristic(txCharacteristic);
  bleService.addCharacteristic(rxCharacteristic);
  BLE.addService(bleService);
  BLE.advertise();
  
  Serial.println("Env Monitor Ready");
}

void loop() {
  BLEDevice central = BLE.central();
  
  // Scroll Logic
  if (millis() - lastScrollTime > 100) { // Scroll speed
    updateScroll();
    lastScrollTime = millis();
  }

  if (central) {
    if (central.connected()) {
      if (millis() - lastTelemetry > 2000) {
        sendEnvData();
        lastTelemetry = millis();
      }
    }
  }
}

void updateScroll() {
  // Clear frame
  for(int y=0; y<8; y++) for(int x=0; x<12; x++) frame[y][x] = 0;
  
  int len = strlen(scrollText);
  int totalWidth = len * 4; // 3 pixels char + 1 space
  
  // Draw visible characters
  for (int i=0; i<len; i++) {
    int charX = (i * 4) - scrollPos;
    
    // Optimization: Only draw if visible
    if (charX > -4 && charX < 12) {
      drawChar(scrollText[i], charX);
    }
  }
  
  matrix.renderBitmap(frame, 8, 12);
  
  scrollPos++;
  if (scrollPos > totalWidth) {
    scrollPos = -12; // Wrap around
  }
}

void drawChar(char c, int xStart) {
  int idx = getCharIndex(c);
  for (int col=0; col<3; col++) {
    int x = xStart + col;
    if (x < 0 || x >= 12) continue;
    
    byte colData = tinyFont[idx][col];
    for (int row=0; row<5; row++) {
      int y = row + 1; // Center vertically
      if (bitRead(colData, row)) {
         frame[y][x] = 1;
      }
    }
  }
}

void sendEnvData() {
  float hum = 0;
  float t = 0;
  
  // Modulino Thermo simplifies everything - just call get calls directly
  // It handles its own I2C logic internally
  float t_check = thermo.getTemperature();
  float h_check = thermo.getHumidity();
  
  // Basic validation (e.g. if it returns exactly 0 or -999 on error?)
  // Usually it just works or returns last known.
  if (t_check != 0 || h_check != 0) {
      currentTemp = t_check;
      hum = h_check;
  }
  
  // Update scroll text buffer
  // Format: "24.5 C   50 %   "
  snprintf(scrollText, sizeof(scrollText), "%.1f C   %.0f %%   ", currentTemp, hum);

  char buffer[32];
  snprintf(buffer, sizeof(buffer), "Temp: %.1fC  Hum: %.0f%%", currentTemp, hum);
  txCharacteristic.writeValue((uint8_t*)buffer, strlen(buffer));
  
  sendGraphData(currentTemp);
}

void sendGraphData(float val) {
  uint8_t packet[10];
  packet[0] = 0xAA; 
  packet[1] = 0x01; 
  packet[2] = 0x03; 
  
  int encoded = (int)(val * 10);
  if (encoded > 255) encoded = 255;
  packet[3] = (uint8_t)encoded;
  
  packet[4] = 0x01; 
  packet[5] = 0; packet[6]=0; packet[7]=0; 
  
  uint8_t xor_check = 0;
  for(int i=1; i<=7; i++) xor_check ^= packet[i];
  packet[8] = xor_check;
  packet[9] = 0x55; 
  
  txCharacteristic.writeValue(packet, 10);
}
