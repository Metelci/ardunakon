@file:Suppress("DEPRECATION", "DEPRECATION_ERROR")

package com.metelci.ardunakon.ui.testutils

import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.drawscope.ContentDrawScope

internal object NoOpIndication : Indication {
    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance {
        return NoOpIndicationInstance
    }
}

internal object NoOpIndicationInstance : IndicationInstance {
    override fun ContentDrawScope.drawIndication() {
        drawContent()
    }
}
