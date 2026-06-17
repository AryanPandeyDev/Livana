package com.livana.app.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.Dp
import com.livana.app.core.designsystem.theme.ComponentDimens
import com.livana.app.core.designsystem.theme.LivanaColors

/**
 * Livana brand mark — INTERIM 4-petal bloom (jade petals, coral center).
 *
 * The final logo is NOT yet provided by the client. Keep all logo usage funneled through this
 * composable so the real asset can be swapped in ONE place (replace the body with the supplied
 * vector). See docs/android_ui_implementation_guide.md §6.
 */
@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    size: Dp = ComponentDimens.BrandMarkSize,
    petalColor: Color = LivanaColors.Primary,
    centerColor: Color = LivanaColors.Secondary,
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension
        val c = s / 2f
        val petalR = s * 0.165f
        val centerR = s * 0.115f
        val off = s * 0.27f
        drawCircle(petalColor, petalR, Offset(c, c - off))
        drawCircle(petalColor, petalR, Offset(c, c + off))
        drawCircle(petalColor, petalR, Offset(c - off, c))
        drawCircle(petalColor, petalR, Offset(c + off, c))
        drawCircle(centerColor, centerR, Offset(c, c))
    }
}
