package com.metelci.ardunakon.ota

import android.content.Context
import java.io.File
import java.util.zip.CRC32
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * OTA Update State
 */
enum class OtaState {
    IDLE,
    CONNECTING,
    TRANSFERRING,
    VERIFYING,
    COMPLETE,
    ERROR
}

/**
 * OTA Transfer Method
 */
enum class OtaMethod {
    BLE,
    WIFI
}

/**
 * OTA Progress Data
 */
data class OtaProgress(
    val state: OtaState = OtaState.IDLE,
    val method: OtaMethod = OtaMethod.WIFI,
    val bytesTransferred: Long = 0L,
    val totalBytes: Long = 0L,
    val errorMessage: String? = null
) {
    val percent: Int
        get() = if (totalBytes > 0) ((bytesTransferred * 100) / totalBytes).toInt() else 0
}

/**
 * Interface for OTA transport implementations
 */
interface OtaTransport {
    suspend fun connect(): Boolean
    suspend fun sendChunk(data: ByteArray, offset: Int): Boolean
    suspend fun complete(crc: Long): Boolean
    suspend fun abort()
    fun disconnect()
}

/**
 * OTA Manager - Handles firmware updates via BLE or WiFi
 */
class OtaManager(private val context: Context) {

    private val _progress = MutableStateFlow(OtaProgress())
    val progress: StateFlow<OtaProgress> = _progress.asStateFlow()

    private var transport: OtaTransport? = null
    private var isRunning = false

    companion object {
        const val CHUNK_SIZE = 512 // bytes per chunk
    }

    /**
     * Start OTA update with selected method
     */
    suspend fun startUpdate(
        file: File,
        method: OtaMethod,
        bleTransport: OtaTransport? = null,
        wifiTransport: OtaTransport? = null
    ): Boolean {
        if (isRunning) return false
        isRunning = true

        try {
            // Select transport based on method
            transport = when (method) {
                OtaMethod.BLE -> bleTransport ?: throw IllegalArgumentException("BLE transport not provided")
                OtaMethod.WIFI -> wifiTransport ?: throw IllegalArgumentException("WiFi transport not provided")
            }

            val fileBytes = file.readBytes()
            val totalSize = fileBytes.size.toLong()

            // Calculate CRC32
            val crc = CRC32()
            crc.update(fileBytes)
            val checksum = crc.value

            _progress.value = OtaProgress(
                state = OtaState.CONNECTING,
                method = method,
                totalBytes = totalSize
            )

            // Connect
            if (!transport!!.connect()) {
                _progress.value = _progress.value.copy(
                    state = OtaState.ERROR,
                    errorMessage = "Connection failed"
                )
                return false
            }

            _progress.value = _progress.value.copy(state = OtaState.TRANSFERRING)

            // Transfer chunks
            var offset = 0
            while (offset < fileBytes.size) {
                val chunkSize = minOf(CHUNK_SIZE, fileBytes.size - offset)
                val chunk = fileBytes.copyOfRange(offset, offset + chunkSize)

                if (!transport!!.sendChunk(chunk, offset)) {
                    _progress.value = _progress.value.copy(
                        state = OtaState.ERROR,
                        errorMessage = "Transfer failed at offset $offset"
                    )
                    return false
                }

                offset += chunkSize
                _progress.value = _progress.value.copy(bytesTransferred = offset.toLong())
            }

            // Verify
            _progress.value = _progress.value.copy(state = OtaState.VERIFYING)

            if (!transport!!.complete(checksum)) {
                _progress.value = _progress.value.copy(
                    state = OtaState.ERROR,
                    errorMessage = "Verification failed"
                )
                return false
            }

            _progress.value = _progress.value.copy(state = OtaState.COMPLETE)
            return true
        } catch (e: Exception) {
            _progress.value = _progress.value.copy(
                state = OtaState.ERROR,
                errorMessage = e.message ?: "Unknown error"
            )
            return false
        } finally {
            isRunning = false
            transport?.disconnect()
        }
    }

    /**
     * Abort current update
     */
    suspend fun abort() {
        transport?.abort()
        _progress.value = OtaProgress()
        isRunning = false
    }

    /**
     * Reset state
     */
    fun reset() {
        _progress.value = OtaProgress()
    }
}
