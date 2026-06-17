package com.livana.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.livana.app.core.designsystem.theme.ComponentDimens
import com.livana.app.core.designsystem.theme.ComponentTextStyles
import com.livana.app.core.designsystem.theme.Borders
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.PillShape
import com.livana.app.core.designsystem.theme.SheetShape
import com.livana.app.core.designsystem.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LivanaBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = state,
        shape = SheetShape,
        containerColor = LivanaColors.Surface,
        contentColor = LivanaColors.Text,
        scrimColor = LivanaColors.SheetScrim,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = Spacing.S20, bottom = Spacing.S16)
                    .size(ComponentDimens.SheetGrabberWidth, ComponentDimens.SheetGrabberHeight)
                    .background(LivanaColors.Border, PillShape),
            )
        },
        content = {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Spacing.S20,
                        end = Spacing.S20,
                        bottom = Spacing.S20,
                    ),
                content = content,
            )
        },
    )
}

@Composable
fun LivanaTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(LivanaColors.Canvas)
            .drawBehind {
                drawLine(
                    color = LivanaColors.Hairline,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                    strokeWidth = Borders.Hairline.toPx(),
                )
            },
    ) {
        tabs.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(ComponentDimens.MinimumTouchTarget)
                    .clickable(
                        role = Role.Tab,
                        onClick = { onTabSelected(index) },
                    )
                    .semantics {
                        role = Role.Tab
                        selected = isSelected
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (isSelected) LivanaColors.Primary else LivanaColors.TextMuted,
                    style = ComponentTextStyles.Tab,
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(ComponentDimens.TabIndicatorHeight)
                        .background(if (isSelected) LivanaColors.Primary else Color.Transparent),
                )
            }
        }
    }
}
