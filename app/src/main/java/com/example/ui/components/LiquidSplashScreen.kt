package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun LiquidSplashScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splashLinearTransition")
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1050, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .drawWithCache {
                val w = size.width
                val h = size.height
                val minDim = size.minDimension.coerceAtLeast(1f)

                val baseBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF031033),
                        Color(0xFF061E5C),
                        Color(0xFF041542)
                    ),
                    start = Offset.Zero,
                    end = Offset(w, h)
                )

                val centerGlowBrush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2563EB).copy(alpha = 0.34f),
                        Color(0xFF0D9488).copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = minDim * 0.55f
                )

                onDrawBehind {
                    drawRect(brush = baseBrush)
                    drawCircle(
                        brush = centerGlowBrush,
                        radius = minDim * 0.55f,
                        center = Offset(w * 0.5f, h * 0.5f)
                    )
                }
            }
            .testTag("liquid_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Centered App Icon (matches Android system splash icon position seamlessly)
        Box(
            modifier = Modifier
                .size(108.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFF60A5FA).copy(alpha = 0.7f),
                            Color(0xFF2DD4BF).copy(alpha = 0.7f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.sait_launcher_fg),
                contentDescription = "SAIT Pass+",
                modifier = Modifier.size(86.dp),
                contentScale = ContentScale.Fit
            )
        }

        // Small Fluid Liquid Linear Loader directly below the centered icon
        SmallFluidLinearLoader(
            progress = shimmerProgress,
            modifier = Modifier
                .offset(y = 76.dp)
                .width(108.dp)
                .height(4.5.dp)
        )
    }
}

@Composable
private fun SmallFluidLinearLoader(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val corner = CornerRadius(size.height / 2f, size.height / 2f)

        // Frosted liquid glass capsule track
        drawRoundRect(
            color = Color.White.copy(alpha = 0.18f),
            size = size,
            cornerRadius = corner
        )

        // Smooth traveling fluid gradient bar
        val segmentWidth = size.width * 0.48f
        val startX = (size.width + segmentWidth) * progress - segmentWidth
        val clampedStart = startX.coerceIn(0f, size.width)
        val clampedEnd = (startX + segmentWidth).coerceIn(0f, size.width)

        if (clampedEnd > clampedStart) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF2DD4BF),
                        Color(0xFF60A5FA),
                        Color.White,
                        Color(0xFF38BDF8)
                    ),
                    startX = clampedStart,
                    endX = clampedEnd
                ),
                topLeft = Offset(clampedStart, 0f),
                size = Size(clampedEnd - clampedStart, size.height),
                cornerRadius = corner
            )
        }
    }
}
