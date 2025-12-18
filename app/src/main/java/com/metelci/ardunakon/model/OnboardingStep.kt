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
    UNO_OTHER("Arduino UNO (other models)", "Use an external Bluetooth module (HC-05/HC-06/HM-10)", "Classic BT/BLE"),

    NANO_CLASSIC("Arduino Nano (classic/older)", "Use an external Bluetooth module (HC-05/HC-06/HM-10)", "Classic BT/BLE"),
    NANO_R4("Arduino Nano R4", "Select if your project is based on a Nano R4 board", "BLE/WiFi"),
    NANO_ESP32("Arduino Nano ESP32", "ESP32-based Nano (BLE/WiFi via your sketch)", "BLE/WiFi"),

    MEGA_2560("Arduino Mega 2560 Rev3", "Use an external Bluetooth module (HC-05/HC-06/HM-10)", "Classic BT/BLE"),
    GIGA_R1("Arduino GIGA R1", "Use BLE/WiFi depending on your sketch/firmware", "BLE/WiFi"),
    DUE("Arduino Due", "Use an external Bluetooth module or custom firmware", "Classic BT/BLE"),

    OTHER_BLE("Other BLE Modules", "HM-10, HC-08, AT-09, MLT-BT05, etc.", "BLE")
}
