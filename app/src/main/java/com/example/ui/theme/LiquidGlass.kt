package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Apple iOS 26 "Liquid Glass" Optical Tokens for Light & Dark Modes.
 * Inspired by Kyant0/AndroidLiquidGlass and QWEA0/Liquid-Glass-Android:
 * - Multi-stop translucent glass body
 * - Specular meniscus rim highlight (top-left bright, bottom-right refracted)
 * - Inner top-edge light dispersion
 * - Spring-physics tactile compression & lens bloom
 */
@Immutable
data class LiquidGlassPalette(
    val isDark: Boolean,
    val baseCanvasStart: Color,
    val baseCanvasEnd: Color,
    val orbPrimary: Color,
    val orbSecondary: Color,
    val orbTertiary: Color,
    val glassSurfaceTop: Color,
    val glassSurfaceBottom: Color,
    val glassElevatedTop: Color,
    val glassElevatedBottom: Color,
    val specularRimBright: Color,
    val specularRimMid: Color,
    val specularRimSubtle: Color,
    val innerMeniscusGlow: Color,
    val pillActiveTop: Color,
    val pillActiveBottom: Color,
    val pillActiveBorder: Color,
    val ambientShadowColor: Color
)

val LightLiquidGlassPalette = LiquidGlassPalette(
    isDark = false,
    baseCanvasStart = Color(0xFFEAF2FC),
    baseCanvasEnd = Color(0xFFF6F9FE),
    orbPrimary = Color(0xFF38BDF8).copy(alpha = 0.24f),
    orbSecondary = Color(0xFF6366F1).copy(alpha = 0.16f),
    orbTertiary = Color(0xFF14B8A6).copy(alpha = 0.18f),
    glassSurfaceTop = Color(0xFFFFFFFF).copy(alpha = 0.82f),
    glassSurfaceBottom = Color(0xFFF0F7FF).copy(alpha = 0.68f),
    glassElevatedTop = Color(0xFFFFFFFF).copy(alpha = 0.92f),
    glassElevatedBottom = Color(0xFFEFF6FF).copy(alpha = 0.80f),
    specularRimBright = Color(0xFFFFFFFF).copy(alpha = 0.98f),
    specularRimMid = Color(0xFF7DD3FC).copy(alpha = 0.55f),
    specularRimSubtle = Color(0xFFCBD5E1).copy(alpha = 0.35f),
    innerMeniscusGlow = Color(0xFFFFFFFF).copy(alpha = 0.70f),
    pillActiveTop = Color(0xFF0F2942).copy(alpha = 0.92f),
    pillActiveBottom = Color(0xFF0284C7).copy(alpha = 0.88f),
    pillActiveBorder = Color(0xFF7DD3FC).copy(alpha = 0.80f),
    ambientShadowColor = Color(0xFF0F2942).copy(alpha = 0.10f)
)

val DarkLiquidGlassPalette = LiquidGlassPalette(
    isDark = true,
    baseCanvasStart = Color(0xFF050A15),
    baseCanvasEnd = Color(0xFF0B1528),
    orbPrimary = Color(0xFF0EA5E9).copy(alpha = 0.26f),
    orbSecondary = Color(0xFF4F46E5).copy(alpha = 0.22f),
    orbTertiary = Color(0xFF10B981).copy(alpha = 0.18f),
    glassSurfaceTop = Color(0xFF1E293B).copy(alpha = 0.66f),
    glassSurfaceBottom = Color(0xFF0F172A).copy(alpha = 0.52f),
    glassElevatedTop = Color(0xFF26354D).copy(alpha = 0.78f),
    glassElevatedBottom = Color(0xFF131D31).copy(alpha = 0.68f),
    specularRimBright = Color(0xFFE0F2FE).copy(alpha = 0.52f),
    specularRimMid = Color(0xFF38BDF8).copy(alpha = 0.32f),
    specularRimSubtle = Color(0xFF1E293B).copy(alpha = 0.35f),
    innerMeniscusGlow = Color(0xFF7DD3FC).copy(alpha = 0.18f),
    pillActiveTop = Color(0xFF0284C7).copy(alpha = 0.85f),
    pillActiveBottom = Color(0xFF0369A1).copy(alpha = 0.75f),
    pillActiveBorder = Color(0xFF7DD3FC).copy(alpha = 0.70f),
    ambientShadowColor = Color(0xFF000000).copy(alpha = 0.45f)
)

val LocalLiquidGlassPalette = staticCompositionLocalOf { LightLiquidGlassPalette }

/**
 * Dynamic iOS 26 Liquid Glass Ambient Background with slow-breathing refractive light orbs.
 */
@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalLiquidGlassPalette.current
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_glass_aurora")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbPhase"
    )

    val breathe by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbBreathe"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val minDim = size.minDimension.coerceAtLeast(1f)

            // 1. Base atmospheric gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(palette.baseCanvasStart, palette.baseCanvasEnd)
                )
            )

            // 2. Top-left drifting sapphire/cyan refraction orb
            val orb1Center = Offset(
                x = w * (0.22f + 0.10f * cos(phase)),
                y = h * (0.16f + 0.08f * sin(phase))
            )
            val orb1Radius = (minDim * 0.62f * breathe).coerceAtLeast(10f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.orbPrimary, Color.Transparent),
                    center = orb1Center,
                    radius = orb1Radius
                ),
                center = orb1Center,
                radius = orb1Radius
            )

            // 3. Mid-right drifting indigo/violet refraction orb
            val orb2Center = Offset(
                x = w * (0.82f - 0.12f * sin(phase * 0.8f)),
                y = h * (0.46f + 0.10f * cos(phase * 0.8f))
            )
            val orb2Radius = (minDim * 0.58f * (2f - breathe)).coerceAtLeast(10f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.orbSecondary, Color.Transparent),
                    center = orb2Center,
                    radius = orb2Radius
                ),
                center = orb2Center,
                radius = orb2Radius
            )

            // 4. Bottom-left drifting teal/emerald prism orb
            val orb3Center = Offset(
                x = w * (0.30f + 0.14f * sin(phase)),
                y = h * (0.84f - 0.07f * cos(phase))
            )
            val orb3Radius = (minDim * 0.55f * breathe).coerceAtLeast(10f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.orbTertiary, Color.Transparent),
                    center = orb3Center,
                    radius = orb3Radius
                ),
                center = orb3Center,
                radius = orb3Radius
            )
        }

        content()
    }
}

/**
 * Core Apple iOS 26 Liquid Glass surface modifier:
 * Renders a translucent frosted lens fill, top meniscus light dispersion,
 * and a dual-angle specular refraction border.
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(24.dp),
    cornerRadius: Dp = 24.dp,
    elevated: Boolean = false,
    tintColor: Color? = null,
    strokeWidth: Dp = 1.25.dp,
    shadowElevation: Dp = 8.dp
): Modifier = composed {
    val palette = LocalLiquidGlassPalette.current
    val topFill = when {
        tintColor != null -> tintColor.copy(alpha = if (palette.isDark) 0.36f else 0.42f)
        elevated -> palette.glassElevatedTop
        else -> palette.glassSurfaceTop
    }
    val bottomFill = when {
        tintColor != null -> tintColor.copy(alpha = if (palette.isDark) 0.20f else 0.24f)
        elevated -> palette.glassElevatedBottom
        else -> palette.glassSurfaceBottom
    }

    this
        .shadow(
            elevation = shadowElevation,
            shape = shape,
            ambientColor = palette.ambientShadowColor,
            spotColor = palette.ambientShadowColor
        )
        .clip(shape)
        .drawWithCache {
            val radiusPx = cornerRadius.toPx()
            val strokePx = strokeWidth.toPx()

            val bodyBrush = Brush.verticalGradient(
                colors = listOf(topFill, bottomFill)
            )

            // Meniscus top reflection (simulates light refracting through curved glass top edge)
            val meniscusBrush = Brush.verticalGradient(
                colors = listOf(
                    palette.innerMeniscusGlow,
                    Color.Transparent
                ),
                startY = 0f,
                endY = (size.height * 0.32f).coerceAtLeast(12f)
            )

            // Diagonal specular rim border (bright top-left, cyan prism mid, bright bottom-right)
            val rimBrush = Brush.linearGradient(
                colors = listOf(
                    palette.specularRimBright,
                    palette.specularRimMid,
                    palette.specularRimSubtle,
                    palette.specularRimMid,
                    palette.specularRimBright.copy(alpha = if (palette.isDark) 0.38f else 0.78f)
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width.coerceAtLeast(1f), size.height.coerceAtLeast(1f))
            )

            onDrawWithContent {
                drawRoundRect(
                    brush = bodyBrush,
                    cornerRadius = CornerRadius(radiusPx, radiusPx)
                )
                drawRoundRect(
                    brush = meniscusBrush,
                    cornerRadius = CornerRadius(radiusPx, radiusPx)
                )
                drawContent()
                drawRoundRect(
                    brush = rimBrush,
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    style = Stroke(width = strokePx)
                )
            }
        }
}

/**
 * Spring-physics press scale & specular touch feedback inspired by iOS 26 Liquid Glass.
 */
fun Modifier.liquidPressEffect(
    pressedScale: Float = 0.968f,
    enabled: Boolean = true
): Modifier = composed {
    if (!enabled) return@composed this
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "liquidPressScale"
    )

    this
        .scale(scale)
        .pointerInput(enabled) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
        }
}

/**
 * High-level iOS 26 Liquid Glass Card container.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    elevated: Boolean = false,
    tintColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val baseModifier = if (onClick != null) {
        modifier
            .liquidPressEffect(pressedScale = 0.975f)
            .liquidGlass(
                shape = shape,
                cornerRadius = cornerRadius,
                elevated = elevated,
                tintColor = tintColor
            )
            .clickable(onClick = onClick)
    } else {
        modifier.liquidGlass(
            shape = shape,
            cornerRadius = cornerRadius,
            elevated = elevated,
            tintColor = tintColor
        )
    }

    Column(
        modifier = baseModifier,
        content = content
    )
}

/**
 * iOS 26 Segmented Liquid Glass Capsule Selector (e.g. Walk-In vs Appointment, Light vs Dark).
 */
@Composable
fun LiquidGlassSegmentedControl(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    icons: Map<String, ImageVector> = emptyMap(),
    testTagPrefix: String = "segment"
) {
    val palette = LocalLiquidGlassPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(18.dp),
                cornerRadius = 18.dp,
                elevated = false,
                shadowElevation = 4.dp
            )
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.0f else 0.97f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "segmentScale_$option"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                animationSpec = tween(200),
                label = "segmentText_$option"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(14.dp))
                    .testTag("${testTagPrefix}_${option.lowercase().replace("-", "").replace(" ", "_")}")
                    .then(
                        if (isSelected) {
                            Modifier
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(palette.pillActiveTop, palette.pillActiveBottom)
                                    )
                                )
                                .drawWithCache {
                                    val r = 14.dp.toPx()
                                    onDrawWithContent {
                                        drawContent()
                                        drawRoundRect(
                                            color = palette.pillActiveBorder,
                                            cornerRadius = CornerRadius(r, r),
                                            style = Stroke(width = 1.2.dp.toPx())
                                        )
                                    }
                                }
                        } else {
                            Modifier.background(Color.Transparent)
                        }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onOptionSelected(option) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val icon = icons[option]
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = option,
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Data model for an item in the floating iOS 26 Liquid Glass Bottom Dock.
 */
data class LiquidNavTabItem(
    val index: Int,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0,
    val badgeIsWarning: Boolean = false,
    val testTag: String
)

/**
 * Floating iOS 26 Liquid Glass Bottom Capsule Dock with spring-physics lens selection.
 */
@Composable
fun LiquidGlassBottomDock(
    items: List<LiquidNavTabItem>,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalLiquidGlassPalette.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("bottom_nav_bar"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(
                    shape = RoundedCornerShape(32.dp),
                    cornerRadius = 32.dp,
                    elevated = true,
                    strokeWidth = 1.5.dp,
                    shadowElevation = 16.dp
                )
                .padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = selectedTab == item.index
                val pillScale by animateFloatAsState(
                    targetValue = if (selected) 1.03f else 0.96f,
                    animationSpec = spring(
                        dampingRatio = 0.62f,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "dockScale_${item.index}"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(220),
                    label = "dockColor_${item.index}"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .scale(pillScale)
                        .clip(RoundedCornerShape(24.dp))
                        .testTag(item.testTag)
                        .then(
                            if (selected) {
                                Modifier
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(palette.pillActiveTop, palette.pillActiveBottom)
                                        )
                                    )
                                    .drawWithCache {
                                        val r = 24.dp.toPx()
                                        onDrawWithContent {
                                            drawContent()
                                            drawRoundRect(
                                                color = palette.pillActiveBorder,
                                                cornerRadius = CornerRadius(r, r),
                                                style = Stroke(width = 1.2.dp.toPx())
                                            )
                                        }
                                    }
                            } else {
                                Modifier
                            }
                        )
                        .clickable { onSelectTab(item.index) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                tint = contentColor,
                                modifier = Modifier.size(21.dp)
                            )
                            if (item.badgeCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 14.dp)
                                        .background(
                                            color = if (item.badgeIsWarning) StatusWarning else StatusSuccess,
                                            shape = CircleShape
                                        )
                                        .padding(horizontal = 5.dp, vertical = 1.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.badgeCount.toString(),
                                        color = if (item.badgeIsWarning) Color.Black else Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            color = contentColor,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
