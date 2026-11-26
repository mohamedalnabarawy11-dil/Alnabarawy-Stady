package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class NabraawyColors(
    val isDark: Boolean,
    val gold: Color,
    val goldSoft: Color,
    val goldDeep: Color,
    val goldBorder: Color,
    val cardBorder: Color,
    val screenBg: Color,
    val cardBg: Color,
    val elevatedCardBg: Color,
    val contrastWhiteCardBg: Color,
    val contrastWhiteText: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val success: Color,
    val successBg: Color,
    val error: Color,
    val errorBg: Color
)

val LocalNabraawyColors = staticCompositionLocalOf {
    NabraawyColors(
        isDark = true,
        gold = ChampagneGold,
        goldSoft = SoftGoldGlow,
        goldDeep = AntiqueGold,
        goldBorder = MutedGoldOutline,
        cardBorder = Color(0xFF2B2720),
        screenBg = ObsidianBlack,
        cardBg = ObsidianCard,
        elevatedCardBg = ObsidianElevated,
        contrastWhiteCardBg = IvoryWhite,
        contrastWhiteText = CharcoalInk,
        primaryText = IvoryWhite,
        secondaryText = WarmSilverText,
        success = EmeraldCorrect,
        successBg = EmeraldSoftBg,
        error = CrimsonError,
        errorBg = CrimsonSoftBg
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = ChampagneGold,
    onPrimary = ObsidianPure,
    primaryContainer = Color(0xFF2A2313),
    onPrimaryContainer = SoftGoldGlow,
    secondary = SoftGoldGlow,
    onSecondary = ObsidianPure,
    secondaryContainer = ObsidianElevated,
    onSecondaryContainer = IvoryWhite,
    tertiary = AntiqueGold,
    onTertiary = IvoryWhite,
    background = ObsidianBlack,
    onBackground = IvoryWhite,
    surface = ObsidianSurface,
    onSurface = IvoryWhite,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = WarmSilverText,
    outline = MutedGoldOutline,
    outlineVariant = Color(0xFF2A2A32),
    error = CrimsonError,
    onError = IvoryWhite
)

private val LightColorScheme = lightColorScheme(
    primary = AntiqueGold,
    onPrimary = PurePorcelain,
    primaryContainer = Color(0xFFF3E8CF),
    onPrimaryContainer = CharcoalInk,
    secondary = DeepBronzeGold,
    onSecondary = PurePorcelain,
    secondaryContainer = WarmAlabaster,
    onSecondaryContainer = CharcoalInk,
    tertiary = ChampagneGold,
    onTertiary = ObsidianPure,
    background = IvoryWhite,
    onBackground = CharcoalInk,
    surface = PurePorcelain,
    onSurface = CharcoalInk,
    surfaceVariant = WarmAlabaster,
    onSurfaceVariant = MutedSlateInk,
    outline = Color(0xFFCBB88B),
    outlineVariant = SoftPearl,
    error = CrimsonError,
    onError = PurePorcelain
)

private val LuxuryShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun NabraawyStudyTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val customColors = if (darkTheme) {
        NabraawyColors(
            isDark = true,
            gold = ChampagneGold,
            goldSoft = SoftGoldGlow,
            goldDeep = AntiqueGold,
            goldBorder = MutedGoldOutline,
            cardBorder = Color(0xFF2D271B),
            screenBg = ObsidianBlack,
            cardBg = ObsidianCard,
            elevatedCardBg = ObsidianElevated,
            contrastWhiteCardBg = IvoryWhite,
            contrastWhiteText = CharcoalInk,
            primaryText = IvoryWhite,
            secondaryText = WarmSilverText,
            success = Color(0xFF47B884),
            successBg = EmeraldSoftBg,
            error = Color(0xFFE06C6C),
            errorBg = CrimsonSoftBg
        )
    } else {
        NabraawyColors(
            isDark = false,
            gold = AntiqueGold,
            goldSoft = Color(0xFF6B5018),
            goldDeep = DeepBronzeGold,
            goldBorder = Color(0xFFD2BA82),
            cardBorder = Color(0xFFE2D8C3),
            screenBg = IvoryWhite,
            cardBg = PurePorcelain,
            elevatedCardBg = WarmAlabaster,
            contrastWhiteCardBg = ObsidianSurface,
            contrastWhiteText = IvoryWhite,
            primaryText = CharcoalInk,
            secondaryText = MutedSlateInk,
            success = EmeraldCorrect,
            successBg = Color(0xFFE6F4ED),
            error = CrimsonError,
            errorBg = Color(0xFFFCECEC)
        )
    }

    CompositionLocalProvider(LocalNabraawyColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NabraawyTypography,
            shapes = LuxuryShapes,
            content = content
        )
    }
}
