package com.livana.app.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

val LivanaShapes = Shapes(
    extraSmall = RoundedCornerShape(Radii.Input),
    small = RoundedCornerShape(Radii.Input),
    medium = RoundedCornerShape(Radii.Card),
    large = RoundedCornerShape(Radii.Card),
    extraLarge = RoundedCornerShape(Radii.SheetTop),
)

// Pills / fully-rounded controls
val PillShape = RoundedCornerShape(percent = 50)
val IconChipShape = RoundedCornerShape(Radii.IconChip)
val CardShape = RoundedCornerShape(Radii.Card)
val ImageShape = RoundedCornerShape(Radii.Image)
val InputShape = RoundedCornerShape(Radii.Input)
val SheetShape = RoundedCornerShape(topStart = Radii.SheetTop, topEnd = Radii.SheetTop)
