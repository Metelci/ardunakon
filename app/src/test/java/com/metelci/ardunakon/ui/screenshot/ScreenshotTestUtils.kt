package com.metelci.ardunakon.ui.screenshot

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.fail

object ScreenshotTestUtils {
    private const val GOLDEN_DIR = "app/src/test/resources/snapshots"

    fun assertMatchesGolden(name: String, actual: Bitmap) {
        val goldenFile = File(GOLDEN_DIR, "$name.png")
        if (shouldUpdateGoldens()) {
            goldenFile.parentFile?.mkdirs()
            writePng(goldenFile, actual)
            return
        }

        if (!goldenFile.exists()) {
            fail("Golden file missing: ${goldenFile.path}. Run with -DupdateGoldens=true to create it.")
        }

        val expected = BitmapFactory.decodeFile(goldenFile.absolutePath)
            ?: run {
                fail("Failed to decode golden image: ${goldenFile.path}")
                return
            }

        assertBitmapsEqual(expected, actual, goldenFile.path)
    }

    private fun shouldUpdateGoldens(): Boolean {
        val updateProp = System.getProperty("updateGoldens")?.equals("true", ignoreCase = true) == true
        val updateEnv = System.getenv("UPDATE_GOLDENS")?.equals("true", ignoreCase = true) == true
        return updateProp || updateEnv
    }

    private fun writePng(file: File, bitmap: Bitmap) {
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }

    private fun assertBitmapsEqual(expected: Bitmap, actual: Bitmap, goldenPath: String) {
        if (expected.width != actual.width || expected.height != actual.height) {
            fail(
                "Golden mismatch size for $goldenPath: expected ${expected.width}x${expected.height}, " +
                    "actual ${actual.width}x${actual.height}"
            )
        }

        val width = expected.width
        val height = expected.height
        val expectedPixels = IntArray(width * height)
        val actualPixels = IntArray(width * height)
        expected.getPixels(expectedPixels, 0, width, 0, 0, width, height)
        actual.getPixels(actualPixels, 0, width, 0, 0, width, height)

        for (i in expectedPixels.indices) {
            if (expectedPixels[i] != actualPixels[i]) {
                val x = i % width
                val y = i / width
                fail("Golden mismatch at ($x,$y) for $goldenPath")
            }
        }
    }
}
