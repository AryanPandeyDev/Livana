package com.livana.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import com.livana.app.core.designsystem.theme.ComponentTextStyles
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.LivanaTheme
import com.livana.app.core.designsystem.theme.Spacing

@Preview(name = "Livana - Buttons", widthDp = 390, heightDp = 844)
@Composable
private fun ButtonsPreview() = GalleryFrame {
    GalleryTitle("Buttons")
    LivanaPrimaryButton("Primary", {})
    LivanaCoralButton("Coral", {})
    LivanaTonalButton("Tonal", {})
    LivanaOutlineButton("Outline", {})
    LivanaTextButton("Text button", {})
    LivanaPrimaryButton("Loading", {}, state = LivanaButtonState.Loading)
    LivanaPrimaryButton("Success", {}, state = LivanaButtonState.Success)
    LivanaPrimaryButton("Error", {}, state = LivanaButtonState.Error)
    LivanaPrimaryButton("Disabled", {}, state = LivanaButtonState.Disabled)
}

@Preview(name = "Livana - Chips and status", widthDp = 390, heightDp = 844)
@Composable
private fun ChipsPreview() = GalleryFrame {
    GalleryTitle("Chips and status")
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.S8)) {
        LivanaChip("Newest", selected = false, onClick = {})
        LivanaChip("South Asia", selected = true, onClick = {})
    }
    StatusPill(StatusPillKind.Verified)
    StatusPill(StatusPillKind.Featured, label = "Urgent")
    StatusPill(StatusPillKind.Paused)
    StatusPill(StatusPillKind.Released)
    StatusPill(StatusPillKind.Pending)
    StatusPill(StatusPillKind.Region, label = "South Asia")
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.S8)) {
        IconChip(IconChipTint.Jade) { GalleryGlyph(LivanaColors.Primary) }
        IconChip(IconChipTint.Coral) { GalleryGlyph(LivanaColors.SecondaryInk) }
        IconChip(IconChipTint.Gold) { GalleryGlyph(LivanaColors.PausedInk) }
        IconChip(IconChipTint.Info) { GalleryGlyph(LivanaColors.Info) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.S8)) {
        IconButtonLivana({}, "Standard icon") { GalleryGlyph(LivanaColors.TextSecondary) }
        Box(
            Modifier.background(
                Brush.linearGradient(listOf(LivanaColors.GradHeroA, LivanaColors.GradHeroC)),
            ),
        ) {
            IconButtonLivana(
                onClick = {},
                contentDescription = "Glass icon",
                style = LivanaIconButtonStyle.Glass,
            ) {
                GalleryGlyph(LivanaColors.Text)
            }
        }
    }
}

@Preview(name = "Livana - Cards, fields, progress", widthDp = 390, heightDp = 844)
@Composable
private fun ContentPreview() = GalleryFrame {
    GalleryTitle("Cards, fields, progress")
    LivanaCard {
        Text("Standard card", style = MaterialTheme.typography.titleLarge)
    }
    LivanaCard(style = LivanaCardStyle.Flat) {
        Text("Flat card", style = MaterialTheme.typography.titleLarge)
    }
    LivanaCard(style = LivanaCardStyle.Media) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(Spacing.S40 * 3)
                    .background(
                        Brush.linearGradient(
                            listOf(LivanaColors.GradHeroA, LivanaColors.GradHeroC),
                        ),
                    ),
            )
            Text(
                text = "Media card",
                modifier = Modifier.padding(Spacing.CompactCard),
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
    LivanaProgress(progress = 0.58f)
    var text by remember { mutableStateOf("Clean Water Foundation") }
    LivanaTextField(
        value = text,
        onValueChange = { text = it },
        label = "Organization name",
        helperText = "This helper space is always reserved.",
    )
    LivanaTextField(
        value = text,
        onValueChange = { text = it },
        label = "Description",
        errorText = "Please add more detail.",
        multiline = true,
        suffix = "USDC",
    )
    LivanaTextField(
        value = "Disabled",
        onValueChange = {},
        label = "Disabled field",
        enabled = false,
    )
}

@Preview(name = "Livana - Tabs and transaction", widthDp = 390, heightDp = 844)
@Composable
private fun TransactionPreview() = GalleryFrame {
    GalleryTitle("Tabs and transaction")
    var selected by remember { mutableIntStateOf(0) }
    LivanaTabs(
        tabs = listOf("About", "Donations", "Proofs"),
        selectedIndex = selected,
        onTabSelected = { selected = it },
    )
    AddressText("0x9965507D1a55bcC2695C58ba16FB37d819B0A4dc")
    TxStepper(
        steps = listOf(
            TxStep(
                title = "Approve USDC",
                status = "Complete",
                state = TxStepState.Done,
                transactionHash = "0x2bf5f213aabbccddeeff0011223344556677a81b",
            ),
            TxStep(
                title = "Donate",
                status = "Confirming on-chain\u2026",
                state = TxStepState.Active,
            ),
            TxStep(title = "Done", state = TxStepState.Pending),
            TxStep(
                title = "Failed example",
                status = "Transaction reverted",
                state = TxStepState.Failed,
            ),
        ),
    )
}

@Preview(name = "Livana - Brand mark", widthDp = 390, heightDp = 220)
@Composable
private fun BrandPreview() = GalleryFrame {
    GalleryTitle("Brand mark")
    BrandMark()
}

@Preview(name = "Livana - Bottom sheet", widthDp = 390, heightDp = 844)
@Composable
private fun BottomSheetPreview() {
    LivanaTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = LivanaColors.Canvas) {
            LivanaBottomSheet(onDismissRequest = {}) {
                Text(
                    text = "Confirmation",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "A reusable sheet with the exact grabber, rounded top, and dim scrim.",
                    modifier = Modifier.padding(vertical = Spacing.S16),
                    color = LivanaColors.TextSecondary,
                    style = MaterialTheme.typography.bodyLarge,
                )
                LivanaPrimaryButton("Got it", {})
            }
        }
    }
}

@Composable
private fun GalleryFrame(content: @Composable ColumnScope.() -> Unit) {
    LivanaTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = LivanaColors.Canvas,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.ScreenHorizontal),
                verticalArrangement = Arrangement.spacedBy(Spacing.S12),
                content = content,
            )
        }
    }
}

@Composable
private fun GalleryTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.headlineLarge)
}

@Composable
private fun GalleryGlyph(color: androidx.compose.ui.graphics.Color) {
    LivanaGlyph(kind = LivanaGlyphKind.Check, color = color)
}
