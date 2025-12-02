package com.metelci.ardunakon.model

import com.metelci.ardunakon.model.ButtonConfig

data class AssignedAux(val config: ButtonConfig, val slot: Int, val servoId: Int, val role: String = "")
