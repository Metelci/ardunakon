package com.metelci.ardunakon.util

import android.content.Context
import android.content.res.AssetManager
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AssetReaderTest {

    @Test
    fun readAssetFile_missing_asset_returns_helpful_error_message() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val fileName = "definitely-not-present.txt"

        val result = AssetReader.readAssetFile(context, fileName)

        assertTrue(result.contains("ERROR LOADING DOCUMENTATION"))
        assertTrue(result.contains("File: $fileName"))
        assertTrue(result.contains("WHAT YOU CAN DO:"))
    }

    @Test
    fun readAssetFile_when_assets_throw_runtime_exception_still_returns_error_message() {
        val assetManager = mockk<AssetManager>()
        every { assetManager.open(any()) } throws RuntimeException("boom")

        val context = mockk<Context>()
        every { context.assets } returns assetManager

        val result = AssetReader.readAssetFile(context, "x.txt")
        assertTrue(result.contains("ERROR LOADING DOCUMENTATION"))
        assertTrue(result.contains("Error: boom"))
    }

    @Test
    fun readAssetFile_when_assets_throw_io_exception_returns_error_message() {
        val assetManager = mockk<AssetManager>()
        every { assetManager.open(any()) } throws IOException("missing")

        val context = mockk<Context>()
        every { context.assets } returns assetManager

        val result = AssetReader.readAssetFile(context, "x.txt")
        assertTrue(result.contains("ERROR LOADING DOCUMENTATION"))
        assertTrue(result.contains("Error: missing"))
    }
}
