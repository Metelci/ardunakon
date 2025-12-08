/*
 * Ardunakon Project 1: Wireless Mood Light
 * Hardware: Arduino UNO R4 WiFi + Modulino Pixels
 * 
 * Description:
 * Control the color and brightness of the Modulino Pixels strip.
 * - Left Joystick Y: Brightness
 * - Left Joystick X: Hue (Color Cycle)
 * - Button 'W': Toggle Animation Mode
 * 
 * Libraries required:
 * - ArduinoBLE
 * - Modulino (Install "Modulino" from Library Manager)
 * - Arduino_LED_Matrix
 */

#include <ArduinoBLE.h>
#include <Modulino.h>
#include <Arduino_LED_Matrix.h>

// -----------------------------------------------------------------------
// OBJECTS
// -----------------------------------------------------------------------
ModulinoPixels pixels;
ArduinoLEDMatrix matrix;

// -----------------------------------------------------------------------
// BITMAPS for LED Matrix
// -----------------------------------------------------------------------
const uint32_t heart[] = {
  0x3184a444,
  0x42081100,
  0xa0040000
};

const uint32_t bluetooth[] = {
  0x142a542a,
  0x2a140000,
  0x00000000
};

// BLE UUIDs
#define SERVICE_UUID           "0000ffe0-0000-1000-8000-00805f9b34fb"
#define CHARACTERISTIC_UUID_TX "0000ffe1-0000-1000-8000-00805f9b34fb" 
#define CHARACTERISTIC_UUID_RX "0000ffe2-0000-1000-8000-00805f9b34fb" 

BLEService bleService(SERVICE_UUID);
BLECharacteristic txCharacteristic(CHARACTERISTIC_UUID_TX, BLERead | BLENotify, 20);
BLECharacteristic rxCharacteristic(CHARACTERISTIC_UUID_RX, BLEWrite | BLEWriteWithoutResponse, 20);

// Protocol
#define START_BYTE 0xAA
#define END_BYTE   0x55
#define PACKET_SIZE 10

uint8_t packetBuffer[PACKET_SIZE];
uint8_t bufferIndex = 0;

// State
int brightness = 50; 
int hue = 0;         
bool animationMode = false;
unsigned long lastUpdate = 0;

void setup() {
  Serial.begin(115200);
  
  // Initialize Modulino (Master init)
  Modulino.begin();
  
  // Initialize Pixels
  if (!pixels.begin()) {
    Serial.println("Failed to start Modulino Pixels!");
  }
  
  matrix.begin();
  matrix.loadFrame(heart);

  if (!BLE.begin()) {
    Serial.println("starting BLE failed!");
    while (1);
  }

  BLE.setLocalName("Ardunakon Mood");
  BLE.setAdvertisedService(bleService);
  bleService.addCharacteristic(txCharacteristic);
  bleService.addCharacteristic(rxCharacteristic);
  BLE.addService(bleService);
  BLE.advertise();

  Serial.println("Ardunakon Mood Light Ready");
}

void loop() {
  BLEDevice central = BLE.central();

  if (central) {
    Serial.print("Connected to central: ");
    Serial.println(central.address());
    matrix.loadFrame(bluetooth);

    while (central.connected()) {
      if (rxCharacteristic.written()) {
        handleIncoming(rxCharacteristic.value(), rxCharacteristic.valueLength());
      }
      
      if (millis() - lastUpdate > 50) {
        updateLights();
        lastUpdate = millis();
      }
    }
    Serial.println("Disconnected");
    matrix.loadFrame(heart);
  }
}

void handleIncoming(const uint8_t* data, int length) {
  for (int i = 0; i < length; i++) {
    processByte(data[i]);
  }
}

void processByte(uint8_t b) {
  if (bufferIndex == 0 && b != START_BYTE) return;
  packetBuffer[bufferIndex++] = b;
  
  if (bufferIndex >= PACKET_SIZE) {
    if (packetBuffer[9] == END_BYTE) {
      handlePacket();
    }
    bufferIndex = 0;
  }
}

void handlePacket() {
  uint8_t xor_check = 0;
  for (int i = 1; i <= 7; i++) xor_check ^= packetBuffer[i];
  if (xor_check != packetBuffer[8]) return;

  uint8_t cmd = packetBuffer[2];
  
  if (cmd == 0x01) {
    int rawY = packetBuffer[4]; // 0..200
    brightness = map(rawY, 0, 200, 0, 100); 

    int rawX = packetBuffer[3];
    hue = map(rawX, 0, 200, 0, 255); 
    
    int servoY = map(packetBuffer[6], 0, 200, -100, 100);
    
    static bool wPressed = false;
    if (servoY > 50) { 
      if (!wPressed) {
        animationMode = !animationMode;
        sendTextLog(animationMode ? "Mode: Animation" : "Mode: Manual");
        wPressed = true;
      }
    } else {
      wPressed = false;
    }
  }
}

void updateLights() {
  if (animationMode) {
    static int rainbowOffset = 0;
    rainbowOffset++;
    for (int i = 0; i < 8; i++) { 
       int pixelHue = (rainbowOffset + (i * 10)) % 255;
       setPixelColorHSV(i, pixelHue, 255, brightness * 2.5); 
    }
    pixels.show();
  } else {
    for (int i = 0; i < 8; i++) {
       setPixelColorHSV(i, hue, 255, brightness * 2.5);
    }
    pixels.show();
  }
}

void setPixelColorHSV(int pixel, int h, int s, int v) {
  unsigned char region, remainder, p, q, t;
  unsigned char r, g, b;

  if (s == 0) {
    r = v; g = v; b = v;
  } else {
    region = h / 43;
    remainder = (h - (region * 43)) * 6; 
    p = (v * (255 - s)) >> 8;
    q = (v * (255 - ((s * remainder) >> 8))) >> 8;
    t = (v * (255 - ((s * (255 - remainder)) >> 8))) >> 8;

    switch (region) {
      case 0: r = v; g = t; b = p; break;
      case 1: r = q; g = v; b = p; break;
      case 2: r = p; g = v; b = t; break;
      case 3: r = p; g = q; b = v; break;
      case 4: r = t; g = p; b = v; break;
      default: r = v; g = p; b = q; break;
    }
  }
  pixels.set(pixel, r, g, b); 
}

void sendTextLog(const char* msg) {
  txCharacteristic.writeValue((uint8_t*)msg, strlen(msg));
}
