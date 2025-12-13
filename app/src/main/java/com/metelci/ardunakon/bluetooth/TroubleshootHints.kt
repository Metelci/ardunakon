package com.metelci.ardunakon.bluetooth

/**
 * Troubleshoot Hints - Auto-suggestions for common errors
 * Displayed inline in debug window after error logs
 */
object TroubleshootHints {

    data class Hint(val pattern: String, val explanation: String, val solution: String)

    private val hints = listOf(
        // Connection errors
        Hint(
            "socket",
            "Bağlantı soketi oluşturulamadı",
            "Bluetooth eşleştirmesini kontrol edin ve cihazı yeniden başlatın"
        ),
        Hint("refused", "Bağlantı reddedildi", "Arduino'nun açık ve eşleştirilmiş olduğundan emin olun"),
        Hint("timeout", "Bağlantı zaman aşımına uğradı", "Arduino'ya yaklaşın veya cihazı yeniden başlatın"),
        Hint("discovery", "Cihaz bulunamadı", "Arduino'nun açık ve keşfedilebilir modda olduğunu kontrol edin"),

        // BLE errors
        Hint("GATT", "BLE GATT hatası", "Bluetooth'u kapatıp açın ve tekrar deneyin"),
        Hint(
            "characteristic",
            "BLE özelliği bulunamadı",
            "Arduino sketch'inin doğru UUID'leri kullandığını doğrulayın"
        ),
        Hint("MTU", "Paket boyutu hatası", "Cihaz BLE MTU boyutunu desteklemiyor olabilir"),

        // Heartbeat errors
        Hint("heartbeat", "Kalp atışı yanıtı yok", "Arduino'nun düzgün çalıştığından emin olun"),
        Hint("missed.*ack", "ACK paketleri eksik", "Sinyal kalitesini kontrol edin veya yaklaşın"),

        // OTA errors
        Hint("CRC", "Checksum uyuşmazlığı", "Firmware dosyasını yeniden indirin"),
        Hint("flash", "Flash yazma hatası", "Arduino'da yeterli depolama alanı olduğundan emin olun"),
        Hint("AP not found", "WiFi AP bulunamadı", "Arduino'nun ArdunakonOTA sketch'ini çalıştırdığından emin olun"),

        // Permission errors
        Hint("permission", "İzin reddedildi", "Uygulama ayarlarından Bluetooth iznini verin"),
        Hint(
            "BLUETOOTH",
            "Bluetooth izni eksik",
            "Ayarlar > Uygulamalar > Ardunakon > İzinler'den Bluetooth'u etkinleştirin"
        ),

        // Hardware errors
        Hint("adapter", "Bluetooth adaptörü yok", "Cihazınızda Bluetooth bulunmadığından emin olun"),
        Hint("disabled", "Bluetooth kapalı", "Bluetooth'u sistem ayarlarından açın")
    )

    /**
     * Get hint for error message (if any pattern matches)
     */
    fun getHintForError(errorMessage: String): Pair<String, String>? {
        val lowerMessage = errorMessage.lowercase()
        for (hint in hints) {
            if (lowerMessage.contains(hint.pattern.lowercase()) ||
                Regex(hint.pattern, RegexOption.IGNORE_CASE).containsMatchIn(errorMessage)
            ) {
                return Pair(hint.explanation, hint.solution)
            }
        }
        return null
    }

    /**
     * Format hint as display string
     */
    fun formatHint(explanation: String, solution: String): String = "→ $explanation. $solution"
}
