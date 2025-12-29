package com.metelci.ardunakon.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for OnboardingStep and related enums.
 */
class OnboardingStepTest {

    // ==================== OnboardingStep Sealed Class ====================

    @Test
    fun `Welcome is singleton`() {
        val step1 = OnboardingStep.Welcome
        val step2 = OnboardingStep.Welcome

        assertSame(step1, step2)
    }

    @Test
    fun `Completion is singleton`() {
        val step1 = OnboardingStep.Completion
        val step2 = OnboardingStep.Completion

        assertSame(step1, step2)
    }

    @Test
    fun `Finished is singleton`() {
        val step1 = OnboardingStep.Finished
        val step2 = OnboardingStep.Finished

        assertSame(step1, step2)
    }

    @Test
    fun `InterfaceTour with same element are equal`() {
        val step1 = OnboardingStep.InterfaceTour(InterfaceElement.ESTOP)
        val step2 = OnboardingStep.InterfaceTour(InterfaceElement.ESTOP)

        assertEquals(step1, step2)
    }

    @Test
    fun `InterfaceTour with different elements are not equal`() {
        val step1 = OnboardingStep.InterfaceTour(InterfaceElement.ESTOP)
        val step2 = OnboardingStep.InterfaceTour(InterfaceElement.LEFT_JOYSTICK)

        assertNotEquals(step1, step2)
    }

    @Test
    fun `ConnectionTutorial with same step are equal`() {
        val step1 = OnboardingStep.ConnectionTutorial(ConnectionTutorialStep.CHOOSE_ARDUINO)
        val step2 = OnboardingStep.ConnectionTutorial(ConnectionTutorialStep.CHOOSE_ARDUINO)

        assertEquals(step1, step2)
    }

    @Test
    fun `ConnectionTutorial with different steps are not equal`() {
        val step1 = OnboardingStep.ConnectionTutorial(ConnectionTutorialStep.CHOOSE_ARDUINO)
        val step2 = OnboardingStep.ConnectionTutorial(ConnectionTutorialStep.SETUP_FINAL)

        assertNotEquals(step1, step2)
    }

    // ==================== InterfaceElement Enum ====================

    @Test
    fun `InterfaceElement has 5 values`() {
        assertEquals(5, InterfaceElement.entries.size)
    }

    @Test
    fun `InterfaceElement ESTOP has correct displayName`() {
        assertEquals("Emergency Stop", InterfaceElement.ESTOP.displayName)
    }

    @Test
    fun `InterfaceElement CONNECTION_STATUS has correct displayName`() {
        assertEquals("Connection Status", InterfaceElement.CONNECTION_STATUS.displayName)
    }

    @Test
    fun `InterfaceElement LEFT_JOYSTICK has correct displayName`() {
        assertEquals("Joystick", InterfaceElement.LEFT_JOYSTICK.displayName)
    }

    @Test
    fun `InterfaceElement SERVO_CONTROLS has correct displayName`() {
        assertEquals("Servo Buttons", InterfaceElement.SERVO_CONTROLS.displayName)
    }

    @Test
    fun `InterfaceElement CONNECTION_MODE has correct displayName`() {
        assertEquals("Connection Mode", InterfaceElement.CONNECTION_MODE.displayName)
    }

    @Test
    fun `InterfaceElement descriptions are not empty`() {
        for (element in InterfaceElement.entries) {
            assertTrue("${element.name} description should not be empty", element.description.isNotEmpty())
        }
    }

    // ==================== InterfaceElement tourOrder ====================

    @Test
    fun `tourOrder has 5 elements`() {
        assertEquals(5, InterfaceElement.tourOrder().size)
    }

    @Test
    fun `tourOrder starts with ESTOP`() {
        assertEquals(InterfaceElement.ESTOP, InterfaceElement.tourOrder().first())
    }

    @Test
    fun `tourOrder ends with CONNECTION_MODE`() {
        assertEquals(InterfaceElement.CONNECTION_MODE, InterfaceElement.tourOrder().last())
    }

    @Test
    fun `tourOrder contains all elements`() {
        val order = InterfaceElement.tourOrder()
        for (element in InterfaceElement.entries) {
            assertTrue("${element.name} should be in tourOrder", order.contains(element))
        }
    }

    @Test
    fun `tourOrder is consistent`() {
        val order1 = InterfaceElement.tourOrder()
        val order2 = InterfaceElement.tourOrder()

        assertEquals(order1, order2)
    }

    // ==================== ConnectionTutorialStep Enum ====================

    @Test
    fun `ConnectionTutorialStep has 3 values`() {
        assertEquals(3, ConnectionTutorialStep.entries.size)
    }

    @Test
    fun `ConnectionTutorialStep CHOOSE_ARDUINO has correct displayName`() {
        assertEquals("Choose Your Arduino", ConnectionTutorialStep.CHOOSE_ARDUINO.displayName)
    }

    @Test
    fun `ConnectionTutorialStep CONNECTION_MODE has correct displayName`() {
        assertEquals("Connection Mode", ConnectionTutorialStep.CONNECTION_MODE.displayName)
    }

    @Test
    fun `ConnectionTutorialStep SETUP_FINAL has correct displayName`() {
        assertEquals("Setup & Features", ConnectionTutorialStep.SETUP_FINAL.displayName)
    }

    // ==================== FeatureType Enum ====================

    @Test
    fun `FeatureType has 2 values`() {
        assertEquals(2, FeatureType.entries.size)
    }

    @Test
    fun `FeatureType DEBUG_CONSOLE has correct displayName`() {
        assertEquals("Debug Console", FeatureType.DEBUG_CONSOLE.displayName)
    }

    @Test
    fun `FeatureType TELEMETRY has correct displayName`() {
        assertEquals("Telemetry & Monitoring", FeatureType.TELEMETRY.displayName)
    }

    @Test
    fun `FeatureType descriptions are not empty`() {
        for (feature in FeatureType.entries) {
            assertTrue("${feature.name} description should not be empty", feature.description.isNotEmpty())
        }
    }

    // ==================== ArduinoType Enum ====================

    @Test
    fun `ArduinoType has expected number of values`() {
        assertTrue(ArduinoType.entries.size >= 10)
    }

    @Test
    fun `ArduinoType UNO_Q exists`() {
        assertNotNull(ArduinoType.UNO_Q)
    }

    @Test
    fun `ArduinoType R4_WIFI exists`() {
        assertNotNull(ArduinoType.R4_WIFI)
    }

    @Test
    fun `ArduinoType UNO_OTHER exists`() {
        assertNotNull(ArduinoType.UNO_OTHER)
    }

    @Test
    fun `ArduinoType displayNames are not empty`() {
        for (type in ArduinoType.entries) {
            assertTrue("${type.name} displayName should not be empty", type.displayName.isNotEmpty())
        }
    }

    @Test
    fun `ArduinoType subtitles are not empty`() {
        for (type in ArduinoType.entries) {
            assertTrue("${type.name} subtitle should not be empty", type.subtitle.isNotEmpty())
        }
    }

    @Test
    fun `ArduinoType connectionTypes are not empty`() {
        for (type in ArduinoType.entries) {
            assertTrue("${type.name} connectionType should not be empty", type.connectionType.isNotEmpty())
        }
    }

    @Test
    fun `ArduinoType UNO_Q has BLE connectionType`() {
        assertEquals("BLE", ArduinoType.UNO_Q.connectionType)
    }

    @Test
    fun `ArduinoType R4_WIFI has BLE_WiFi connectionType`() {
        assertEquals("BLE/WiFi", ArduinoType.R4_WIFI.connectionType)
    }

    // ==================== Polymorphism ====================

    @Test
    fun `all OnboardingStep subtypes are OnboardingStep`() {
        val steps: List<OnboardingStep> = listOf(
            OnboardingStep.Welcome,
            OnboardingStep.InterfaceTour(InterfaceElement.ESTOP),
            OnboardingStep.ConnectionTutorial(ConnectionTutorialStep.CHOOSE_ARDUINO),
            OnboardingStep.Completion,
            OnboardingStep.Finished
        )

        assertEquals(5, steps.size)
        for (step in steps) {
            assertTrue(step is OnboardingStep)
        }
    }

    @Test
    fun `when expression covers all OnboardingStep types`() {
        val steps: List<OnboardingStep> = listOf(
            OnboardingStep.Welcome,
            OnboardingStep.InterfaceTour(InterfaceElement.ESTOP),
            OnboardingStep.ConnectionTutorial(ConnectionTutorialStep.CHOOSE_ARDUINO),
            OnboardingStep.Completion,
            OnboardingStep.Finished
        )

        for (step in steps) {
            val name = when (step) {
                is OnboardingStep.Welcome -> "Welcome"
                is OnboardingStep.InterfaceTour -> "InterfaceTour"
                is OnboardingStep.ConnectionTutorial -> "ConnectionTutorial"
                is OnboardingStep.Completion -> "Completion"
                is OnboardingStep.Finished -> "Finished"
            }
            assertTrue(name.isNotEmpty())
        }
    }
}
