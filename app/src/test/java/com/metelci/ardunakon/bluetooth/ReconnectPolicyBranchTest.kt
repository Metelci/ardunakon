package com.metelci.ardunakon.bluetooth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReconnectPolicyBranchTest {

    private fun setPrivateLong(target: Any, fieldName: String, value: Long) {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.setLong(target, value)
    }

    @Test
    fun enable_disable_arm_toggle_shouldReconnect_and_reset_state() {
        val policy = ReconnectPolicy(CoroutineScope(Dispatchers.Unconfined))
        assertFalse(policy.shouldReconnect.value)

        policy.enable()
        assertTrue(policy.shouldReconnect.value)

        policy.disable()
        assertFalse(policy.shouldReconnect.value)

        policy.arm()
        assertFalse(policy.shouldReconnect.value)
    }

    @Test
    fun recordInbound_rtt_is_zero_without_heartbeat_and_updates_when_present() {
        val policy = ReconnectPolicy(CoroutineScope(Dispatchers.Unconfined))

        // No heartbeat sent yet
        val rtt1 = policy.recordInbound()
        assertEquals(0L, rtt1)

        // Heartbeat in the past -> RTT updates
        setPrivateLong(policy, "lastHeartbeatSentAt", System.currentTimeMillis() - 50)
        val rtt2 = policy.recordInbound()
        assertTrue(rtt2 >= 0L)

        // Heartbeat in the future -> branch where now < lastHeartbeatSentAt
        setPrivateLong(policy, "lastHeartbeatSentAt", System.currentTimeMillis() + 60_000)
        val rtt3 = policy.recordInbound()
        assertTrue(rtt3 >= 0L)
    }
}
