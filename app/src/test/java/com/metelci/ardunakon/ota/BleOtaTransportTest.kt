package com.metelci.ardunakon.ota

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.metelci.ardunakon.bluetooth.AppBluetoothManager
import io.mockk.Runs
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BleOtaTransportTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun connectSendsStartCommand() = runTest {
        val bleManager = mockk<AppBluetoothManager>()
        every { bleManager.sendData(any(), any()) } just Runs
        val transport = BleOtaTransport(context, bleManager)

        val result = transport.connect()

        assertTrue(result)
        coVerify { bleManager.sendData(byteArrayOf(BleOtaTransport.CMD_START), true) }
    }

    @Test
    fun sendChunkFormatsPacket() = runTest {
        val bleManager = mockk<AppBluetoothManager>()
        every { bleManager.sendData(any(), any()) } just Runs
        val transport = BleOtaTransport(context, bleManager)

        val result = transport.sendChunk(byteArrayOf(0x0A, 0x0B), offset = 0x0102)

        assertTrue(result)
        val expected = byteArrayOf(
            BleOtaTransport.CMD_DATA,
            0x01,
            0x02,
            0x0A,
            0x0B
        )
        coVerify { bleManager.sendData(expected, true) }
    }

    @Test
    fun completeFormatsCrcPacket() = runTest {
        val bleManager = mockk<AppBluetoothManager>()
        every { bleManager.sendData(any(), any()) } just Runs
        val transport = BleOtaTransport(context, bleManager)

        val result = transport.complete(0x01020304)

        assertTrue(result)
        val expected = byteArrayOf(
            BleOtaTransport.CMD_END,
            0x01,
            0x02,
            0x03,
            0x04
        )
        coVerify { bleManager.sendData(expected, true) }
    }

    @Test
    fun abortSendsAbortCommand() = runTest {
        val bleManager = mockk<AppBluetoothManager>()
        every { bleManager.sendData(any(), any()) } just Runs
        val transport = BleOtaTransport(context, bleManager)

        transport.abort()

        coVerify { bleManager.sendData(byteArrayOf(BleOtaTransport.CMD_ABORT), true) }
    }
}
