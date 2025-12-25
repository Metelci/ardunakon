package com.metelci.ardunakon.bluetooth

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for GattStatus utility.
 *
 * Tests GATT status code classification as transient/permanent,
 * error descriptions, and edge cases.
 */
class GattStatusTest {

    @Test
    fun gattSuccess_isNotTransient() {
        assertFalse(GattStatus.isTransientError(GattStatus.GATT_SUCCESS))
    }

    @Test
    fun gattSuccess_isNotPermanent() {
        assertFalse(GattStatus.isPermanentError(GattStatus.GATT_SUCCESS))
    }

    @Test
    fun gattConnectionTimeout_isTransient() {
        assertTrue(GattStatus.isTransientError(GattStatus.GATT_CONNECTION_TIMEOUT))
    }

    @Test
    fun gattInternalError_isTransient() {
        assertTrue(GattStatus.isTransientError(GattStatus.GATT_INTERNAL_ERROR))
    }

    @Test
    fun gattLinkLoss_isTransient() {
        assertTrue(GattStatus.isTransientError(GattStatus.GATT_LINK_LOSS))
    }

    @Test
    fun gattError62_isTransient() {
        // HM-10 clone common error
        assertTrue(GattStatus.isTransientError(62))
    }

    @Test
    fun gattDeviceNotFound_isPermanent() {
        assertTrue(GattStatus.isPermanentError(GattStatus.GATT_DEVICE_NOT_FOUND))
    }

    @Test
    fun gattInsufficientAuth_isNeitherTransientNorPermanent() {
        assertFalse(GattStatus.isTransientError(GattStatus.GATT_INSUFFICIENT_AUTHENTICATION))
        assertFalse(GattStatus.isPermanentError(GattStatus.GATT_INSUFFICIENT_AUTHENTICATION))
    }

    @Test
    fun gattInsufficientEncryption_isNeitherTransientNorPermanent() {
        assertFalse(GattStatus.isTransientError(GattStatus.GATT_INSUFFICIENT_ENCRYPTION))
        assertFalse(GattStatus.isPermanentError(GattStatus.GATT_INSUFFICIENT_ENCRYPTION))
    }

    @Test
    fun unknownErrorCode_isNotTransient() {
        assertFalse(GattStatus.isTransientError(999))
    }

    @Test
    fun unknownErrorCode_isNotPermanent() {
        assertFalse(GattStatus.isPermanentError(999))
    }

    @Test
    fun getErrorDescription_success() {
        assertEquals("Success", GattStatus.getErrorDescription(GattStatus.GATT_SUCCESS))
    }

    @Test
    fun getErrorDescription_connectionTimeout() {
        val description = GattStatus.getErrorDescription(GattStatus.GATT_CONNECTION_TIMEOUT)
        assertTrue(description.contains("Connection Timeout"))
        assertTrue(description.contains("(8)"))
    }

    @Test
    fun getErrorDescription_insufficientAuth() {
        val description = GattStatus.getErrorDescription(GattStatus.GATT_INSUFFICIENT_AUTHENTICATION)
        assertTrue(description.contains("Authentication"))
        assertTrue(description.contains("(5)"))
        assertTrue(description.contains("Pairing required"))
    }

    @Test
    fun getErrorDescription_insufficientEncryption() {
        val description = GattStatus.getErrorDescription(GattStatus.GATT_INSUFFICIENT_ENCRYPTION)
        assertTrue(description.contains("Encryption"))
        assertTrue(description.contains("(15)"))
    }

    @Test
    fun getErrorDescription_internalError() {
        val description = GattStatus.getErrorDescription(GattStatus.GATT_INTERNAL_ERROR)
        assertTrue(description.contains("Internal Error"))
        assertTrue(description.contains("(129)"))
        assertTrue(description.contains("stack"))
    }

    @Test
    fun getErrorDescription_deviceNotFound() {
        val description = GattStatus.getErrorDescription(GattStatus.GATT_DEVICE_NOT_FOUND)
        assertTrue(description.contains("Device Not Found"))
        assertTrue(description.contains("(133)"))
        assertTrue(description.contains("Out of range"))
    }

    @Test
    fun getErrorDescription_linkLoss() {
        val description = GattStatus.getErrorDescription(GattStatus.GATT_LINK_LOSS)
        assertTrue(description.contains("Link Layer Failure"))
        assertTrue(description.contains("(147)"))
    }

    @Test
    fun getErrorDescription_error62() {
        val description = GattStatus.getErrorDescription(62)
        assertTrue(description.contains("(62)"))
        assertTrue(description.contains("HM-10"))
    }

    @Test
    fun getErrorDescription_unknownError() {
        val description = GattStatus.getErrorDescription(999)
        assertTrue(description.contains("Unknown"))
        assertTrue(description.contains("(999)"))
    }

    @Test
    fun gattStatusConstants_haveCorrectValues() {
        assertEquals(0, GattStatus.GATT_SUCCESS)
        assertEquals(8, GattStatus.GATT_CONNECTION_TIMEOUT)
        assertEquals(5, GattStatus.GATT_INSUFFICIENT_AUTHENTICATION)
        assertEquals(15, GattStatus.GATT_INSUFFICIENT_ENCRYPTION)
        assertEquals(129, GattStatus.GATT_INTERNAL_ERROR)
        assertEquals(133, GattStatus.GATT_DEVICE_NOT_FOUND)
        assertEquals(147, GattStatus.GATT_LINK_LOSS)
    }

    @Test
    fun transientErrors_canBeRetried() {
        val transientErrors = listOf(
            GattStatus.GATT_CONNECTION_TIMEOUT,
            GattStatus.GATT_INTERNAL_ERROR,
            GattStatus.GATT_LINK_LOSS,
            62
        )

        transientErrors.forEach { errorCode ->
            assertTrue(
                "Error $errorCode should be transient",
                GattStatus.isTransientError(errorCode)
            )
        }
    }

    @Test
    fun permanentErrors_shouldNotBeRetried() {
        val permanentErrors = listOf(
            GattStatus.GATT_DEVICE_NOT_FOUND
        )

        permanentErrors.forEach { errorCode ->
            assertTrue(
                "Error $errorCode should be permanent",
                GattStatus.isPermanentError(errorCode)
            )
        }
    }

    @Test
    fun allKnownErrors_haveDescriptions() {
        val knownErrors = listOf(
            GattStatus.GATT_SUCCESS,
            GattStatus.GATT_CONNECTION_TIMEOUT,
            GattStatus.GATT_INSUFFICIENT_AUTHENTICATION,
            GattStatus.GATT_INSUFFICIENT_ENCRYPTION,
            GattStatus.GATT_INTERNAL_ERROR,
            GattStatus.GATT_DEVICE_NOT_FOUND,
            GattStatus.GATT_LINK_LOSS,
            62
        )

        knownErrors.forEach { errorCode ->
            val description = GattStatus.getErrorDescription(errorCode)
            assertFalse(
                "Error $errorCode has empty description",
                description.isEmpty()
            )
        }
    }

    @Test
    fun negativeErrorCode_handledGracefully() {
        val description = GattStatus.getErrorDescription(-1)
        assertTrue(description.contains("Unknown"))
        assertTrue(description.contains("(-1)"))
    }

    @Test
    fun largeErrorCode_handledGracefully() {
        val description = GattStatus.getErrorDescription(10000)
        assertTrue(description.contains("Unknown"))
        assertTrue(description.contains("(10000)"))
    }
}
