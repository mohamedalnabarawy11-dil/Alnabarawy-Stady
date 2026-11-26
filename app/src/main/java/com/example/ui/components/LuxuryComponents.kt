package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.audio.SoundEngine
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.LocalNabraawyColors
import com.example.ui.theme.ObsidianPure
import com.example.ui.theme.SoftGoldGlow

@Composable
fun LuxuryPressableBox(
    onClick: () -> Unit,
    soundEngine: SoundEngine,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp),
    pressDepth: Dp = 3.5.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val dropOffset by animateDpAsState(
        targetValue = if (isPressed && enabled) pressDepth else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressDrop"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                translationY = dropOffset.toPx()
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = ChampagneGold.copy(alpha = 0.25f)),
                enabled = enabled,
                onClick = {
                    soundEngine.playClick()
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .alpha(if (enabled) 1f else 0.5f),
        content = content
    )
}

@Composable
fun LuxuryGoldButton(
    text: String,
    onClick: () -> Unit,
    soundEngine: SoundEngine,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isPrimary: Boolean = true,
    testTagName: String? = null
) {
    val palette = LocalNabraawyColors.current
    val shape = RoundedCornerShape(16.dp)
    val taggedModifier = if (testTagName != null) modifier.testTag(testTagName) else modifier

    val bgBrush = if (isPrimary) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFD6B463),
                ChampagneGold,
                Color(0xFFB38B36)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(palette.cardBg, palette.elevatedCardBg)
        )
    }

    val borderStroke = if (isPrimary) {
        BorderStroke(1.dp, SoftGoldGlow.copy(alpha = 0.65f))
    } else {
        BorderStroke(1.2.dp, palette.gold.copy(alpha = 0.7f))
    }

    val contentColor = if (isPrimary) ObsidianPure else palette.gold

    LuxuryPressableBox(
        onClick = onClick,
        soundEngine = soundEngine,
        enabled = enabled,
        shape = shape,
        modifier = taggedModifier
            .defaultMinSize(minHeight = 50.dp)
            .background(brush = bgBrush, shape = shape)
            .border(borderStroke, shape)
            .padding(horizontal = 18.dp, vertical = 13.dp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LuxurySectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector? = null,
    useIvoryContrast: Boolean = false,
    accentBorder: Boolean = true,
    headerAction: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalNabraawyColors.current
    val bgColor = if (useIvoryContrast) palette.contrastWhiteCardBg else palette.cardBg
    val fgColor = if (useIvoryContrast) palette.contrastWhiteText else palette.primaryText
    val goldAccent = if (useIvoryContrast && palette.isDark) Color(0xFF8E6B20) else palette.gold
    val borderColor = if (accentBorder) {
        if (useIvoryContrast) goldAccent.copy(alpha = 0.55f) else palette.goldBorder
    } else {
        palette.cardBorder
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = bgColor,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (title != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (icon != null) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(goldAccent.copy(alpha = 0.16f))
                                    .border(1.dp, goldAccent.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = title,
                                    tint = goldAccent,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = fgColor
                        )
                    }
                    if (headerAction != null) {
                        headerAction()
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                GoldHairlineDivider(color = goldAccent)
                Spacer(modifier = Modifier.height(12.dp))
            }
            content()
        }
    }
}

@Composable
fun GoldHairlineDivider(
    modifier: Modifier = Modifier,
    color: Color = LocalNabraawyColors.current.gold
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        color.copy(alpha = 0.55f),
                        Color.Transparent
                    )
                )
            )
    )
}

@Composable
fun TeacherLoadingCard(
    message: String,
    retryingMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalNabraawyColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "beacon")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LuxurySectionCard(modifier = modifier, accentBorder = true) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(palette.gold.copy(alpha = 0.15f * pulse))
                    .border(1.5.dp, palette.gold.copy(alpha = pulse), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(palette.gold.copy(alpha = pulse))
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.primaryText
                )
                if (!retryingMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = retryingMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.gold
                    )
                }
            }
        }
    }
}
