package com.metelci.ardunakon.ui.screens.control

import android.content.Context
import android.content.res.Configuration
import android.view.View
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.metelci.ardunakon.bluetooth.ConnectionState
import com.metelci.ardunakon.ui.screenshot.ScreenshotTestUtils
import com.metelci.ardunakon.ui.testutils.NoOpIndication
import com.metelci.ardunakon.ui.testutils.createRegisteredComposeRule
import com.metelci.ardunakon.wifi.WifiConnectionState
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
@RunWith(RobolectricTestRunner::class)
class ControlHeaderBarScreenshotTest {

    @get:Rule
    val composeRule = createRegisteredComposeRule()

    @Test
    fun controlHeaderBar_portrait_matchesGolden() {
        val enabled = System.getProperty("enableScreenshotTests")?.equals("true", ignoreCase = true) == true ||
            System.getenv("ENABLE_SCREENSHOT_TESTS")?.equals("true", ignoreCase = true) == true
        assumeTrue("Screenshot tests disabled; set enableScreenshotTests=true to run.", enabled)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = View(context)
        val config = Configuration().apply {
            screenWidthDp = 360
            screenHeightDp = 640
        }

        composeRule.setContent {
            CompositionLocalProvider(
                LocalConfiguration provides config,
                LocalIndication provides NoOpIndication
            ) {
                MaterialTheme {
                    Box(
                        modifier = Modifier
                            .width(360.dp)
                            .height(72.dp)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        ControlHeaderBar(
                            connectionMode = ConnectionMode.BLUETOOTH,
                            bluetoothConnectionState = ConnectionState.DISCONNECTED,
                            wifiConnectionState = WifiConnectionState.DISCONNECTED,
                            rssiValue = -60,
                            wifiRssi = -70,
                            rttHistory = emptyList(),
                            wifiRttHistory = emptyList(),
                            isEStopActive = false,
                            autoReconnectEnabled = false,
                            onToggleAutoReconnect = {},
                            isWifiEncrypted = false,
                            buttonSize = 40.dp,
                            eStopSize = 72.dp,
                            onScanDevices = {},
                            onReconnectDevice = {},
                            onSwitchToWifi = {},
                            onSwitchToBluetooth = {},
                            onConfigureWifi = {},
                            onTelemetryGraph = {},
                            onToggleEStop = {},
                            onShowSettings = {},
                            onShowHelp = {},
                            onShowAbout = {},
                             onShowCrashLog = {},
                             onShowPerformanceStats = {},
                             onOpenArduinoCloud = {},
                             onDisconnect = {},
                             context = context,
                             view = view,
                             modifier = Modifier.fillMaxWidth()
                         )
                    }
                }
            }
        }

        composeRule.waitForIdle()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        ScreenshotTestUtils.assertMatchesGolden("control_header_bar_portrait", bitmap)
    }
}
