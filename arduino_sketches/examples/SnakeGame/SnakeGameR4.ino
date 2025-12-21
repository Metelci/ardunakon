/*
 * Ardunakon + Modulino BLE Yılan Oyunu
 * Arduino R4 WiFi için tasarlandı
 * 
 * Kontrol Seçenekleri:
 * 1. Ardunakon Android Uygulaması (BLE)
 *    - Sol Joystick: Sol/Sağ = Yılanı döndür
 *    - W butonu: Oyunu başlat
 *    - L butonu: Sola dön
 *    - R butonu: Sağa dön
 *    - B butonu: Oyunu başlat (alternatif)
 * 
 * 2. Modulino Buttons (Fiziksel)
 *    - Sol Buton (0): Sola dön
 *    - Sağ Buton (2): Sağa dön
 *    - Orta Buton (1): Oyunu başlat
 * 
 * Özellikler:
 * - Sonsuz Ekran (Pac-Man modu)
 * - Göreceli yönlendirme
 * - Zorluk seviyesine göre renk değişimi
 * - Ses efektleri
 * 
 * BLE Protokolü: Nordic UART Service (NUS)
 * - Service:  6E400001-B5A3-F393-E0A9-E50E24DCCA9E
 * - TX Char:  6E400003-... (notify - device → phone)
 * - RX Char:  6E400002-... (write - phone → device) 
 * 
 * v2.1 - Refactored to use ArdunakonProtocol library
 */

#include "Arduino_LED_Matrix.h"
#include <Modulino.h>
#include <ArduinoBLE.h>
#include <ArdunakonProtocol.h>

// --- BLE Nordic UART Service UUIDs ---
#define SERVICE_UUID        "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"  // Phone → Arduino (write)
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"  // Arduino → Phone (notify)

// --- Fonksiyon Ön Tanımları ---
void gameLoop();
void checkCollisions();
void playEatSound();
void playStartMelody();
void playGameOverSound();
void updatePixels();
void setPixelsColor(int r, int g, int b);
void spawnFood();
void drawGame();
void endGame();
void resetGame();
void showIcon(String type);
void changeDirection(int turn);
bool isClicked(int btnIndex);
void handleBlePacket(const uint8_t* packetData);
void sendTelemetry();

// --- BLE Objects ---
BLEService uartService(SERVICE_UUID);
BLECharacteristic rxCharacteristic(CHARACTERISTIC_UUID_RX, BLEWrite | BLEWriteWithoutResponse, 20);
BLECharacteristic txCharacteristic(CHARACTERISTIC_UUID_TX, BLERead | BLENotify, 20);

// --- Protocol Objects ---
ArdunakonProtocol protocol;

// --- Modül Tanımları ---
ModulinoButtons buttons;
ModulinoPixels pixels;
ModulinoBuzzer buzzer;
ArduinoLEDMatrix matrix;

// --- BLE State ---
bool bleConnected = false;
uint8_t packetBuffer[ArdunakonProtocol::PACKET_SIZE];
uint8_t bufferIndex = 0;
unsigned long lastTelemetryTime = 0;
uint32_t packetsReceived = 0;

// --- Modulino State ---
bool buttonsAvailable = false;  // Buton modülü bağlı mı?

// --- Joystick Dead Zone ---
#define JOYSTICK_THRESHOLD 30  // Joystick hassasiyet eşiği

// --- Oyun Değişkenleri ---
const int width = 12;
const int height = 8;
int gameSpeed = 250; 
 
struct Point { int x; int y; };
Point snake[100];
int snakeLength = 3;

// Yön: 0: Yukarı, 1: Sağ, 2: Aşağı, 3: Sol
int currentDir = 1; 
int nextDir = 1; 

Point food;
bool gameOver = true;
unsigned long lastMoveTime = 0;

// Buton Durumları (Tekrarı önlemek için)
bool btnLastState[3] = {false, false, false};

// BLE Buton Durumları (Tekrarı önlemek için)
uint8_t lastAuxBits = 0;
int8_t lastJoystickX = 0;

// Ekran Tamponu
uint8_t frame[8][12];

void setup() {
  Serial.begin(9600);  // Standart baud rate
  delay(1000);  // USB stabilizasyonu için bekle
  Serial.println("Ardunakon Yilan Oyunu v2.1 - BLE + Modulino");
  
  // LED Matrix önce başlat (görsel geri bildirim için)
  matrix.begin();
  
  // Modülleri Başlat
  Modulino.begin();
  delay(200);  // Modulino stabilizasyonu
  
  // Buton modülünü kontrol et
  buttonsAvailable = buttons.begin();
  if (buttonsAvailable) {
    Serial.println("Modulino Buttons: BAGLI");
  } else {
    Serial.println("Modulino Buttons: YOK - Sadece BLE kontrol aktif");
  }
  
  pixels.begin();
  buzzer.begin();
  
  // Başlangıç animasyonu
  setPixelsColor(50, 50, 0);  // Sarı = başlatılıyor
  
  // BLE Başlat - R4 WiFi bazen birkaç deneme gerektirir
  Serial.println("BLE baslatiliyor...");
  
  bool bleOk = false;
  for (int attempt = 1; attempt <= 3; attempt++) {
    Serial.print("BLE deneme "); Serial.print(attempt); Serial.println("/3...");
    
    delay(500 * attempt);  // Her denemede daha uzun bekle
    
    if (BLE.begin()) {
      bleOk = true;
      Serial.println("BLE baslatildi!");
      break;
    }
    
    Serial.println("BLE basarisiz, tekrar deneniyor...");
  }
  
  if (!bleOk) {
    Serial.println("BLE baslatma HATASI! (3 deneme sonra)");
    Serial.println("Sadece Modulino butonlari ile oynanabilir.");
    setPixelsColor(100, 0, 0);  // Kırmızı = hata
  } else {
    // Cihaz adını ayarla
    BLE.setLocalName("SnakeR4");
    BLE.setDeviceName("SnakeR4");
    
    // Bağlanılabilir olarak ayarla
    BLE.setConnectable(true);
    
    // Service ve Characteristic ekle
    uartService.addCharacteristic(rxCharacteristic);
    uartService.addCharacteristic(txCharacteristic);
    BLE.addService(uartService);
    
    // Reklam ayarları - daha hızlı ve görünür
    BLE.setAdvertisedServiceUuid(uartService.uuid());
    BLE.setAdvertisedService(uartService);
    
    // Reklamı başlat
    if (BLE.advertise()) {
      Serial.println("BLE hazir - 'SnakeR4' olarak yayinda");
      Serial.println("Service UUID: " + String(SERVICE_UUID));
    } else {
      Serial.println("BLE advertise HATASI!");
      setPixelsColor(100, 50, 0);  // Turuncu = reklam hatası
    }
  }

  // Rastgelelik için çekirdek oluştur
  randomSeed(analogRead(0));
  
  // Bekleme Modu Işıkları (Mavi)
  setPixelsColor(0, 0, 50);
  showIcon("START"); 
  
  // Buton durumlarını senkronize et (sadece modül bağlıysa)
  if (buttonsAvailable) {
    Serial.println("Butonlar senkronize ediliyor...");
    delay(1000);  // 1 saniye bekle
    
    // Birkaç kez oku ve son durumu kaydet
    for (int j = 0; j < 10; j++) {
      buttons.update();
      delay(50);
    }
    for (int i = 0; i < 3; i++) {
      btnLastState[i] = buttons.isPressed(i);
      Serial.print("Buton "); Serial.print(i); 
      Serial.print(" durumu: "); Serial.println(btnLastState[i] ? "BASILI" : "SERBEST");
    }
  }
  
  Serial.println("Oyun Hazir - Bir butona basin veya Ardunakon ile baglanin!");
  buzzer.tone(1000, 100);  // Hazır sesi
}

void loop() {
  // BLE olaylarını işle (reklam, bağlantı vb.)
  BLE.poll();
  
  // --- BLE İşlemleri ---
  BLEDevice central = BLE.central();
  
  if (central) {
    if (!bleConnected) {
      bleConnected = true;
      Serial.println("BLE Baglandi: " + central.address());
      
      // Bağlantı bildirimi - yeşil flaş
      setPixelsColor(0, 100, 0);
      buzzer.tone(1500, 50);
      delay(100);
      setPixelsColor(0, 0, 50);
    }
    
    // Gelen veri var mı kontrol et
    if (rxCharacteristic.written()) {
      int len = rxCharacteristic.valueLength();
      const uint8_t* data = rxCharacteristic.value();
      
      // Paket parse et
      for (int i = 0; i < len; i++) {
        uint8_t byte = data[i];
        
        if (bufferIndex == 0 && byte != ArdunakonProtocol::START_BYTE) continue;
        
        packetBuffer[bufferIndex++] = byte;
        
        if (bufferIndex >= ArdunakonProtocol::PACKET_SIZE) {
          if (packetBuffer[9] == ArdunakonProtocol::END_BYTE) {
             // We can pass the buffer to handleBlePacket which now uses the library parser
             handleBlePacket(packetBuffer);
             packetsReceived++;
          }
          bufferIndex = 0;
        }
      }
    }
    
    // Telemetri gönder (20 saniye aralıkla)
    if (millis() - lastTelemetryTime > 20000) {
      sendTelemetry();
      lastTelemetryTime = millis();
    }
  } else {
    if (bleConnected) {
      bleConnected = false;
      Serial.println("BLE Baglanti kesildi");
      setPixelsColor(0, 0, 50);  // Mavi'ye dön
    }
  }

  // --- Modulino Buton Kontrolleri (sadece modül bağlıysa) ---
  if (buttonsAvailable) {
    buttons.update();

    // Eğer oyun bitmişse (Bekleme Modu)
    if (gameOver) {
      // Herhangi bir butona basılırsa oyunu başlat
      if (isClicked(0) || isClicked(1) || isClicked(2)) {
        resetGame();
      }
    } else {
      // --- OYUN SIRASINDA KONTROLLER ---
      
      // Sol Buton (0): Sola dön
      if (isClicked(0)) {
        changeDirection(-1);
        buzzer.tone(1000, 20);
      }
      
      // Sağ Buton (2): Sağa dön
      if (isClicked(2)) {
        changeDirection(1);
        buzzer.tone(1000, 20);
      }
    }
  }
  
  // Bekleme Animasyonu (her zaman çalışır)
  if (gameOver) {
    if (!bleConnected) {
      if(millis() % 2000 < 1000) setPixelsColor(0, 0, 20); 
      else setPixelsColor(0, 0, 50);
    } else {
      // BLE bağlıyken yeşil nefes
      if(millis() % 2000 < 1000) setPixelsColor(0, 20, 0); 
      else setPixelsColor(0, 50, 0);
    }
    return;
  }

  // Oyun Döngüsü
  if (millis() - lastMoveTime > gameSpeed) {
    gameLoop();
    lastMoveTime = millis();
  }
}

// --- BLE Paket İşleme ---
void handleBlePacket(const uint8_t* packetData) {
  // Use library to parse packet
  ArdunakonProtocol::ControlPacket packet = protocol.parsePacket(packetData);
  
  if (!packet.valid) return;
  
  switch (packet.cmd) {
    case ArdunakonProtocol::CMD_JOYSTICK: {
      // Library already mapped 0-200 to -100 to 100
      int8_t leftX = packet.leftX;
      int8_t leftY = packet.leftY;
      int8_t rightX = packet.rightX;
      int8_t rightY = packet.rightY;
      
      // Servo butonlarını algıla - merkez→basılı geçişini algıla
      
      static bool servoLActive = false;
      static bool servoRActive = false;
      static bool servoWActive = false;
      static bool servoBActive = false;
      static int8_t lastJoystickX_local = 0;
      
      // L butonu - merkez→negatif geçişi
      bool lPressed = rightX < -50;
      if (lPressed && !servoLActive) {
        servoLActive = true;
        Serial.println(">>> L: SOLA");
        if (!gameOver) { changeDirection(-1); buzzer.tone(1000, 20); }
      } else if (!lPressed && rightX > -30) {
        servoLActive = false;  // Merkeze döndü
      }
      
      // R butonu - merkez→pozitif geçişi
      bool rPressed = rightX > 50;
      if (rPressed && !servoRActive) {
        servoRActive = true;
        Serial.println(">>> R: SAGA");
        if (!gameOver) { changeDirection(1); buzzer.tone(1000, 20); }
      } else if (!rPressed && rightX < 30) {
        servoRActive = false;  // Merkeze döndü
      }
      
      // W butonu - pozitif yön
      bool wPressed = rightY > 50;
      if (wPressed && !servoWActive) {
        servoWActive = true;
        Serial.println(">>> W: OYUN BASLAT");
        if (gameOver) resetGame();
      } else if (!wPressed && rightY < 30) {
        servoWActive = false;
      }
      
      // B butonu - negatif yön
      bool bPressed = rightY < -50;
      if (bPressed && !servoBActive) {
        servoBActive = true;
        Serial.println(">>> B: OYUN BASLAT");
        if (gameOver) resetGame();
      } else if (!bPressed && rightY > -30) {
        servoBActive = false;
      }
      
      // Sol Joystick ile kontrol
      if (gameOver) {
        // Oyun başlat: herhangi bir yöne
        if (abs(leftX) > 30 || abs(leftY) > 30) {
          Serial.println(">>> JOY: OYUN BASLAT");
          resetGame();
        }
      } else {
        // Oyun sırasında: Sol/sağ dönüş (geçiş algılama)
        if (leftX < -30 && lastJoystickX_local >= -30) {
          Serial.println(">>> JOY: SOLA");
          changeDirection(-1);
          buzzer.tone(1000, 20);
        } else if (leftX > 30 && lastJoystickX_local <= 30) {
          Serial.println(">>> JOY: SAGA");
          changeDirection(1);
          buzzer.tone(1000, 20);
        }
      }
      
      lastJoystickX_local = leftX;
      break;
    }
    
    case ArdunakonProtocol::CMD_BUTTON: {
      // Library puts raw D1 into packet.leftX, D2 into leftY, D1 into auxBits for convenience
      uint8_t auxBits = packet.auxBits;
      
      // W veya B butonu - Oyunu başlat
      if ((auxBits & ArdunakonProtocol::AUX_W) && !(lastAuxBits & ArdunakonProtocol::AUX_W)) {
        Serial.println(">>> W BUTONU - OYUN BASLAT");
        if (gameOver) resetGame();
      }
      if ((auxBits & ArdunakonProtocol::AUX_B) && !(lastAuxBits & ArdunakonProtocol::AUX_B)) {
        Serial.println(">>> B BUTONU - OYUN BASLAT");
        if (gameOver) resetGame();
      }
      
      // L butonu - Sola dön
      if ((auxBits & ArdunakonProtocol::AUX_L) && !(lastAuxBits & ArdunakonProtocol::AUX_L)) {
        Serial.println(">>> L BUTONU - SOLA DON");
        if (!gameOver) {
          changeDirection(-1);
          buzzer.tone(1000, 20);
        }
      }
      
      // R butonu - Sağa dön
      if ((auxBits & ArdunakonProtocol::AUX_R) && !(lastAuxBits & ArdunakonProtocol::AUX_R)) {
        Serial.println(">>> R BUTONU - SAGA DON");
        if (!gameOver) {
          changeDirection(1);
          buzzer.tone(1000, 20);
        }
      }
      
      lastAuxBits = auxBits;
      break;
    }
    
    case ArdunakonProtocol::CMD_HEARTBEAT:
      // Heartbeat - bağlantı sağlığı
      break;
      
    case ArdunakonProtocol::CMD_ESTOP:
      Serial.println(">>> E-STOP");
      if (!gameOver) {
        endGame();
      }
      break;
  }
}

// --- Telemetri Gönder ---
void sendTelemetry() {
  uint8_t telemetry[10];
  
  // NOTE: This sketch uses a custom telemetry format that includes game state (snake length, etc.)
  // We will keep this format for this specific sketch as it provides game data, 
  // rather than switching to the generic formatTelemetry() which only does voltage/status.
  // We can still use constants from the library.
  
  telemetry[0] = ArdunakonProtocol::START_BYTE;
  telemetry[1] = 0x01;  // Device ID
  telemetry[2] = 0x10;  // CMD_TELEMETRY equivalent
  telemetry[3] = gameOver ? 0 : 1;  // Game state instead of Voltage
  telemetry[4] = (uint8_t)snakeLength;  // Score instead of Status
  telemetry[5] = (uint8_t)(gameSpeed / 10);  // Speed indicator
  telemetry[6] = bleConnected ? 1 : 0;
  telemetry[7] = 0;
  
  // Use library helper for checksum
  protocol.createChecksum(telemetry);
  telemetry[9] = ArdunakonProtocol::END_BYTE;
  
  txCharacteristic.writeValue(telemetry, 10);
}

// Buton Tıklama Kontrolü (State Change Detection)
bool isClicked(int btnIndex) {
  bool currentState = buttons.isPressed(btnIndex);
  
  if (currentState && !btnLastState[btnIndex]) {
    btnLastState[btnIndex] = true;
    return true;
  } else if (!currentState) {
    btnLastState[btnIndex] = false;
  }
  
  return false;
}

// Yön Değiştirme Mantığı (Göreceli)
void changeDirection(int turn) {
  nextDir = currentDir + turn;
  
  if (nextDir > 3) nextDir = 0;
  if (nextDir < 0) nextDir = 3;
}

// --- Ana Oyun Mantığı ---
void gameLoop() {
  currentDir = nextDir;
  
  // Kuyruğu takip ettir
  for (int i = snakeLength - 1; i > 0; i--) {
    snake[i] = snake[i - 1];
  }

  // Başı hareket ettir
  if (currentDir == 0) snake[0].y--;
  else if (currentDir == 1) snake[0].x++;
  else if (currentDir == 2) snake[0].y++;
  else if (currentDir == 3) snake[0].x--;

  // Sonsuz Ekran
  if (snake[0].x >= width) snake[0].x = 0;
  else if (snake[0].x < 0) snake[0].x = width - 1;
  
  if (snake[0].y >= height) snake[0].y = 0;
  else if (snake[0].y < 0) snake[0].y = height - 1;

  checkCollisions();
  
  if (!gameOver) {
    drawGame();
    updatePixels();
  }
}

void checkCollisions() {
  // Kendine Çarpma
  for (int i = 1; i < snakeLength; i++) {
    if (snake[0].x == snake[i].x && snake[0].y == snake[i].y) {
      endGame();
      return;
    }
  }

  // Yem Yeme
  if (snake[0].x == food.x && snake[0].y == food.y) {
    snakeLength++;
    playEatSound();
    
    if (gameSpeed > 80) gameSpeed -= 5;
    
    spawnFood();
  }
}

// --- Ses Efektleri ---
void playEatSound() {
  buzzer.tone(2000, 50); 
}

void playStartMelody() {
  int notes[] = {523, 659, 784, 1047};
  int duration = 100;
  
  for(int i=0; i<4; i++) {
    buzzer.tone(notes[i], duration);
    delay(duration + 20);
  }
}

void playGameOverSound() {
  int notes[] = {330, 294, 262, 196}; 
  int durations[] = {150, 150, 150, 400};

  for (int i = 0; i < 4; i++) {
    buzzer.tone(notes[i], durations[i]);
    delay(durations[i] * 1.3);
  }
}

// --- Görsel Efektler ---
void updatePixels() {
  int r, g, b;
  
  if (snakeLength < 6) {       
    r = 0; g = 50; b = 0;
  } else if (snakeLength < 12) { 
    r = 40; g = 30; b = 0;
  } else {                       
    r = 60; g = 0; b = 0;
  }

  for (int i = 0; i < 8; i++) {
    pixels.set(i, r, g, b);
  }
  pixels.show();
}

void setPixelsColor(int r, int g, int b) {
  for (int i = 0; i < 8; i++) {
    pixels.set(i, r, g, b);
  }
  pixels.show();
}

// --- Yardımcı Fonksiyonlar ---
void spawnFood() {
  bool valid = false;
  int attempts = 0;
  
  while (!valid && attempts < 100) {
    food.x = random(0, width);
    food.y = random(0, height);
    valid = true;
    for (int i = 0; i < snakeLength; i++) {
      if (snake[i].x == food.x && snake[i].y == food.y) {
        valid = false;
        break;
      }
    }
    attempts++;
  }
}

void drawGame() {
  memset(frame, 0, sizeof(frame));

  // Yılan
  for (int i = 0; i < snakeLength; i++) {
    frame[snake[i].y][snake[i].x] = 1;
  }
  // Yem
  frame[food.y][food.x] = 1;
  
  matrix.renderBitmap(frame, 8, 12);
}

void endGame() {
  gameOver = true;
  playGameOverSound();
  
  // Kırmızı Yanıp Sönme
  for(int k=0; k<3; k++){
    setPixelsColor(100, 0, 0);
    delay(300);
    setPixelsColor(0, 0, 0);
    delay(300);
  }
  showIcon("OVER");
  
  // Skor bildirimi gönder
  if (bleConnected) {
    sendTelemetry();
  }
  
  Serial.println("Game Over! Skor: " + String(snakeLength - 3));
}

void resetGame() {
  snakeLength = 3;
  snake[0] = {4, 4}; snake[1] = {3, 4}; snake[2] = {2, 4};
  currentDir = 1;
  nextDir = 1;
  gameSpeed = 250;
  gameOver = false;
  
  // BLE buton durumlarını sıfırla
  lastAuxBits = 0;
  lastJoystickX = 0;
  
  spawnFood();
  playStartMelody();
  
  Serial.println("Oyun basladi!");
}

void showIcon(String type) {
   uint8_t tempFrame[8][12] = {0};
   
   if (type == "OVER") {
      // X İkonu
      tempFrame[0][0]=1; tempFrame[0][11]=1;
      tempFrame[1][1]=1; tempFrame[1][10]=1;
      tempFrame[2][2]=1; tempFrame[2][9]=1;
      tempFrame[3][3]=1; tempFrame[3][8]=1;
      tempFrame[4][3]=1; tempFrame[4][8]=1;
      tempFrame[5][2]=1; tempFrame[5][9]=1;
      tempFrame[6][1]=1; tempFrame[6][10]=1;
      tempFrame[7][0]=1; tempFrame[7][11]=1;
   } else if (type == "START") {
      // Ok İkonu (Play)
      for(int y=1; y<=6; y++) tempFrame[y][4] = 1;
      for(int y=2; y<=5; y++) tempFrame[y][5] = 1;
      for(int y=3; y<=4; y++) tempFrame[y][6] = 1;
   }
   matrix.renderBitmap(tempFrame, 8, 12);
}