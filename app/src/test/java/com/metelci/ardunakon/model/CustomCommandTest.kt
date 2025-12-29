package com.metelci.ardunakon.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomCommandTest {

    @Test
    fun isValidCommandId_accepts_only_custom_range() {
        assertTrue(CustomCommand.isValidCommandId(0x20))
        assertTrue(CustomCommand.isValidCommandId(0x3F))
        assertFalse(CustomCommand.isValidCommandId(0x1F))
        assertFalse(CustomCommand.isValidCommandId(0x40))
    }

    @Test
    fun isValidShortcut_rejects_reserved_keys_and_non_letters() {
        assertTrue(CustomCommand.isValidShortcut(null))
        assertTrue(CustomCommand.isValidShortcut('c'))
        assertFalse(CustomCommand.isValidShortcut('W'))
        assertFalse(CustomCommand.isValidShortcut('w'))
        assertFalse(CustomCommand.isValidShortcut('1'))
    }

    @Test
    fun isValid_requires_5_byte_payload_and_valid_commandId() {
        val ok = CustomCommand(
            id = "cmd-1",
            name = "OK",
            commandId = 0x20,
            payload = byteArrayOf(1, 2, 3, 4, 5)
        )
        assertTrue(ok.isValid())

        val badId = ok.copy(commandId = 0x10)
        assertFalse(badId.isValid())

        val badPayload = ok.copy(payload = byteArrayOf(1, 2, 3))
        assertFalse(badPayload.isValid())
    }

    @Test
    fun equals_and_hashCode_consider_payload_contents() {
        val a = CustomCommand(
            id = "cmd-1",
            name = "Same",
            commandId = 0x20,
            payload = byteArrayOf(1, 2, 3, 4, 5)
        )
        val b = a.copy(payload = byteArrayOf(1, 2, 3, 4, 6))
        assertFalse(a == b)
        assertFalse(a.hashCode() == b.hashCode())
    }
}
