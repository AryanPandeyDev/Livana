package com.livana.app.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// Tokens copied verbatim from docs/mockups/livana.css :root. Do not edit values ad-hoc.
object LivanaColors {
    val Canvas = Color(0xFFF7F4EF)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceAlt = Color(0xFFF1EEE8)
    val Surface2 = Color(0xFFFBF9F5)

    val Primary = Color(0xFF0F766E)
    val PrimaryPressed = Color(0xFF0B5C56)
    val PrimaryBright = Color(0xFF13A697)
    val PrimaryContainer = Color(0xFFD7F0EA)
    val PrimaryContainer2 = Color(0xFFE7F6F1)
    val OnPrimary = Color(0xFFFFFFFF)

    val Secondary = Color(0xFFF2785C)
    val SecondaryPressed = Color(0xFFE1623F)
    val SecondaryContainer = Color(0xFFFCE3DB)
    val SecondaryInk = Color(0xFFB4452A)

    val Gold = Color(0xFFE0A458)
    val GoldContainer = Color(0xFFFBEED6)

    val Text = Color(0xFF16201D)
    val TextSecondary = Color(0xFF566460)
    val TextMuted = Color(0xFF929A97)

    val Border = Color(0xFFE8E3DA)
    val Hairline = Color(0xFFEFEBE3)

    val Success = Color(0xFF1F9D6B)
    val Warning = Color(0xFFE0A458)
    val Error = Color(0xFFD5503F)
    val Info = Color(0xFF2F77B6)
    val Focus = Color(0xFF0F766E)

    // Interaction and translucent surface tokens used by the shared components.
    val TonalHovered = Color(0xFFC8EBE2)
    val NeutralHovered = Color(0xFFE7E2D9)
    val GlassSurface = Color(0xEBFFFFFF)
    val RegionSurface = Color(0xEDFFFFFF)
    val ShimmerHighlight = Color(0xFFECE7DE)
    val ErrorContainerSoft = Color(0xFFFBE4E0)

    // Gradient stops
    val GradHeroA = Color(0xFF159185)
    val GradHeroB = Color(0xFF0F766E)
    val GradHeroC = Color(0xFF0A5A55)
    val GradProgressA = Color(0xFF0F766E)
    val GradProgressB = Color(0xFF16AE9D)
    val GradCoralA = Color(0xFFF89070)
    val GradCoralB = Color(0xFFEC6A48)
    val ScrimBottom = Color(0xA8081C18) // rgba(8,28,24,0.66)
    val SheetScrim = Color(0x73081C18)  // rgba(8,28,24,0.45)
    val HeroHighlight = Color(0x2BFFFFFF)

    // Shadow colors copied from the CSS rgba values.
    val ShadowSmall = Color(0x0D14201B)
    val ShadowStandard = Color(0x1214201B)
    val ShadowLarge = Color(0x1F14201B)
    val ShadowJade = Color(0x470F766E)
    val ShadowCoral = Color(0x4DEC6A48)
    val ShadowGlass = Color(0x2E081C18)

    // Status pill text/bg not already covered
    val ReleasedBg = Color(0xFFDBF1E6)
    val ReleasedInk = Color(0xFF16774E)
    val PendingBg = Color(0xFFE2EEF7)
    val PendingInk = Color(0xFF235D86)
    val PausedInk = Color(0xFF946017)
}
