package com.livana.app.core.designsystem.theme

import android.animation.ValueAnimator
import androidx.compose.runtime.Composable

object MotionTokens {
    const val FastMillis = 200
    const val StandardMillis = 240
    const val SlowMillis = 280
    const val SpinnerMillis = 800
    const val ShimmerMillis = 1_400
    const val PressedScale = 0.985f
}

@Composable
fun reducedMotionEnabled(): Boolean = !ValueAnimator.areAnimatorsEnabled()
