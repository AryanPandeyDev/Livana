package com.livana.app.feature.leaderboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.livana.app.core.designsystem.component.LivanaChip
import com.livana.app.core.designsystem.theme.Spacing
import com.livana.app.feature.leaderboard.BoardsSegment

/**
 * Segmented toggle between the Donors and NGOs leaderboards (10-ngo-leaderboard.html).
 * Built from [LivanaChip]; each segment fills half the row. Selecting a segment only
 * switches which already-loaded list renders — it does not refetch.
 */
@Composable
fun BoardsSegmentToggle(
    selected: BoardsSegment,
    onSelect: (BoardsSegment) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.S8),
    ) {
        LivanaChip(
            text = "Donors",
            selected = selected == BoardsSegment.Donors,
            onClick = { onSelect(BoardsSegment.Donors) },
            modifier = Modifier.weight(1f),
        )
        LivanaChip(
            text = "NGOs",
            selected = selected == BoardsSegment.Ngos,
            onClick = { onSelect(BoardsSegment.Ngos) },
            modifier = Modifier.weight(1f),
        )
    }
}
