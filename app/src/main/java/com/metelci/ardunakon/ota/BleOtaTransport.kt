package com.metelci.ardunakon.ota

import android.content.Context
import android.util.Log
import com.metelci.ardunakon.bluetooth.AppBluetoothManager
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * BLE OTA Transport - Uses existing BLE connection for firmware transfer
 */
class BleOtaTransport(private val context: Context, private val bluetoothManager: AppBluetoothManager) : OtaTransport {

    companion object {
        const val TAG = "BleOtaTransport"

        // OTA GATT Service UUIDs
        val OTA_SERVICE_UUID: UUID = UUID.fromString("0000ff00-0000-1000-8000-00805f9b34fb")
        val OTA_CONTROL_UUID: UUID = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb")
        val OTA_DATA_UUID: UUID = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb")
        val OTA_STATUS_UUID: UUID = UUID.fromString("0000ff03-0000-1000-8000-00805f9b34fb")

        // Control commands
        const val CMD_START: Byte = 0x01
        const val CMD_DATA: Byte = 0x02
        const val CMD_END: Byte = 0x03
        const val CMD_ABORT: Byte = 0xFF.toByte()
    }

    private var isConnected = false

    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        // BLE should already be connected via BluetoothManager
        // Just verify OTA service is available
        try {
            // Send OTA START command through existing connection
            val startCmd = byteArrayOf(CMD_START)
            bluetoothManager.sendData(startCmd, force = true)
            delay(500) // Wait for Arduino to prepare
            isConnected = true
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "BLE OTA connect error: ${e.message}")
            return@withContext false
        }
    }

    override suspend fun sendChunk(data: ByteArray, offset: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            // Format: [CMD_DATA][offset_hi][offset_lo][data...]
            val packet = ByteArray(data.size + 3)
            packet[0] = CMD_DATA
            packet[1] = ((offset shr 8) and 0xFF).toByte()
            packet[2] = (offset and 0xFF).toByte()
            System.arraycopy(data, 0, packet, 3, data.size)

            bluetoothManager.sendData(packet, force = true)
            delay(50) // BLE needs time between packets

            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "BLE send chunk error: ${e.message}")
            return@withContext false
        }
    }

    override suspend fun complete(crc: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            // Format: [CMD_END][crc bytes]
            val packet = byteArrayOf(
                CMD_END,
                ((crc shr 24) and 0xFF).toByte(),
                ((crc shr 16) and 0xFF).toByte(),
                ((crc shr 8) and 0xFF).toByte(),
                (crc and 0xFF).toByte()
            )

            bluetoothManager.sendData(packet, force = true)
            delay(1000) // Wait for verification

            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "BLE complete error: ${e.message}")
            return@withContext false
        }
    }

    override suspend fun abort() {
        try {
            bluetoothManager.sendData(byteArrayOf(CMD_ABORT), force = true)
        } catch (_: Exception) {}
    }

    override fun disconnect() {
        isConnected = false
        // Don't disconnect BLE - it's managed by BluetoothManager
    }
}
