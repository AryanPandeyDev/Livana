package com.livana.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import com.livana.app.core.designsystem.theme.Borders
import com.livana.app.core.designsystem.theme.ComponentDimens
import com.livana.app.core.designsystem.theme.ComponentTextStyles
import com.livana.app.core.designsystem.theme.InputShape
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.Spacing

@Composable
fun AddressText(
    address: String,
    modifier: Modifier = Modifier,
    showCopyIcon: Boolean = true,
    onCopied: (() -> Unit)? = null,
) {
    val clipboard = LocalClipboardManager.current
    val copy: () -> Unit = {
        clipboard.setText(AnnotatedString(address))
        onCopied?.invoke()
        Unit
    }
    Row(
        modifier = modifier
            .clip(InputShape)
            .clickable(role = Role.Button, onClick = copy)
            .semantics {
                role = Role.Button
                onClick(label = "Copy address") {
                    copy()
                    true
                }
            }
            .padding(vertical = Spacing.S8),
        horizontalArrangement = Arrangement.spacedBy(Spacing.S8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = truncateAddress(address),
            modifier = Modifier.weight(1f, fill = false),
            color = LivanaColors.TextSecondary,
            style = ComponentTextStyles.Address,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (showCopyIcon) {
            LivanaGlyph(LivanaGlyphKind.Copy, color = LivanaColors.TextSecondary)
        }
    }
}

enum class TxStepState {
    Done,
    Active,
    Pending,
    Failed,
}

data class TxStep(
    val title: String,
    val status: String? = null,
    val state: TxStepState,
    val transactionHash: String? = null,
)

@Composable
fun TxStepper(
    steps: List<TxStep>,
    modifier: Modifier = Modifier,
    onHashCopied: (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        steps.forEachIndexed { index, step ->
            TxStepRow(
                step = step,
                showConnector = index < steps.lastIndex,
                connectorDone = step.state == TxStepState.Done,
                onHashCopied = onHashCopied,
            )
        }
    }
}

@Composable
private fun TxStepRow(
    step: TxStep,
    showConnector: Boolean,
    connectorDone: Boolean,
    onHashCopied: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.S16),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TxStepNode(step.state)
            if (showConnector) {
                Box(
                    modifier = Modifier
                        .padding(vertical = Spacing.S4)
                        .width(Borders.Stepper)
                        .height(ComponentDimens.StepperConnectorMinimumHeight)
                        .background(if (connectorDone) LivanaColors.Primary else LivanaColors.Border),
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = Spacing.S4, bottom = Spacing.S8),
        ) {
            Text(
                text = step.title,
                color = if (step.state == TxStepState.Pending) LivanaColors.TextMuted else LivanaColors.Text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                ),
            )
            if (step.status != null) {
                Text(
                    text = step.status,
                    color = when (step.state) {
                        TxStepState.Failed -> LivanaColors.Error
                        TxStepState.Active, TxStepState.Done -> LivanaColors.Primary
                        TxStepState.Pending -> LivanaColors.TextMuted
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    ),
                    modifier = Modifier.padding(top = Spacing.S4),
                )
            }
            if (step.transactionHash != null) {
                LivanaCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.S8),
                    style = LivanaCardStyle.Flat,
                    contentPadding = PaddingValues(
                        horizontal = ComponentDimens.TransactionRowHorizontalPadding,
                        vertical = ComponentDimens.TransactionRowVerticalPadding,
                    ),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Tx",
                            color = LivanaColors.TextSecondary,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            ),
                        )
                        Spacer(Modifier.width(Spacing.S8))
                        AddressText(
                            address = step.transactionHash,
                            modifier = Modifier.weight(1f),
                            onCopied = onHashCopied,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TxStepNode(state: TxStepState) {
    val container = when (state) {
        TxStepState.Done -> LivanaColors.Primary
        TxStepState.Active -> LivanaColors.PrimaryContainer
        TxStepState.Pending -> LivanaColors.Surface
        TxStepState.Failed -> LivanaColors.ErrorContainerSoft
    }
    Box(
        modifier = Modifier
            .size(ComponentDimens.StepperNodeSize)
            .background(container, CircleShape)
            .then(
                if (state == TxStepState.Pending) {
                    Modifier.background(LivanaColors.Surface, CircleShape)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            TxStepState.Done -> LivanaGlyph(LivanaGlyphKind.Check, color = LivanaColors.OnPrimary)
            TxStepState.Active -> LivanaSpinner()
            TxStepState.Pending -> androidx.compose.foundation.Canvas(
                Modifier.size(ComponentDimens.StepperNodeSize),
            ) {
                drawCircle(
                    color = LivanaColors.Border,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(Borders.Stepper.toPx()),
                )
            }
            TxStepState.Failed -> LivanaGlyph(LivanaGlyphKind.Alert, color = LivanaColors.Error)
        }
    }
}

fun truncateAddress(value: String): String {
    if (value.length <= 11) return value
    return "${value.take(6)}\u2026${value.takeLast(4)}"
}
