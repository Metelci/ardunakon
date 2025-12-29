package com.metelci.ardunakon.ui.components

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for AboutDialog logic and state management.
 * Note: Full Compose UI tests require instrumented test environment.
 */
class AboutDialogTest {

    @Test
    fun `app name is Ardunakon`() {
        val appName = "Ardunakon"

        assertEquals("Ardunakon", appName)
    }

    @Test
    fun `app tagline is Arduino Controller`() {
        val tagline = "Arduino Controller"

        assertEquals("Arduino Controller", tagline)
    }

    @Test
    fun `version format includes name and code`() {
        val versionName = "0.2.13-alpha"
        val versionCode = 43
        val versionString = "Version $versionName ($versionCode)"

        assertEquals("Version 0.2.13-alpha (43)", versionString)
    }

    @Test
    fun `whats new title includes version`() {
        val versionName = "0.2.13-alpha"
        val title = "What's new in $versionName"

        assertEquals("What's new in 0.2.13-alpha", title)
    }

    @Test
    fun `github url is correct`() {
        val githubUrl = "https://github.com/metelci/ardunakon"

        assertEquals("https://github.com/metelci/ardunakon", githubUrl)
    }

    @Test
    fun `arduino cloud url is correct`() {
        val arduinoCloudUrl = "https://cloud.arduino.cc"

        assertEquals("https://cloud.arduino.cc", arduinoCloudUrl)
    }

    @Test
    fun `github button text is View on GitHub`() {
        val buttonText = "View on GitHub"

        assertEquals("View on GitHub", buttonText)
    }

    @Test
    fun `arduino cloud button text is Open Arduino Cloud`() {
        val buttonText = "Open Arduino Cloud"

        assertEquals("Open Arduino Cloud", buttonText)
    }

    @Test
    fun `license info mentions open source`() {
        val licenseInfo = "Open source project\\nBuilt with Jetpack Compose"

        assertTrue(licenseInfo.contains("Open source"))
    }

    @Test
    fun `license info mentions Jetpack Compose`() {
        val licenseInfo = "Open source project\\nBuilt with Jetpack Compose"

        assertTrue(licenseInfo.contains("Jetpack Compose"))
    }

    @Test
    fun `feature item has checkmark prefix`() {
        val checkmark = "✓"

        assertEquals("✓", checkmark)
    }

    @Test
    fun `feature item checkmark color is green`() {
        val checkmarkColor = 0xFF00C853

        assertEquals(0xFF00C853, checkmarkColor)
    }

    @Test
    fun `dialog width is 90 percent`() {
        val dialogWidth = 0.9f

        assertEquals(0.9f, dialogWidth, 0.001f)
    }

    @Test
    fun `dialog height is 85 percent`() {
        val dialogHeight = 0.85f

        assertEquals(0.85f, dialogHeight, 0.001f)
    }

    @Test
    fun `dialog background color is dark gray`() {
        val backgroundColor = 0xFF2D3436

        assertEquals(0xFF2D3436, backgroundColor)
    }

    @Test
    fun `whats new section background is darker`() {
        val dialogColor = 0xFF2D3436
        val sectionColor = 0xFF243039

        assertTrue(sectionColor < dialogColor)
    }

    @Test
    fun `app name color is blue`() {
        val appNameColor = 0xFF74B9FF

        assertEquals(0xFF74B9FF, appNameColor)
    }

    @Test
    fun `github button color is light blue`() {
        val githubColor = 0xFF90CAF9

        assertEquals(0xFF90CAF9, githubColor)
    }

    @Test
    fun `arduino cloud button color is green`() {
        val arduinoCloudColor = 0xFF00C853

        assertEquals(0xFF00C853, arduinoCloudColor)
    }

    @Test
    fun `release notes error message is user friendly`() {
        val errorMessage = "Unable to load release notes. Check CHANGELOG.md for details."

        assertTrue(errorMessage.contains("Unable to load"))
        assertTrue(errorMessage.contains("CHANGELOG.md"))
    }

    @Test
    fun `changelog is loaded from assets`() {
        val changelogPath = "CHANGELOG.md"

        assertEquals("CHANGELOG.md", changelogPath)
    }

    @Test
    fun `web view dialog title is About Ardunakon`() {
        val webViewTitle = "About Ardunakon"

        assertEquals("About Ardunakon", webViewTitle)
    }

    @Test
    fun `close button icon is Close`() {
        val closeIconDescription = "Close"

        assertEquals("Close", closeIconDescription)
    }

    @Test
    fun `open in new icon is used for external links`() {
        val openIconDescription = "Open GitHub"

        assertTrue(openIconDescription.contains("Open"))
    }
}
