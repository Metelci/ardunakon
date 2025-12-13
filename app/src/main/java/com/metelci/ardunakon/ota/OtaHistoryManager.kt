package com.metelci.ardunakon.ota

import android.content.Context
import android.content.SharedPreferences
import java.io.File
import java.util.zip.CRC32
import org.json.JSONArray
import org.json.JSONObject

/**
 * OTA History Manager - Tracks recent firmware uploads
 */
class OtaHistoryManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "ota_history"
        private const val KEY_HISTORY = "recent_files"
        private const val MAX_HISTORY = 5
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class HistoryEntry(
        val fileName: String,
        val filePath: String,
        val fileSize: Long,
        val crc32: Long,
        val uploadDate: Long,
        val successful: Boolean
    ) {
        fun toJson(): JSONObject = JSONObject().apply {
            put("fileName", fileName)
            put("filePath", filePath)
            put("fileSize", fileSize)
            put("crc32", crc32)
            put("uploadDate", uploadDate)
            put("successful", successful)
        }

        companion object {
            fun fromJson(json: JSONObject): HistoryEntry = HistoryEntry(
                fileName = json.getString("fileName"),
                filePath = json.getString("filePath"),
                fileSize = json.getLong("fileSize"),
                crc32 = json.getLong("crc32"),
                uploadDate = json.getLong("uploadDate"),
                successful = json.getBoolean("successful")
            )
        }
    }

    /**
     * Get recent upload history
     */
    fun getHistory(): List<HistoryEntry> {
        val json = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { HistoryEntry.fromJson(array.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Add entry to history
     */
    fun addEntry(file: File, successful: Boolean) {
        val crc = calculateCrc32(file)
        val entry = HistoryEntry(
            fileName = file.name,
            filePath = file.absolutePath,
            fileSize = file.length(),
            crc32 = crc,
            uploadDate = System.currentTimeMillis(),
            successful = successful
        )

        val history = getHistory().toMutableList()

        // Remove duplicate if exists
        history.removeAll { it.filePath == file.absolutePath }

        // Add new entry at beginning
        history.add(0, entry)

        // Trim to max size
        while (history.size > MAX_HISTORY) {
            history.removeAt(history.size - 1)
        }

        // Save
        val array = JSONArray()
        history.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    /**
     * Calculate CRC32 of file
     */
    fun calculateCrc32(file: File): Long {
        val crc = CRC32()
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                crc.update(buffer, 0, bytesRead)
            }
        }
        return crc.value
    }

    /**
     * Validate file exists and matches CRC
     */
    fun validateFile(entry: HistoryEntry): Boolean {
        val file = File(entry.filePath)
        if (!file.exists()) return false
        if (file.length() != entry.fileSize) return false
        return calculateCrc32(file) == entry.crc32
    }

    /**
     * Clear history
     */
    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }
}
