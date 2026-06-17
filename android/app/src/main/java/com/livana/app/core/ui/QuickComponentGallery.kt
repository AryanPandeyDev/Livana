package com.livana.app.core.ui

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
import androidx.compose.material3.HorizontalDivider
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
import com.livana.app.core.designsystem.component.AddressText
import com.livana.app.core.designsystem.component.BrandMark
import com.livana.app.core.designsystem.component.IconButtonLivana
import com.livana.app.core.designsystem.component.IconChip
import com.livana.app.core.designsystem.component.IconChipTint
import com.livana.app.core.designsystem.component.LivanaButtonState
import com.livana.app.core.designsystem.component.LivanaCard
import com.livana.app.core.designsystem.component.LivanaCardStyle
import com.livana.app.core.designsystem.component.LivanaChip
import com.livana.app.core.designsystem.component.LivanaCoralButton
import com.livana.app.core.designsystem.component.LivanaIconButtonStyle
import com.livana.app.core.designsystem.component.LivanaOutlineButton
import com.livana.app.core.designsystem.component.LivanaPrimaryButton
import com.livana.app.core.designsystem.component.LivanaProgress
import com.livana.app.core.designsystem.component.LivanaTabs
import com.livana.app.core.designsystem.component.LivanaTextButton
import com.livana.app.core.designsystem.component.LivanaTextField
import com.livana.app.core.designsystem.component.LivanaTonalButton
import com.livana.app.core.designsystem.component.StatusPill
import com.livana.app.core.designsystem.component.StatusPillKind
import com.livana.app.core.designsystem.component.TxStep
import com.livana.app.core.designsystem.component.TxStepState
import com.livana.app.core.designsystem.component.TxStepper
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.LivanaTheme
import com.livana.app.core.designsystem.theme.Spacing

@Preview(name = "Livana - Quick component gallery", widthDp = 390, heightDp = 1600)
@Composable
private fun QuickComponentGalleryPreview() {
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
                verticalArrangement = Arrangement.spacedBy(Spacing.S16),
            ) {
                GallerySection("Brand") {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.S12)) {
                        BrandMark()
                        Text("Livana", style = MaterialTheme.typography.headlineMedium)
                    }
                }

                GallerySection("Buttons") {
                    LivanaPrimaryButton("Primary", {})
                    LivanaCoralButton("Coral", {})
                    LivanaTonalButton("Tonal", {})
                    LivanaOutlineButton("Outline", {})
                    LivanaTextButton("Text button", {})
                    LivanaPrimaryButton("Loading", {}, state = LivanaButtonState.Loading)
                    LivanaPrimaryButton("Disabled", {}, state = LivanaButtonState.Disabled)
                }

                GallerySection("Chips, pills, icon controls") {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.S8)) {
                        LivanaChip("Newest", selected = false, onClick = {})
                        LivanaChip("South Asia", selected = true, onClick = {})
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.S8)) {
                        StatusPill(StatusPillKind.Verified)
                        StatusPill(StatusPillKind.Featured, label = "Urgent")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.S8)) {
                        StatusPill(StatusPillKind.Paused)
                        StatusPill(StatusPillKind.Released)
                        StatusPill(StatusPillKind.Pending)
                    }
                    StatusPill(StatusPillKind.Region, label = "South Asia")
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.S8)) {
                        IconChip(IconChipTint.Jade) { BrandMark() }
                        IconChip(IconChipTint.Coral) { BrandMark(centerColor = LivanaColors.Primary) }
                        IconChip(IconChipTint.Gold) { BrandMark(centerColor = LivanaColors.Gold) }
                        IconChip(IconChipTint.Info) { BrandMark(centerColor = LivanaColors.Info) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.S8)) {
                        IconButtonLivana({}, "Standard icon button") { BrandMark() }
                        Box(
                            modifier = Modifier.background(
                                Brush.linearGradient(
                                    listOf(LivanaColors.GradHeroA, LivanaColors.GradHeroC),
                                ),
                            ),
                        ) {
                            IconButtonLivana(
                                onClick = {},
                                contentDescription = "Glass icon button",
                                style = LivanaIconButtonStyle.Glass,
                            ) {
                                BrandMark()
                            }
                        }
                    }
                }

                GallerySection("Cards, progress, fields") {
                    LivanaCard {
                        Text("Standard card", style = MaterialTheme.typography.titleLarge)
                    }
                    LivanaCard(style = LivanaCardStyle.Flat) {
                        Text("Flat card", style = MaterialTheme.typography.titleLarge)
                    }
                    LivanaCard(style = LivanaCardStyle.Media) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(Spacing.S40 * 3)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(LivanaColors.GradHeroA, LivanaColors.GradHeroC),
                                        ),
                                    ),
                            )
                            Text(
                                text = "Media card with token gradient fallback",
                                modifier = Modifier.padding(Spacing.CompactCard),
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
                    }
                    LivanaProgress(progress = 0.62f)
                    var fieldValue by remember { mutableStateOf("Clean Water Foundation") }
                    LivanaTextField(
                        value = fieldValue,
                        onValueChange = { fieldValue = it },
                        label = "Organization name",
                        helperText = "Helper/error space is reserved.",
                    )
                    LivanaTextField(
                        value = "10,000",
                        onValueChange = {},
                        label = "Target amount",
                        suffix = "USDC",
                    )
                    LivanaTextField(
                        value = "Short description of the cause.",
                        onValueChange = {},
                        label = "Description",
                        multiline = true,
                    )
                    LivanaTextField(
                        value = "Needs attention",
                        onValueChange = {},
                        label = "Error state",
                        errorText = "Please review this field.",
                    )
                }

                GallerySection("Tabs, address, transaction") {
                    var selectedTab by remember { mutableIntStateOf(0) }
                    LivanaTabs(
                        tabs = listOf("About", "Donations", "Proofs"),
                        selectedIndex = selectedTab,
                        onTabSelected = { selectedTab = it },
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
                        ),
                    )
                }

                GallerySection("Screen states") {
                    EmptyState(actionLabel = "Explore causes", onAction = {})
                    HorizontalDivider(color = LivanaColors.Hairline)
                    ErrorState(onRetry = {})
                    HorizontalDivider(color = LivanaColors.Hairline)
                    OfflineState(onRetry = {})
                    OfflineBanner(onRetry = {})
                }

                GallerySection("Skeletons") {
                    StatCardSkeleton()
                    PoolCardSkeleton()
                    ListRowSkeleton()
                    DetailScreenSkeleton()
                }
            }
        }
    }
}

@Composable
private fun GallerySection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.S12)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        content()
    }
}
