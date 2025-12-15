package com.metelci.ardunakon.util

import android.content.Context
import java.io.IOException

/**
 * Utility object for reading text files from the app's assets directory.
 * Provides graceful error handling with fallback messages for missing files.
 */
object AssetReader {

    /**
     * Reads a text file from the assets directory.
     *
     * @param context Android context for accessing assets
     * @param fileName Name of the file in assets (e.g., "docs/setup_guide.txt")
     * @return File contents as a string, or error message if file cannot be read
     */
    fun readAssetFile(context: Context, fileName: String): String = try {
        context.assets.open(fileName).bufferedReader().use { it.readText() }
    } catch (e: IOException) {
        buildErrorMessage(fileName, e)
    } catch (e: Exception) {
        buildErrorMessage(fileName, e)
    }

    /**
     * Builds a user-friendly error message when an asset file cannot be loaded.
     *
     * @param fileName Name of the file that failed to load
     * @param exception The exception that occurred
     * @return Formatted error message with troubleshooting information
     */
    private fun buildErrorMessage(fileName: String, exception: Exception): String = """
            ====================================================================================================
            ERROR LOADING DOCUMENTATION
            ====================================================================================================

            The requested documentation file could not be loaded.

            File: $fileName
            Error: ${exception.message}

            WHAT YOU CAN DO:

            1. TAP "VIEW FULL GUIDE ONLINE" BUTTON BELOW
               - Opens the complete documentation in the in-app viewer
               - Loads the live GitHub page for the latest information
               - Includes images, links, and full formatting

            2. CHECK YOUR APP VERSION
               - Make sure you're using the latest version of Ardunakon
               - Update from Play Store if needed

            3. REINSTALL THE APP
               - This error may be caused by a corrupted installation
               - Uninstall and reinstall from Play Store

            4. REPORT THE ISSUE
               - Visit: https://github.com/metelci/ardunakon/issues
               - Include: App version, Android version, device model

            ====================================================================================================

            QUICK LINKS:
            - Setup Guide: https://github.com/metelci/ardunakon/blob/main/arduino_sketches/SETUP_GUIDE.md
            - Compatibility: https://github.com/metelci/ardunakon/blob/main/BLUETOOTH_COMPATIBILITY.md

            ====================================================================================================
    """.trimIndent()
}
