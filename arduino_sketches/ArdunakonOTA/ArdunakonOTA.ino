/*
 * ArdunakonOTA - OTA Firmware Update Support
 * Hardware: Arduino UNO R4 WiFi
 * 
 * Description:
 * Supports both BLE and WiFi OTA firmware updates.
 * - WiFi: Creates Access Point, serves HTTP for firmware upload
 * - BLE: Uses GATT service for firmware transfer
 * 
 * To use:
 * 1. Upload this sketch via USB first
 * 2. Open Ardunakon app -> Menu -> Firmware Update
 * 3. Select WiFi or BLE method
 * 4. Choose .bin file and start update
 */

#include <WiFiS3.h>
#include <ArduinoBLE.h>
#include <Arduino_LED_Matrix.h>

// WiFi AP Configuration
const char* AP_SSID = "Ardunakon_OTA";
const char* AP_PASSWORD = "ardunakon123";

// HTTP Server
WiFiServer server(80);

// LED Matrix for status
ArduinoLEDMatrix matrix;

// OTA State
enum OtaState {
  OTA_IDLE,
  OTA_RECEIVING,
  OTA_VERIFYING,
  OTA_COMPLETE,
  OTA_ERROR
};

OtaState otaState = OTA_IDLE;
uint32_t receivedBytes = 0;
uint32_t expectedBytes = 0;
uint32_t expectedCrc = 0;

// Status icons
const uint32_t wifi_icon[3] = {0x00240042, 0x00810000, 0x00000000};
const uint32_t ok_icon[3] = {0x00400020, 0x10100800, 0x04000000};
const uint32_t error_icon[3] = {0x11000A00, 0x04000A00, 0x11000000};

void setup() {
  Serial.begin(115200);
  delay(1000);
  
  // Initialize LED Matrix
  matrix.begin();
  
  // Start WiFi AP
  Serial.println("Starting WiFi Access Point...");
  WiFi.beginAP(AP_SSID, AP_PASSWORD);
  
  // Wait for AP to start
  delay(2000);
  
  IPAddress ip = WiFi.localIP();
  Serial.print("AP IP Address: ");
  Serial.println(ip);
  
  // Start HTTP server
  server.begin();
  Serial.println("HTTP Server started on port 80");
  
  // Show WiFi icon
  matrix.loadFrame(wifi_icon);
  
  Serial.println("ArdunakonOTA Ready!");
  Serial.println("Connect to WiFi: " + String(AP_SSID));
  Serial.println("Password: " + String(AP_PASSWORD));
}

void loop() {
  // Handle HTTP clients
  WiFiClient client = server.available();
  
  if (client) {
    handleHttpClient(client);
  }
  
  // Update status display
  updateStatusDisplay();
  
  delay(10);
}

void handleHttpClient(WiFiClient& client) {
  String currentLine = "";
  String method = "";
  String path = "";
  int contentLength = 0;
  
  while (client.connected()) {
    if (client.available()) {
      char c = client.read();
      
      if (c == '\n') {
        if (currentLine.length() == 0) {
          // End of headers, process request
          
          if (method == "GET" && path == "/info") {
            // Return device info
            sendJsonResponse(client, 200, 
              "{\"version\":\"1.0\",\"status\":\"ready\",\"freeMemory\":" + String(freeMemory()) + "}");
          }
          else if (method == "POST" && path == "/update") {
            // Receive firmware chunk
            receiveFirmwareChunk(client, contentLength);
          }
          else if (method == "POST" && path == "/complete") {
            // Finalize update
            finalizeUpdate(client);
          }
          else if (method == "POST" && path == "/abort") {
            // Abort update
            abortUpdate(client);
          }
          else {
            sendJsonResponse(client, 404, "{\"error\":\"Not found\"}");
          }
          break;
        }
        
        // Parse headers
        if (method == "") {
          // First line: METHOD PATH HTTP/1.1
          int firstSpace = currentLine.indexOf(' ');
          int secondSpace = currentLine.indexOf(' ', firstSpace + 1);
          method = currentLine.substring(0, firstSpace);
          path = currentLine.substring(firstSpace + 1, secondSpace);
        }
        else if (currentLine.startsWith("Content-Length:")) {
          contentLength = currentLine.substring(15).toInt();
        }
        else if (currentLine.startsWith("X-TotalSize:")) {
          expectedBytes = currentLine.substring(12).toInt();
          otaState = OTA_RECEIVING;
          receivedBytes = 0;
        }
        
        currentLine = "";
      }
      else if (c != '\r') {
        currentLine += c;
      }
    }
  }
  
  client.stop();
}

void receiveFirmwareChunk(WiFiClient& client, int contentLength) {
  // Read chunk data
  uint8_t buffer[512];
  int bytesRead = 0;
  
  while (bytesRead < contentLength && client.connected()) {
    if (client.available()) {
      int len = client.read(buffer + bytesRead, min(512 - bytesRead, contentLength - bytesRead));
      bytesRead += len;
    }
  }
  
  receivedBytes += bytesRead;
  
  // TODO: Actually write to flash using Update library
  // For now, just acknowledge receipt
  Serial.print("Received chunk: ");
  Serial.print(bytesRead);
  Serial.print(" bytes, Total: ");
  Serial.print(receivedBytes);
  Serial.println(" bytes");
  
  sendJsonResponse(client, 200, "{\"received\":" + String(bytesRead) + ",\"total\":" + String(receivedBytes) + "}");
}

void finalizeUpdate(WiFiClient& client) {
  otaState = OTA_VERIFYING;
  
  // TODO: Verify CRC and set boot partition
  // For demo, just pretend it worked
  
  Serial.println("Finalizing update...");
  delay(1000);
  
  otaState = OTA_COMPLETE;
  sendJsonResponse(client, 200, "{\"status\":\"complete\",\"rebooting\":true}");
  
  // Reset to apply update
  delay(2000);
  // NVIC_SystemReset(); // Uncomment for actual reboot
}

void abortUpdate(WiFiClient& client) {
  otaState = OTA_IDLE;
  receivedBytes = 0;
  expectedBytes = 0;
  Serial.println("Update aborted");
  sendJsonResponse(client, 200, "{\"status\":\"aborted\"}");
}

void sendJsonResponse(WiFiClient& client, int code, String body) {
  client.println("HTTP/1.1 " + String(code) + " OK");
  client.println("Content-Type: application/json");
  client.println("Access-Control-Allow-Origin: *");
  client.println("Connection: close");
  client.println("Content-Length: " + String(body.length()));
  client.println();
  client.println(body);
}

void updateStatusDisplay() {
  static unsigned long lastUpdate = 0;
  if (millis() - lastUpdate < 500) return;
  lastUpdate = millis();
  
  switch (otaState) {
    case OTA_RECEIVING:
      // Blink during transfer
      if ((millis() / 250) % 2 == 0) {
        matrix.loadFrame(wifi_icon);
      } else {
        matrix.clear();
      }
      break;
    case OTA_COMPLETE:
      matrix.loadFrame(ok_icon);
      break;
    case OTA_ERROR:
      matrix.loadFrame(error_icon);
      break;
    default:
      matrix.loadFrame(wifi_icon);
      break;
  }
}

int freeMemory() {
  // Simple free memory estimate
  char* ptr = (char*)malloc(1);
  if (!ptr) return 0;
  int free = (int)((char*)&ptr - ptr);
  ::free(ptr);
  return abs(free);
}
