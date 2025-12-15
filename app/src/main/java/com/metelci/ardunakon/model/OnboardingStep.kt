package com.metelci.ardunakon.model

/**
 * Represents the current step in the onboarding flow.
 * Uses sealed class for type-safe state management.
 */
sealed class OnboardingStep {
    /** Phase 1: Welcome screen with value propositions */
    data object Welcome : OnboardingStep()
    
    /** Phase 2: Interface tour - highlighting UI elements */
    data class InterfaceTour(val element: InterfaceElement) : OnboardingStep()
    
    /** Phase 3: Connection tutorial - real device connection */
    data class ConnectionTutorial(val step: ConnectionTutorialStep) : OnboardingStep()
    

    
    /** Phase 5: Completion and next steps */
    data object Completion : OnboardingStep()
    
    /** Onboarding finished - user completed or skipped */
    data object Finished : OnboardingStep()
}

/**
 * UI elements highlighted during the interface tour phase.
 */
enum class InterfaceElement(val displayName: String, val description: String) {
    ESTOP("Emergency Stop", "Instantly stops all motors - the most important button!"),
    CONNECTION_STATUS("Connection Status", "Shows if you're connected to your Arduino device"),
    LEFT_JOYSTICK("Joystick", "Controls movement (direction and speed)"),
    SERVO_CONTROLS("Servo Buttons", "Control the camera, arm, or other attachments"),
    CONNECTION_MODE("Connection Mode", "Switch between Bluetooth and WiFi");
    
    companion object {
        /** Returns the ordered list of elements for the interface tour */
        fun tourOrder(): List<InterfaceElement> = listOf(
            ESTOP,
            CONNECTION_STATUS,
            LEFT_JOYSTICK,
            SERVO_CONTROLS,
            CONNECTION_MODE
        )
    }
}

/**
 * Steps within the connection tutorial phase.
 */
enum class ConnectionTutorialStep(val displayName: String) {
    CHOOSE_ARDUINO("Choose Your Arduino"),
    CONNECTION_MODE("Connection Mode"),
    SETUP_FINAL("Setup & Features")
}

/**
 * Advanced features that can be explored.
 */
enum class FeatureType(val displayName: String, val description: String) {
    DEBUG_CONSOLE("Debug Console", "See connection logs and troubleshoot issues"),
    TELEMETRY("Telemetry & Monitoring", "Watch battery voltage, signal strength, and more")
}

/**
 * Arduino board types for the connection tutorial.
 */
enum class ArduinoType(
    val displayName: String,
    val subtitle: String,
    val connectionType: String
) {
    UNO_Q("Arduino UNO Q", "Built-in Bluetooth 5.1", "BLE"),
    R4_WIFI("Arduino UNO R4 WiFi", "Built-in BLE + WiFi", "BLE/WiFi"),
    CLASSIC_BT("Classic Arduino + HC-05/HC-06", "External Bluetooth module", "Classic BT"),
    OTHER_BLE("Other BLE Modules", "HM-10, AT-09, MLT-BT05, etc.", "BLE")
}
