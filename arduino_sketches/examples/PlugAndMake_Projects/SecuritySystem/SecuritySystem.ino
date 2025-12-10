/*
 * Ardunakon Project 3: Smart Security Alarm
 * Hardware: Arduino UNO R4 WiFi + Modulino Distance + Modulino Buzzer
 * 
 * Description:
 * - Monitors distance sensor.
 * - If system is ARMED (via App) and distance < threshold: Trigger Alarm.
 * - LED Matrix displays DISTANCE in CM (e.g. "45").
 * - "99+" for > 99cm.
 * 
 * Libraries:
 * - ArduinoBLE
 * - Modulino (Install "Modulino" library)
 * - Arduino_LED_Matrix
 */

#include <ArduinoBLE.h>
#include <Modulino.h>
#include <Arduino_LED_Matrix.h>

ModulinoDistance distanceSensor;
ModulinoBuzzer buzzer;
ArduinoLEDMatrix matrix;

// --------------------------------------------------------------------------
// ICONS & FONTS
// --------------------------------------------------------------------------
const uint32_t lock_icon[] = { 0x183c2424, 0x3c3c3c3c, 0x00000000 };    
const uint32_t unlock_icon[] = { 0x18242400, 0x3c3c3c3c, 0x00000000 };  
const uint32_t alarm_icon[] = { 0xa5429942, 0xa5000000, 0x00000000 };   

// Tiny 3x5 Font for Digits 0-9
// Each byte represents a column (3 columns per digit)
// 0 is top bit, 4 is bottom valid bit (5 pixels high)
const byte tinyFont[10][3] = {
  {0x1F, 0x11, 0x1F}, // 0
  {0x00, 0x1F, 0x00}, // 1 (just a line)
  {0x1D, 0x15, 0x17}, // 2
  {0x15, 0x15, 0x1F}, // 3
  {0x07, 0x04, 0x1F}, // 4
  {0x17, 0x15, 0x1D}, // 5
  {0x1F, 0x15, 0x1D}, // 6
  {0x01, 0x01, 0x1F}, // 7
  {0x1F, 0x15, 0x1F}, // 8
  {0x17, 0x15, 0x1F}  // 9
};

const byte plusSign[3] = {0x04, 0x0E, 0x04}; // +

// Frame buffer 8x12
byte frame[8][12];

// --------------------------------------------------------------------------

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

bool armed = false;
bool alarmTriggered = false;

void setup() {
  Serial.begin(115200);
  
  Modulino.begin();
  
  if (!distanceSensor.begin()) Serial.println("Distance Sensor Failed");
  if (!buzzer.begin()) Serial.println("Buzzer Failed");
  
  matrix.begin();
  
  if (!BLE.begin()) while (1);

  BLE.setLocalName("Ardunakon Guard");
  BLE.setAdvertisedService(bleService);
  bleService.addCharacteristic(txCharacteristic);
  bleService.addCharacteristic(rxCharacteristic);
  BLE.addService(bleService);
  BLE.advertise();
  
  Serial.println("Security System Ready");
}

void loop() {
  BLEDevice central = BLE.central();
  
  if (central) {
    while (central.connected()) {
      if (rxCharacteristic.written()) {
        handleIncoming(rxCharacteristic.value(), rxCharacteristic.valueLength());
      }
      runSecurityLogic();
      delay(50); 
    }
  } else {
    runSecurityLogic();
    delay(50);
  }
}

void runSecurityLogic() {
  int dist_mm = -1;
  if (distanceSensor.available()) {
     dist_mm = distanceSensor.get();
  }

  if (dist_mm == -1) dist_mm = 9999; 
  int dist_cm = dist_mm / 10;

  // DISPLAY LOGIC
  bool showDistance = true;

  if (alarmTriggered && armed) {
    // BLINKING LOGIC
    // Toggle every 250ms
    if ((millis() / 250) % 2 == 0) {
      showDistance = true;
    } else {
      showDistance = false;
    }
  }

  if (showDistance) {
    clearFrame();
    if (dist_cm > 99) {
      drawDigit(9, 1);
      drawDigit(9, 5);
      drawPlus(9);
    } else {
      int tens = dist_cm / 10;
      int ones = dist_cm % 10;
      if (tens > 0) drawDigit(tens, 2);
      drawDigit(ones, 7);
    }
    matrix.renderBitmap(frame, 8, 12);
  } else {
    // Blank screen for blink effect
    clearFrame();
    matrix.renderBitmap(frame, 8, 12);
  }

  // ALARM LOGIC
  if (armed) {
    // Threshold 30cm (300mm)
    if (dist_mm > 0 && dist_mm < 300) { 
      if (!alarmTriggered) {
         alarmTriggered = true;
         sendTextLog("ALARM: Intruder Detected!");
      }
      // Sound Alarm: Frequency 2000Hz, Duration 200ms
      buzzer.tone(2000, 200); 
    } else {
      if (alarmTriggered) {
        alarmTriggered = false;
        // buzzer.noTone(); // Not strictly needed if duration expires, but good practice
      }
    }
  } else {
    alarmTriggered = false;
    // buzzer.noTone(); should not be needed if not armed and not playing
  }
}

// --------------------------------------------------------------------------
// GRAPHICS HELPERS
// --------------------------------------------------------------------------
void clearFrame() {
  for (int y=0; y<8; y++) for(int x=0; x<12; x++) frame[y][x] = 0;
}

void drawDigit(int num, int xStart) {
  if (num < 0 || num > 9) return;
  for (int col=0; col<3; col++) { // 3 columns wide
    if (xStart + col >= 12) break;
    byte colData = tinyFont[num][col];
    for (int row=0; row<5; row++) { // 5 rows high
      // Center vertically (start at Y=1 or Y=2)
      int y = row + 1; 
      if (bitRead(colData, row)) {
         frame[y][xStart+col] = 1;
      }
    }
  }
}

void drawPlus(int xStart) {
  for (int col=0; col<3; col++) {
    if (xStart + col >= 12) break;
    byte colData = plusSign[col];
    for (int row=0; row<5; row++) {
      int y = row + 1;
      if (bitRead(colData, row)) frame[y][xStart+col] = 1;
    }
  }
}
// --------------------------------------------------------------------------

void handleIncoming(const uint8_t* data, int length) {
  for (int i=0; i<length; i++) processByte(data[i]);
}

void processByte(uint8_t b) {
  if (bufferIndex == 0 && b != START_BYTE) return;
  packetBuffer[bufferIndex++] = b;
  if (bufferIndex >= PACKET_SIZE) {
    if (packetBuffer[9] == END_BYTE) handlePacket();
    bufferIndex = 0;
  }
}

void handlePacket() {
  if (packetBuffer[2] == 0x01) { 
    int rawY = packetBuffer[6];
    
    if (rawY > 150) { // 'W' Pressed -> Disarm
       if (armed) {
         armed = false;
         sendTextLog("System DISARMED");
         alarmTriggered = false;
         buzzer.tone(500, 200); 
       }
    }
    else if (rawY < 50) { // 'B' (Back) Pressed -> Arm
       if (!armed) {
         armed = true;
         sendTextLog("System ARMED");
         buzzer.tone(2000, 200); 
         delay(200);
         buzzer.tone(2000, 200);
       }
    }
  }
}

void sendTextLog(const char* msg) {
  BLEDevice central = BLE.central();
  if (central && central.connected()) {
     txCharacteristic.writeValue((uint8_t*)msg, strlen(msg));
  }
}
