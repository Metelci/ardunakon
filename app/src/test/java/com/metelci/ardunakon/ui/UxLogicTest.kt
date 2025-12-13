package com.metelci.ardunakon.ui

import org.junit.Assert.*
import org.junit.Test

class UxLogicTest {

    // Replicating logic from SignalStrengthIcon.kt
    private fun getBarsForRssi(rssi: Int): Int = when {
        rssi == 0 -> 0
        rssi > -50 -> 4
        rssi > -65 -> 3
        rssi > -80 -> 2
        rssi > -95 -> 1
        else -> 0
    }

    @Test
    fun testRssiMapping() {
        assertEquals(4, getBarsForRssi(-40))
        assertEquals(4, getBarsForRssi(-49))
        assertEquals(3, getBarsForRssi(-50)) // Boundary check: > -50 is 4, so -50 is 3? Wait, logic says > -50. So -50 is 3.
        assertEquals(3, getBarsForRssi(-60))
        assertEquals(2, getBarsForRssi(-70))
        assertEquals(1, getBarsForRssi(-90))
        assertEquals(0, getBarsForRssi(-100))
        assertEquals(0, getBarsForRssi(0))
    }

    // Replicating logic from BluetoothManager.kt
    private fun decodeTerminalData(data: ByteArray): String {
        val decodedText = try {
            String(data, Charsets.UTF_8).trim()
        } catch (e: Exception) {
            null
        }

        if (decodedText != null &&
            decodedText.isNotEmpty() &&
            decodedText.all {
                it.isLetterOrDigit() ||
                    it.isWhitespace() ||
                    it in ".,;:!?-_()[]{}@#$%^&*+=<>/\\|~`'\"\n\r"
            }
        ) {
            return decodedText
        } else {
            return data.joinToString(" ") { "%02X".format(it) }
        }
    }

    @Test
    fun testTerminalDecoding() {
        // Text
        val textData = "Hello World".toByteArray()
        assertEquals("Hello World", decodeTerminalData(textData))

        // Text with symbols
        val symbolData = "Status: OK!".toByteArray()
        assertEquals("Status: OK!", decodeTerminalData(symbolData))

        // Binary
        val binaryData = byteArrayOf(0xAA.toByte(), 0x01, 0x02, 0x55.toByte())
        assertEquals("AA 01 02 55", decodeTerminalData(binaryData))

        // Mixed (should be treated as binary if it contains non-printable chars)
        val mixedData = byteArrayOf(0x48, 0x65, 0x00, 0x6C) // He\0l
        // 0x00 is not in the allowed list, so it should be hex
        assertEquals("48 65 00 6C", decodeTerminalData(mixedData))
    }
}
