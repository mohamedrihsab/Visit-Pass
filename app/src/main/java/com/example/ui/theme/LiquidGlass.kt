package com.example.ui.theme

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.intellij.lang.annotations.Language

/**
 * QWEA0/Liquid-Glass-Android Multi-Tier Optical Pipeline:
 * - Min SDK 24 (Android 7.0+): Classic multi-layer Canvas SDF + Prismatic Dispersion pipeline
 * - API 33+ (Android 13+): Single-pass AGSL `RuntimeShader` Liquid Glass 2.0 Lens Pipeline
 *   (`sdRoundedBox` SDF, 3D surface normals, Snell's Law RGB chromatic dispersion,
 *   Schlick Fresnel rim reflection, and curved water-droplet crescent meniscus)
 * - API 36+ (Android 16+): Automatic AGSL Color Filter / Vibrance Blend enhancements (`uApi36Enhance`)
 */
object LiquidGlassShaderEngine {

    val isRuntimeShaderSupported: Boolean by lazy {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
    }

    val isApi36BlendEnhanced: Boolean by lazy {
        Build.VERSION.SDK_INT >= 36
    }

    @Language("AGSL")
    private const val LIQUID_GLASS_V2_AGSL_SOURCE = """
        uniform float2 uResolution;
        uniform float uCornerRadius;
        uniform float uIsDark;
        uniform float uIsButton;
        uniform float uApi36Enhance;
        uniform float4 uTintTop;
        uniform float4 uTintBottom;

        float sdRoundedBox(float2 p, float2 b, float r) {
            float2 q = abs(p) - b + float2(r, r);
            return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
        }

        half4 main(float2 fragCoord) {
            float2 res = max(uResolution, float2(1.0, 1.0));
            float2 uv = fragCoord / res;
            float2 halfSize = res * 0.5;
            float r = min(uCornerRadius, min(halfSize.x, halfSize.y));
            float2 p = fragCoord - halfSize;

            // 1. Signed Distance Field (sdRoundedBox)
            float d = sdRoundedBox(p, halfSize, r);
            float insideDist = max(-d, 0.0);

            // 2. Analytical finite-difference SDF gradient & 3D convex lens normal
            float eps = 1.5;
            float dx = sdRoundedBox(p + float2(eps, 0.0), halfSize, r) - sdRoundedBox(p - float2(eps, 0.0), halfSize, r);
            float dy = sdRoundedBox(p + float2(0.0, eps), halfSize, r) - sdRoundedBox(p - float2(0.0, eps), halfSize, r);
            float2 grad = normalize(float2(dx, dy) + float2(0.0001, 0.0001));

            float bevelWidth = mix(24.0, 18.0, uIsButton);
            float edgeCompression = exp(-insideDist / bevelWidth);
            float2 nXY = grad * edgeCompression * 0.90;
            float nZ = sqrt(max(1.0 - dot(nXY, nXY), 0.04));
            float3 normal = normalize(float3(nXY, nZ));

            // 3. Base Translucent Lens Body + Convex Droplet Depth
            float3 baseColor = mix(uTintTop.rgb, uTintBottom.rgb, clamp(uv.y, 0.0, 1.0));
            float baseAlpha = mix(uTintTop.a, uTintBottom.a, clamp(uv.y, 0.0, 1.0));

            // Convex water-droplet top-left depth shadow & center illumination
            float dropletShadow = smoothstep(0.0, 0.60, 1.0 - (uv.x * 0.65 + uv.y * 0.75)) * edgeCompression;
            float3 depthTint = mix(float3(0.01, 0.12, 0.38), float3(0.01, 0.04, 0.20), uIsDark);
            baseColor = mix(baseColor, depthTint, dropletShadow * mix(0.18, 0.36, uIsButton));

            // 4. Schlick Fresnel Reflection & Directional Specular Lighting (Amplified Ultra-Prominent)
            float3 lightDir = normalize(float3(-0.55, -0.68, 0.48));
            float specTopLeft = pow(max(dot(normal, lightDir), 0.0), 9.0);
            float3 rimLightDir = normalize(float3(0.55, 0.65, 0.52));
            float specBottomRight = pow(max(dot(normal, rimLightDir), 0.0), 7.0);
            float fresnel = 0.08 + 0.92 * pow(clamp(1.0 - normal.z, 0.0, 1.0), 3.2);

            // 5. QWEA0 Physical RGB Chromatic Dispersion Fringe along curved SDF bevel
            float prismBand = exp(-pow((insideDist - 2.8) / 2.5, 2.0));
            float angleFactor = clamp((grad.x + grad.y) * 0.5 + 0.5, 0.0, 1.0);
            float3 coralGold = mix(float3(1.0, 0.38, 0.55), float3(1.0, 0.78, 0.30), clamp(uv.x, 0.0, 1.0));
            float3 cyanEmerald = mix(float3(0.0, 0.95, 1.0), float3(0.0, 0.95, 0.52), clamp(uv.y, 0.0, 1.0));
            float3 prismColor = mix(coralGold, cyanEmerald, angleFactor);

            // 6. Curved Water-Droplet Crescent Meniscus Ring
            float crescentRing = exp(-pow((insideDist - 6.0) / 2.4, 2.0)) * smoothstep(0.12, 0.95, abs(grad.x));
            float3 crescentColor = mix(float3(1.0, 0.98, 1.0), float3(0.70, 0.96, 1.0), angleFactor);

            // Combine optical layers with ultra-prominent radiance
            float3 color = baseColor;
            color += float3(0.92, 0.98, 1.0) * (specTopLeft * 0.34 + fresnel * 0.28);
            color += float3(0.26, 0.82, 1.0) * (specBottomRight * 0.24);
            color = mix(color, prismColor, prismBand * mix(0.48, 0.62, uIsDark));
            color += crescentColor * (crescentRing * mix(0.42, 0.36, uIsDark));

            // 7. API 36+ (Android 16) AGSL Color Filter / Harmonic Vibrance Blend Enhancement
            if (uApi36Enhance > 0.5) {
                float luma = dot(color, float3(0.2126, 0.7152, 0.0722));
                float3 chroma = color - float3(luma, luma, luma);
                color = float3(luma, luma, luma) + chroma * 1.25 + float3(0.02, 0.04, 0.08) * fresnel;
            }

            float finalAlpha = clamp(baseAlpha + fresnel * 0.22 + prismBand * 0.26 + crescentRing * 0.18, 0.0, 0.98);
            return half4(half3(clamp(color, 0.0, 1.0)), half(finalAlpha));
        }
    """

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun createConfiguredLensBrush(
        width: Float,
        height: Float,
        cornerRadiusPx: Float,
        isDark: Boolean,
        isButton: Boolean,
        topColor: Color,
        bottomColor: Color
    ): ShaderBrush? {
        if (!isRuntimeShaderSupported) return null
        return try {
            val shader = RuntimeShader(LIQUID_GLASS_V2_AGSL_SOURCE)
            shader.setFloatUniform("uResolution", width.coerceAtLeast(1f), height.coerceAtLeast(1f))
            shader.setFloatUniform("uCornerRadius", cornerRadiusPx)
            shader.setFloatUniform("uIsDark", if (isDark) 1f else 0f)
            shader.setFloatUniform("uIsButton", if (isButton) 1f else 0f)
            shader.setFloatUniform("uApi36Enhance", if (isApi36BlendEnhanced) 1f else 0f)
            shader.setFloatUniform(
                "uTintTop",
                topColor.red,
                topColor.green,
                topColor.blue,
                topColor.alpha
            )
            shader.setFloatUniform(
                "uTintBottom",
                bottomColor.red,
                bottomColor.green,
                bottomColor.blue,
                bottomColor.alpha
            )
            ShaderBrush(shader)
        } catch (_: Throwable) {
            null
        }
    }
}

@Immutable
data class LiquidGlassPalette(
    val isDark: Boolean,
    val meshBgTopLeft: Color,
    val meshBgCenter: Color,
    val meshBgBottomRight: Color,
    val waveSpherePrimary: Color,
    val waveSphereSecondary: Color,
    val waveEmeraldStart: Color,
    val waveEmeraldEnd: Color,
    val waveEmeraldRim: Color,
    val waveIndigoStart: Color,
    val waveIndigoEnd: Color,
    val waveIndigoRim: Color,
    val orbCyan: Color,
    val orbViolet: Color,
    val orbEmerald: Color,
    val orbRose: Color,
    val causticBeamColor: Color,
    val ambientShadowColor: Color,
    val outerContrastRim: Color,
    val glassSurfaceTop: Color,
    val glassSurfaceBottom: Color,
    val glassElevatedTop: Color,
    val glassElevatedBottom: Color,
    val innerTileTop: Color,
    val innerTileBottom: Color,
    val innerTileSelectedTop: Color,
    val innerTileSelectedBottom: Color,
    val sdfRefractionRingTop: Color,
    val sdfRefractionRingBottom: Color,
    val dispersionCoralRose: Color,
    val dispersionGold: Color,
    val dispersionCyan: Color,
    val dispersionEmerald: Color,
    val dispersionMagenta: Color,
    val specularRimBright: Color,
    val specularRimMid: Color,
    val specularRimEnd: Color,
    val innerMeniscusGlow: Color,
    val diagonalSheenColor: Color,
    val pressFlareColor: Color,
    val pillActiveTop: Color,
    val pillActiveBottom: Color,
    val pillActiveBorder: Color,
    val inputFieldContainer: Color,
    val inputFieldBorderUnfocused: Color,
    val chipUnselectedContainer: Color
)

val LightLiquidGlassPalette = LiquidGlassPalette(
    isDark = false,
    meshBgTopLeft = Color(0xFF93C5FD),
    meshBgCenter = Color(0xFFDbeafe),
    meshBgBottomRight = Color(0xFFA5F3FC),
    waveSpherePrimary = Color(0xFF3B82F6).copy(alpha = 0.36f),
    waveSphereSecondary = Color(0xFF1D4ED8).copy(alpha = 0.20f),
    waveEmeraldStart = Color(0xFF10B981).copy(alpha = 0.45f),
    waveEmeraldEnd = Color(0xFF06B6D4).copy(alpha = 0.32f),
    waveEmeraldRim = Color(0xFFFFFFFF).copy(alpha = 0.85f),
    waveIndigoStart = Color(0xFF2563EB).copy(alpha = 0.28f),
    waveIndigoEnd = Color(0xFF4F46E5).copy(alpha = 0.18f),
    waveIndigoRim = Color(0xFFBAE6FD).copy(alpha = 0.88f),
    orbCyan = Color(0xFF0284C7).copy(alpha = 0.34f),
    orbViolet = Color(0xFF4F46E5).copy(alpha = 0.26f),
    orbEmerald = Color(0xFF0D9488).copy(alpha = 0.28f),
    orbRose = Color(0xFFEC4899).copy(alpha = 0.18f),
    causticBeamColor = Color(0xFFFFFFFF).copy(alpha = 0.65f),
    ambientShadowColor = Color(0xFF03396C),
    outerContrastRim = Color(0xFF0284C7).copy(alpha = 0.36f),
    // High-contrast frosted glass surfaces so dark ink text is 100% legible in Light Mode
    glassSurfaceTop = Color(0xFFFFFFFF).copy(alpha = 0.84f),
    glassSurfaceBottom = Color(0xFFF0F9FF).copy(alpha = 0.74f),
    glassElevatedTop = Color(0xFFFFFFFF).copy(alpha = 0.90f),
    glassElevatedBottom = Color(0xFFF0FDFA).copy(alpha = 0.80f),
    innerTileTop = Color(0xFFFFFFFF).copy(alpha = 0.82f),
    innerTileBottom = Color(0xFFE0F2FE).copy(alpha = 0.68f),
    innerTileSelectedTop = Color(0xFFBAE6FD).copy(alpha = 0.86f),
    innerTileSelectedBottom = Color(0xFFC7D2FE).copy(alpha = 0.76f),
    sdfRefractionRingTop = Color(0xFFFFFFFF).copy(alpha = 0.94f),
    sdfRefractionRingBottom = Color(0xFF0284C7).copy(alpha = 0.45f),
    dispersionCoralRose = Color(0xFFFF5E7E).copy(alpha = 0.58f),
    dispersionGold = Color(0xFFFFB347).copy(alpha = 0.52f),
    dispersionCyan = Color(0xFF00B8D4).copy(alpha = 0.65f),
    dispersionEmerald = Color(0xFF00C853).copy(alpha = 0.54f),
    dispersionMagenta = Color(0xFF7C3AED).copy(alpha = 0.48f),
    specularRimBright = Color(0xFFFFFFFF).copy(alpha = 0.98f),
    specularRimMid = Color(0xFF38BDF8).copy(alpha = 0.78f),
    specularRimEnd = Color(0xFFFFFFFF).copy(alpha = 0.94f),
    innerMeniscusGlow = Color(0xFFFFFFFF).copy(alpha = 0.88f),
    diagonalSheenColor = Color(0xFFFFFFFF).copy(alpha = 0.42f),
    pressFlareColor = Color(0xFF38BDF8).copy(alpha = 0.34f),
    pillActiveTop = Color(0xFF00558F),
    pillActiveBottom = Color(0xFF1D4ED8),
    pillActiveBorder = Color(0xFFBAE6FD),
    inputFieldContainer = Color(0xFFFFFFFF).copy(alpha = 0.86f),
    inputFieldBorderUnfocused = Color(0xFF026AA7).copy(alpha = 0.48f),
    chipUnselectedContainer = Color(0xFFFFFFFF).copy(alpha = 0.82f)
)

val DarkLiquidGlassPalette = LiquidGlassPalette(
    isDark = true,
    meshBgTopLeft = Color(0xFF020B24),
    meshBgCenter = Color(0xFF061B48),
    meshBgBottomRight = Color(0xFF02081A),
    waveSpherePrimary = Color(0xFF155EEF).copy(alpha = 0.60f),
    waveSphereSecondary = Color(0xFF062366).copy(alpha = 0.42f),
    waveEmeraldStart = Color(0xFF00D492).copy(alpha = 0.68f),
    waveEmeraldEnd = Color(0xFF0D9488).copy(alpha = 0.48f),
    waveEmeraldRim = Color(0xFF6EE7B7).copy(alpha = 0.85f),
    waveIndigoStart = Color(0xFF041446).copy(alpha = 0.88f),
    waveIndigoEnd = Color(0xFF0D2E8A).copy(alpha = 0.72f),
    waveIndigoRim = Color(0xFF2563EB).copy(alpha = 0.90f),
    orbCyan = Color(0xFF00E5FF).copy(alpha = 0.30f),
    orbViolet = Color(0xFF6366F1).copy(alpha = 0.28f),
    orbEmerald = Color(0xFF10B981).copy(alpha = 0.26f),
    orbRose = Color(0xFFFF5E7E).copy(alpha = 0.18f),
    causticBeamColor = Color(0xFF38BDF8).copy(alpha = 0.18f),
    ambientShadowColor = Color(0xFF000000),
    outerContrastRim = Color(0xFF38BDF8).copy(alpha = 0.40f),
    // High-contrast deep sapphire glass surfaces so ice-white text pops with 100% clarity
    glassSurfaceTop = Color(0xFF0E2248).copy(alpha = 0.78f),
    glassSurfaceBottom = Color(0xFF061229).copy(alpha = 0.70f),
    glassElevatedTop = Color(0xFF132C5C).copy(alpha = 0.84f),
    glassElevatedBottom = Color(0xFF081736).copy(alpha = 0.76f),
    innerTileTop = Color(0xFF152D5A).copy(alpha = 0.74f),
    innerTileBottom = Color(0xFF0A1834).copy(alpha = 0.64f),
    innerTileSelectedTop = Color(0xFF155EEF).copy(alpha = 0.74f),
    innerTileSelectedBottom = Color(0xFF0284C7).copy(alpha = 0.64f),
    sdfRefractionRingTop = Color(0xFFBAE6FD).copy(alpha = 0.46f),
    sdfRefractionRingBottom = Color(0xFF38BDF8).copy(alpha = 0.34f),
    dispersionCoralRose = Color(0xFFFF5E7E).copy(alpha = 0.76f),
    dispersionGold = Color(0xFFFFB74D).copy(alpha = 0.64f),
    dispersionCyan = Color(0xFF00E5FF).copy(alpha = 0.78f),
    dispersionEmerald = Color(0xFF00E676).copy(alpha = 0.70f),
    dispersionMagenta = Color(0xFFC084FC).copy(alpha = 0.60f),
    specularRimBright = Color(0xFFE0F2FE).copy(alpha = 0.92f),
    specularRimMid = Color(0xFF38BDF8).copy(alpha = 0.72f),
    specularRimEnd = Color(0xFF6EE7B7).copy(alpha = 0.80f),
    innerMeniscusGlow = Color(0xFFBAE6FD).copy(alpha = 0.36f),
    diagonalSheenColor = Color(0xFF60A5FA).copy(alpha = 0.16f),
    pressFlareColor = Color(0xFF38BDF8).copy(alpha = 0.38f),
    pillActiveTop = Color(0xFF155EEF),
    pillActiveBottom = Color(0xFF0284C7),
    pillActiveBorder = Color(0xFF7DD3FC),
    inputFieldContainer = Color(0xFF0A1936).copy(alpha = 0.80f),
    inputFieldBorderUnfocused = Color(0xFF38BDF8).copy(alpha = 0.48f),
    chipUnselectedContainer = Color(0xFF12264A).copy(alpha = 0.78f)
)

val LocalLiquidGlassPalette = staticCompositionLocalOf { LightLiquidGlassPalette }

/**
 * Animated QWEA0/Liquid-Glass-Android Curved Geometric Wave & Sphere Wallpaper Background.
 *
 * Features smooth, continuous floating and breathing motion on the refractive spheres,
 * emerald wave, and caustic light shaft so glass lenses dynamically refract moving light.
 */
@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalLiquidGlassPalette.current
    val infiniteTransition = rememberInfiniteTransition(label = "liquidGlassAmbientWave")

    val waveDrift by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveDrift"
    )

    val orbBreath by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbBreath"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val w = size.width.coerceAtLeast(1f)
                val h = size.height.coerceAtLeast(1f)
                val minDim = size.minDimension.coerceAtLeast(1f)
                val maxDim = size.maxDimension.coerceAtLeast(1f)

                val baseBrush = Brush.linearGradient(
                    colors = listOf(
                        palette.meshBgTopLeft,
                        palette.meshBgCenter,
                        palette.meshBgBottomRight
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )

                val indigoWaveBrush = Brush.linearGradient(
                    colors = listOf(
                        palette.waveIndigoStart,
                        palette.waveIndigoEnd
                    ),
                    start = Offset(w * 0.40f, 0f),
                    end = Offset(w, h)
                )

                val emeraldWaveBrush = Brush.linearGradient(
                    colors = listOf(
                        palette.waveEmeraldStart,
                        palette.waveEmeraldEnd
                    ),
                    start = Offset(0f, h * 0.38f),
                    end = Offset(w * 0.65f, h)
                )

                val causticBeam = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        palette.causticBeamColor,
                        Color.Transparent
                    ),
                    start = Offset(0f, h * 0.16f),
                    end = Offset(w, h * 0.52f)
                )

                onDrawBehind {
                    val driftX = waveDrift * (w * 0.032f)
                    val driftY = waveDrift * (h * 0.024f)

                    // 1. Base Sky/Midnight Canvas
                    drawRect(brush = baseBrush)

                    // 2. Ambient Upper Orbs with gentle breathing motion
                    val topOrbCenter = Offset(w * 0.85f - driftX, h * 0.14f + driftY)
                    val topOrbRadius = minDim * 0.62f * orbBreath
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(palette.orbCyan, Color.Transparent),
                            center = topOrbCenter,
                            radius = topOrbRadius
                        ),
                        radius = topOrbRadius,
                        center = topOrbCenter
                    )

                    val midOrbCenter = Offset(w * 0.18f + driftX, h * 0.24f - driftY)
                    val midOrbRadius = minDim * 0.48f * orbBreath
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(palette.orbRose, Color.Transparent),
                            center = midOrbCenter,
                            radius = midOrbRadius
                        ),
                        radius = midOrbRadius,
                        center = midOrbCenter
                    )

                    // 3. Large Cobalt-Sapphire Lit Sphere
                    val cobaltCenter = Offset(w * 0.44f + driftX * 0.7f, h * 0.36f + driftY * 0.5f)
                    val cobaltRadius = maxDim * 0.48f * orbBreath
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                palette.waveSpherePrimary,
                                palette.waveSphereSecondary,
                                Color.Transparent
                            ),
                            center = Offset(w * 0.34f + driftX, h * 0.28f + driftY),
                            radius = cobaltRadius
                        ),
                        radius = cobaltRadius,
                        center = cobaltCenter
                    )
                    drawCircle(
                        color = palette.waveIndigoRim.copy(alpha = 0.45f),
                        radius = cobaltRadius,
                        center = cobaltCenter,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // 4. Overlapping Right Indigo Crescent Arc with Glowing Blue Specular Edge
                    val indigoCenter = Offset(w * 0.92f - driftX * 0.5f, h * 0.46f + driftY * 0.6f)
                    val indigoRadius = maxDim * 0.56f
                    drawCircle(
                        brush = indigoWaveBrush,
                        radius = indigoRadius,
                        center = indigoCenter
                    )
                    drawCircle(
                        color = palette.waveIndigoRim,
                        radius = indigoRadius,
                        center = indigoCenter,
                        style = Stroke(width = 2.4.dp.toPx())
                    )

                    // 5. Signature Bottom-Left Emerald-Teal Curved Wave with Bright Specular Rim
                    val emeraldCenter = Offset(-w * 0.12f + driftX * 0.6f, h * 0.82f - driftY * 0.5f)
                    val emeraldRadius = maxDim * 0.58f
                    drawCircle(
                        brush = emeraldWaveBrush,
                        radius = emeraldRadius,
                        center = emeraldCenter
                    )
                    drawCircle(
                        color = palette.waveEmeraldRim,
                        radius = emeraldRadius,
                        center = emeraldCenter,
                        style = Stroke(width = 2.6.dp.toPx())
                    )

                    // 6. Diagonal Caustic Light Shaft
                    drawRect(brush = causticBeam)
                }
            }
    ) {
        content()
    }
}

/**
 * Smooth iOS 26 Liquid Glass Spring Entrance Animation Modifier.
 * Animates a subtle vertical float, refractive scale bloom, and alpha fade-in when entering composition.
 */
@Composable
fun Modifier.liquidAppearAnimation(
    delayMillis: Int = 0,
    initialScale: Float = 0.95f,
    initialOffsetYDp: Float = 14f
): Modifier {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (delayMillis > 0) {
            delay(delayMillis.toLong())
        }
        appeared = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "liquidAppearAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else initialScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "liquidAppearScale"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (appeared) 0f else initialOffsetYDp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "liquidAppearOffsetY"
    )

    return this.graphicsLayer {
        this.alpha = alpha
        this.scaleX = scale
        this.scaleY = scale
        this.translationY = offsetY * density
    }
}

/**
 * Core QWEA0/Liquid-Glass-Android & iOS 26 Convex Lens Surface Modifier.
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(24.dp),
    cornerRadius: Dp = 24.dp,
    elevated: Boolean = false,
    tintColor: Color? = null,
    strokeWidth: Dp = 1.6.dp,
    shadowElevation: Dp = 10.dp
): Modifier {
    val palette = LocalLiquidGlassPalette.current
    val topFill = when {
        tintColor != null -> tintColor.copy(alpha = if (palette.isDark) 0.68f else 0.76f)
        elevated -> palette.glassElevatedTop
        else -> palette.glassSurfaceTop
    }
    val bottomFill = when {
        tintColor != null -> tintColor.copy(alpha = if (palette.isDark) 0.54f else 0.62f)
        elevated -> palette.glassElevatedBottom
        else -> palette.glassSurfaceBottom
    }

    return this
        .drawWithCache {
            val radiusPx = cornerRadius.toPx()
            val elevPx = if (elevated) shadowElevation.toPx() else (shadowElevation * 0.70f).toPx()
            val shadowBaseAlpha = if (palette.isDark) {
                if (elevated) 0.42f else 0.28f
            } else {
                if (elevated) 0.16f else 0.11f
            }

            onDrawBehind {
                if (elevPx > 0.5f) {
                    drawRoundRect(
                        color = palette.ambientShadowColor.copy(alpha = shadowBaseAlpha * 0.38f),
                        topLeft = Offset(-2.5.dp.toPx(), elevPx * 0.70f),
                        size = Size(size.width + 5.dp.toPx(), size.height + 4.dp.toPx()),
                        cornerRadius = CornerRadius(radiusPx + 3.dp.toPx(), radiusPx + 3.dp.toPx())
                    )
                    drawRoundRect(
                        color = palette.ambientShadowColor.copy(alpha = shadowBaseAlpha * 0.60f),
                        topLeft = Offset(-1.dp.toPx(), elevPx * 0.45f),
                        size = Size(size.width + 2.dp.toPx(), size.height + 2.dp.toPx()),
                        cornerRadius = CornerRadius(radiusPx + 1.5.dp.toPx(), radiusPx + 1.5.dp.toPx())
                    )
                    drawRoundRect(
                        color = palette.ambientShadowColor.copy(alpha = shadowBaseAlpha * 0.85f),
                        topLeft = Offset(0f, elevPx * 0.22f),
                        size = size,
                        cornerRadius = CornerRadius(radiusPx, radiusPx)
                    )
                }
            }
        }
        .clip(shape)
        .drawWithCache {
            val radiusPx = cornerRadius.toPx()
            val strokePx = strokeWidth.toPx()
            val w = size.width.coerceAtLeast(1f)
            val h = size.height.coerceAtLeast(1f)

            val agslLensBrush: Brush? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                LiquidGlassShaderEngine.createConfiguredLensBrush(
                    width = w,
                    height = h,
                    cornerRadiusPx = radiusPx,
                    isDark = palette.isDark,
                    isButton = false,
                    topColor = topFill,
                    bottomColor = bottomFill
                )
            } else {
                null
            }

            val bodyBrush: Brush = agslLensBrush ?: Brush.verticalGradient(
                colors = listOf(topFill, bottomFill)
            )

            val sheenBrush = Brush.linearGradient(
                colors = listOf(
                    palette.diagonalSheenColor,
                    Color.Transparent,
                    palette.diagonalSheenColor.copy(alpha = palette.diagonalSheenColor.alpha * 0.55f)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            val meniscusBrush = Brush.verticalGradient(
                colors = listOf(
                    palette.innerMeniscusGlow,
                    palette.innerMeniscusGlow.copy(alpha = palette.innerMeniscusGlow.alpha * 0.28f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = (h * 0.36f).coerceAtLeast(18f)
            )

            val sdfInset = 3.8.dp.toPx()
            val sdfStroke = 3.0.dp.toPx()
            val sdfBrush = Brush.linearGradient(
                colors = listOf(
                    palette.sdfRefractionRingTop,
                    palette.dispersionCyan.copy(alpha = 0.20f),
                    Color.Transparent,
                    palette.dispersionEmerald.copy(alpha = 0.22f),
                    palette.sdfRefractionRingBottom
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            val dispInset = 1.8.dp.toPx()
            val dispStroke = 1.6.dp.toPx()
            val dispersionBrush = Brush.linearGradient(
                colors = listOf(
                    palette.dispersionCoralRose,
                    palette.dispersionGold,
                    palette.dispersionMagenta,
                    palette.dispersionCyan,
                    palette.dispersionEmerald
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            val crescentInset = 5.5.dp.toPx()
            val crescentStroke = 1.3.dp.toPx()
            val crescentBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (palette.isDark) 0.48f else 0.74f),
                    Color.Transparent,
                    Color.Transparent,
                    palette.dispersionCyan.copy(alpha = if (palette.isDark) 0.38f else 0.50f)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            val rimBrush = Brush.linearGradient(
                colors = listOf(
                    palette.specularRimBright,
                    palette.specularRimMid,
                    palette.specularRimBright.copy(alpha = 0.55f),
                    palette.specularRimMid,
                    palette.specularRimEnd
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            onDrawWithContent {
                drawRoundRect(
                    brush = bodyBrush,
                    cornerRadius = CornerRadius(radiusPx, radiusPx)
                )
                drawRoundRect(
                    brush = sheenBrush,
                    cornerRadius = CornerRadius(radiusPx, radiusPx)
                )
                drawRoundRect(
                    brush = meniscusBrush,
                    cornerRadius = CornerRadius(radiusPx, radiusPx)
                )
                if (w > sdfInset * 4 && h > sdfInset * 4) {
                    val innerR = (radiusPx - sdfInset).coerceAtLeast(2f)
                    drawRoundRect(
                        brush = sdfBrush,
                        topLeft = Offset(sdfInset, sdfInset),
                        size = Size(w - sdfInset * 2, h - sdfInset * 2),
                        cornerRadius = CornerRadius(innerR, innerR),
                        style = Stroke(width = sdfStroke)
                    )
                }
                if (w > crescentInset * 4 && h > crescentInset * 4) {
                    val crescR = (radiusPx - crescentInset).coerceAtLeast(2f)
                    drawRoundRect(
                        brush = crescentBrush,
                        topLeft = Offset(crescentInset, crescentInset),
                        size = Size(w - crescentInset * 2, h - crescentInset * 2),
                        cornerRadius = CornerRadius(crescR, crescR),
                        style = Stroke(width = crescentStroke)
                    )
                }

                // Draw child composable content cleanly over the glass base
                drawContent()

                if (w > dispInset * 2 && h > dispInset * 2) {
                    val dispR = (radiusPx - dispInset).coerceAtLeast(2f)
                    drawRoundRect(
                        brush = dispersionBrush,
                        topLeft = Offset(dispInset, dispInset),
                        size = Size(w - dispInset * 2, h - dispInset * 2),
                        cornerRadius = CornerRadius(dispR, dispR),
                        style = Stroke(width = dispStroke)
                    )
                }

                drawRoundRect(
                    color = palette.outerContrastRim,
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    style = Stroke(width = (strokePx + 0.8.dp.toPx()))
                )

                drawRoundRect(
                    brush = rimBrush,
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    style = Stroke(width = strokePx)
                )
            }
        }
}

/**
 * Translucent Inner Glass Tile modifier for nested items inside a `LiquidGlassCard`.
 */
@Composable
fun Modifier.liquidInnerGlass(
    shape: Shape = RoundedCornerShape(14.dp),
    cornerRadius: Dp = 14.dp,
    selected: Boolean = false,
    accentColor: Color? = null,
    tint: Color? = null,
    strokeWidth: Dp = 1.2.dp
): Modifier {
    val palette = LocalLiquidGlassPalette.current
    val primary = MaterialTheme.colorScheme.primary
    val effectiveAccent = tint ?: accentColor

    val topColor = when {
        effectiveAccent != null -> if (palette.isDark) {
            effectiveAccent.copy(alpha = 0.22f)
        } else {
            effectiveAccent.copy(alpha = 0.14f)
        }
        selected -> palette.innerTileSelectedTop
        else -> palette.innerTileTop
    }
    val bottomColor = when {
        effectiveAccent != null -> if (palette.isDark) {
            effectiveAccent.copy(alpha = 0.12f)
        } else {
            effectiveAccent.copy(alpha = 0.08f)
        }
        selected -> palette.innerTileSelectedBottom
        else -> palette.innerTileBottom
    }
    val borderTop = when {
        effectiveAccent != null -> effectiveAccent.copy(alpha = if (palette.isDark) 0.74f else 0.60f)
        selected -> primary.copy(alpha = 0.90f)
        else -> palette.specularRimBright.copy(alpha = if (palette.isDark) 0.56f else 0.90f)
    }
    val borderMid = when {
        effectiveAccent != null -> effectiveAccent.copy(alpha = 0.44f)
        selected -> palette.dispersionCyan.copy(alpha = 0.75f)
        else -> palette.dispersionCyan.copy(alpha = 0.35f)
    }
    val borderBottom = when {
        effectiveAccent != null -> palette.dispersionEmerald.copy(alpha = 0.40f)
        selected -> palette.dispersionEmerald.copy(alpha = 0.70f)
        else -> palette.outerContrastRim
    }

    return this
        .clip(shape)
        .drawWithCache {
            val r = cornerRadius.toPx()
            val w = size.width.coerceAtLeast(1f)
            val h = size.height.coerceAtLeast(1f)

            val fillBrush = Brush.verticalGradient(colors = listOf(topColor, bottomColor))
            val meniscus = Brush.verticalGradient(
                colors = listOf(
                    palette.innerMeniscusGlow.copy(alpha = palette.innerMeniscusGlow.alpha * 0.65f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = (h * 0.38f).coerceAtLeast(12f)
            )
            val sdfInset = 2.2.dp.toPx()
            val sdfBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (palette.isDark) 0.22f else 0.50f),
                    Color.Transparent,
                    borderMid.copy(alpha = 0.25f)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
            val borderBrush = Brush.linearGradient(
                colors = listOf(borderTop, borderMid, borderBottom),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            onDrawWithContent {
                drawRoundRect(brush = fillBrush, cornerRadius = CornerRadius(r, r))
                drawRoundRect(brush = meniscus, cornerRadius = CornerRadius(r, r))
                if (w > sdfInset * 4 && h > sdfInset * 4) {
                    val innerR = (r - sdfInset).coerceAtLeast(2f)
                    drawRoundRect(
                        brush = sdfBrush,
                        topLeft = Offset(sdfInset, sdfInset),
                        size = Size(w - sdfInset * 2, h - sdfInset * 2),
                        cornerRadius = CornerRadius(innerR, innerR),
                        style = Stroke(width = 1.4.dp.toPx())
                    )
                }
                drawContent()
                drawRoundRect(
                    brush = borderBrush,
                    cornerRadius = CornerRadius(r, r),
                    style = Stroke(width = strokeWidth.toPx())
                )
            }
        }
}

/**
 * QWEA0/Liquid-Glass-Android 3D Convex Water-Droplet Button Surface Modifier.
 *
 * Draws the 3D convex droplet depth and meniscus gloss *behind* `drawContent()` so
 * button text and icons remain 100% crisp, bright, and high-contrast in both Light & Dark modes.
 */
@Composable
fun Modifier.liquidButtonGlass(
    shape: Shape = RoundedCornerShape(14.dp),
    cornerRadius: Dp = 14.dp,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    baseColor: Color? = null,
    enabled: Boolean = true
): Modifier {
    val palette = LocalLiquidGlassPalette.current
    val rawColor = baseColor ?: accentColor
    // Ensure button fill is rich enough in Dark Mode so crisp white text is always 100% legible
    val effectiveColor = if (palette.isDark && rawColor == MaterialTheme.colorScheme.primary) {
        Color(0xFF0277BD)
    } else if (palette.isDark && rawColor == MaterialTheme.colorScheme.secondary) {
        Color(0xFF00897B)
    } else {
        rawColor
    }

    return this
        .liquidPressEffect(enabled = enabled)
        .drawWithCache {
            val r = cornerRadius.toPx()
            val w = size.width.coerceAtLeast(1f)
            val h = size.height.coerceAtLeast(1f)

            val shadowAlpha = if (!enabled) 0f else if (palette.isDark) 0.42f else 0.25f

            val topBtnColor = effectiveColor.copy(alpha = if (enabled) 0.95f else 0.48f)
            val bottomBtnColor = Color(
                red = (effectiveColor.red * 0.76f).coerceIn(0f, 1f),
                green = (effectiveColor.green * 0.82f).coerceIn(0f, 1f),
                blue = (effectiveColor.blue * 0.98f).coerceIn(0f, 1f),
                alpha = if (enabled) 0.98f else 0.48f
            )

            val agslBtnBrush: Brush? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                LiquidGlassShaderEngine.createConfiguredLensBrush(
                    width = w,
                    height = h,
                    cornerRadiusPx = r,
                    isDark = palette.isDark,
                    isButton = true,
                    topColor = topBtnColor,
                    bottomColor = bottomBtnColor
                )
            } else {
                null
            }
            val baseBtnBrush = agslBtnBrush ?: Brush.verticalGradient(
                colors = listOf(topBtnColor, bottomBtnColor)
            )

            val dropletDepthBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF031236).copy(alpha = if (enabled) 0.24f else 0.08f),
                    Color.Transparent,
                    Color(0xFF38BDF8).copy(alpha = if (enabled) 0.22f else 0.06f)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            val topGloss = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (enabled) 0.30f else 0.10f),
                    Color.White.copy(alpha = if (enabled) 0.06f else 0.02f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = (h * 0.44f).coerceAtLeast(10f)
            )

            val innerInset = 2.4.dp.toPx()
            val innerCrescentBrush = Brush.linearGradient(
                colors = listOf(
                    palette.dispersionCoralRose.copy(alpha = if (enabled) 0.44f else 0.12f),
                    Color.White.copy(alpha = if (enabled) 0.50f else 0.15f),
                    palette.dispersionCyan.copy(alpha = if (enabled) 0.50f else 0.15f),
                    palette.dispersionEmerald.copy(alpha = if (enabled) 0.44f else 0.12f)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            val rimBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (enabled) 0.92f else 0.25f),
                    Color(0xFF7DD3FC).copy(alpha = if (enabled) 0.85f else 0.20f),
                    Color(0xFF38BDF8).copy(alpha = if (enabled) 0.78f else 0.20f),
                    Color.White.copy(alpha = if (enabled) 0.88f else 0.20f)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            onDrawWithContent {
                if (shadowAlpha > 0f) {
                    drawRoundRect(
                        color = effectiveColor.copy(alpha = shadowAlpha * 0.50f),
                        topLeft = Offset(-1.dp.toPx(), 3.5.dp.toPx()),
                        size = Size(w + 2.dp.toPx(), h + 1.dp.toPx()),
                        cornerRadius = CornerRadius(r + 1.dp.toPx(), r + 1.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color.Black.copy(alpha = shadowAlpha * 0.40f),
                        topLeft = Offset(0f, 2.dp.toPx()),
                        size = Size(w, h),
                        cornerRadius = CornerRadius(r, r)
                    )
                }
                // 1. Draw 3D convex button base fill & water-droplet shading BEHIND text
                drawRoundRect(
                    brush = baseBtnBrush,
                    cornerRadius = CornerRadius(r, r)
                )
                drawRoundRect(
                    brush = dropletDepthBrush,
                    cornerRadius = CornerRadius(r, r)
                )
                drawRoundRect(
                    brush = topGloss,
                    cornerRadius = CornerRadius(r, r)
                )
                // 2. Draw crisp button text and icons on top of the lens background
                drawContent()
                // 3. Draw inner crescent rim and outer specular border
                if (w > innerInset * 4 && h > innerInset * 4) {
                    val innerR = (r - innerInset).coerceAtLeast(2f)
                    drawRoundRect(
                        brush = innerCrescentBrush,
                        topLeft = Offset(innerInset, innerInset),
                        size = Size(w - innerInset * 2, h - innerInset * 2),
                        cornerRadius = CornerRadius(innerR, innerR),
                        style = Stroke(width = 1.3.dp.toPx())
                    )
                }
                drawRoundRect(
                    brush = rimBrush,
                    cornerRadius = CornerRadius(r, r),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }
}

/**
 * Self-contained Liquid Glass Modal surface for Android `Dialog` windows.
 * Includes a spring pop-in entrance animation.
 */
@Composable
fun Modifier.liquidModalGlass(
    shape: Shape = RoundedCornerShape(28.dp),
    cornerRadius: Dp = 28.dp,
    strokeWidth: Dp = 1.8.dp
): Modifier {
    val palette = LocalLiquidGlassPalette.current
    return this
        .liquidAppearAnimation(initialScale = 0.92f, initialOffsetYDp = 18f)
        .clip(shape)
        .drawWithCache {
            val w = size.width.coerceAtLeast(1f)
            val h = size.height.coerceAtLeast(1f)
            val minDim = size.minDimension.coerceAtLeast(1f)
            val radiusPx = cornerRadius.toPx()

            val modalBaseBrush = Brush.linearGradient(
                colors = if (palette.isDark) {
                    listOf(Color(0xFF071326), Color(0xFF0E2040), Color(0xFF08142A))
                } else {
                    listOf(Color(0xFFF0F8FF), Color(0xFFF5F8FF), Color(0xFFF0FDFA))
                },
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            val orb1 = Brush.radialGradient(
                colors = listOf(palette.orbCyan.copy(alpha = 0.24f), Color.Transparent),
                center = Offset(w * 0.18f, h * 0.15f),
                radius = minDim * 0.72f
            )
            val orb2 = Brush.radialGradient(
                colors = listOf(palette.orbViolet.copy(alpha = 0.22f), Color.Transparent),
                center = Offset(w * 0.85f, h * 0.80f),
                radius = minDim * 0.72f
            )

            val meniscusBrush = Brush.verticalGradient(
                colors = listOf(palette.innerMeniscusGlow, Color.Transparent),
                startY = 0f,
                endY = (h * 0.28f).coerceAtLeast(24f)
            )

            val sdfInset = 3.2.dp.toPx()
            val sdfBrush = Brush.linearGradient(
                colors = listOf(
                    palette.sdfRefractionRingTop,
                    Color.Transparent,
                    palette.sdfRefractionRingBottom
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            val rimBrush = Brush.linearGradient(
                colors = listOf(
                    palette.specularRimBright,
                    palette.dispersionCyan,
                    palette.specularRimMid,
                    palette.dispersionMagenta,
                    palette.specularRimEnd
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            onDrawWithContent {
                drawRoundRect(brush = modalBaseBrush, cornerRadius = CornerRadius(radiusPx, radiusPx))
                drawCircle(brush = orb1, radius = minDim * 0.72f, center = Offset(w * 0.18f, h * 0.15f))
                drawCircle(brush = orb2, radius = minDim * 0.72f, center = Offset(w * 0.85f, h * 0.80f))
                drawRoundRect(brush = meniscusBrush, cornerRadius = CornerRadius(radiusPx, radiusPx))
                if (w > sdfInset * 4 && h > sdfInset * 4) {
                    val innerR = (radiusPx - sdfInset).coerceAtLeast(2f)
                    drawRoundRect(
                        brush = sdfBrush,
                        topLeft = Offset(sdfInset, sdfInset),
                        size = Size(w - sdfInset * 2, h - sdfInset * 2),
                        cornerRadius = CornerRadius(innerR, innerR),
                        style = Stroke(width = 2.2.dp.toPx())
                    )
                }
                drawContent()
                drawRoundRect(
                    brush = rimBrush,
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    style = Stroke(width = strokeWidth.toPx())
                )
            }
        }
}

/**
 * Deep Sapphire-Cyan Refractive Hero Glass Modifier for corporate headers & badges.
 * Guarantees crisp white text & logos pop with 100% legibility in both Light and Dark modes.
 */
@Composable
fun Modifier.liquidHeroGlass(
    shape: Shape = RoundedCornerShape(28.dp),
    cornerRadius: Dp = 28.dp
): Modifier {
    val palette = LocalLiquidGlassPalette.current
    return this
        .drawWithCache {
            val r = cornerRadius.toPx()
            onDrawBehind {
                drawRoundRect(
                    color = Color(0xFF0284C7).copy(alpha = if (palette.isDark) 0.32f else 0.22f),
                    topLeft = Offset(0f, 4.dp.toPx()),
                    size = size,
                    cornerRadius = CornerRadius(r, r)
                )
            }
        }
        .clip(shape)
        .drawWithCache {
            val w = size.width.coerceAtLeast(1f)
            val h = size.height.coerceAtLeast(1f)
            val radiusPx = cornerRadius.toPx()

            val heroBodyBrush = Brush.linearGradient(
                colors = if (palette.isDark) {
                    listOf(
                        Color(0xFF061B36).copy(alpha = 0.94f),
                        Color(0xFF034470).copy(alpha = 0.90f),
                        Color(0xFF1A1846).copy(alpha = 0.92f)
                    )
                } else {
                    listOf(
                        Color(0xFF072B47).copy(alpha = 0.95f),
                        Color(0xFF02598A).copy(alpha = 0.92f),
                        Color(0xFF0369A1).copy(alpha = 0.94f)
                    )
                },
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            val heroOrbBrush = Brush.radialGradient(
                colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.36f), Color.Transparent),
                center = Offset(w * 0.85f, h * 0.20f),
                radius = w * 0.58f
            )

            val topMeniscus = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
                startY = 0f,
                endY = (h * 0.42f).coerceAtLeast(18f)
            )

            val sdfInset = 3.dp.toPx()
            val sdfBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.38f),
                    Color.Transparent,
                    Color(0xFF38BDF8).copy(alpha = 0.35f)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            val rimBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.92f),
                    Color(0xFF38BDF8).copy(alpha = 0.85f),
                    Color(0xFFA855F7).copy(alpha = 0.60f),
                    Color(0xFF7DD3FC).copy(alpha = 0.90f)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            onDrawWithContent {
                drawRoundRect(brush = heroBodyBrush, cornerRadius = CornerRadius(radiusPx, radiusPx))
                drawCircle(brush = heroOrbBrush, radius = w * 0.58f, center = Offset(w * 0.85f, h * 0.20f))
                drawRoundRect(brush = topMeniscus, cornerRadius = CornerRadius(radiusPx, radiusPx))
                if (w > sdfInset * 4 && h > sdfInset * 4) {
                    val innerR = (radiusPx - sdfInset).coerceAtLeast(2f)
                    drawRoundRect(
                        brush = sdfBrush,
                        topLeft = Offset(sdfInset, sdfInset),
                        size = Size(w - sdfInset * 2, h - sdfInset * 2),
                        cornerRadius = CornerRadius(innerR, innerR),
                        style = Stroke(width = 2.0.dp.toPx())
                    )
                }
                drawContent()
                drawRoundRect(
                    brush = rimBrush,
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    style = Stroke(width = 1.6.dp.toPx())
                )
            }
        }
}

/**
 * Responsive, Non-Blocking iOS 26 Tactile Press & Specular Flare Modifier.
 */
@Composable
fun Modifier.liquidPressEffect(
    pressedScale: Float = 0.968f,
    enabled: Boolean = true
): Modifier {
    if (!enabled) return this
    val palette = LocalLiquidGlassPalette.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "liquidPressScale"
    )
    val flareAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = tween(durationMillis = if (isPressed) 90 else 220),
        label = "liquidPressFlare"
    )

    return this
        .pointerInput(enabled) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                isPressed = true
                waitForUpOrCancellation(pass = PointerEventPass.Initial)
                isPressed = false
            }
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .drawWithCache {
            val w = size.width.coerceAtLeast(1f)
            val h = size.height.coerceAtLeast(1f)
            val flareBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.28f),
                    palette.pressFlareColor,
                    Color.Transparent
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
            onDrawWithContent {
                drawContent()
                if (flareAlpha > 0.01f) {
                    drawRoundRect(
                        brush = flareBrush,
                        alpha = flareAlpha,
                        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                    )
                }
            }
        }
}

/**
 * High-level QWEA0 / iOS 26 Liquid Glass Card container.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    elevated: Boolean = false,
    tintColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val baseMod = if (onClick != null) {
        modifier.liquidPressEffect(pressedScale = 0.98f)
    } else {
        modifier
    }
    val glassMod = baseMod.liquidGlass(
        shape = shape,
        cornerRadius = cornerRadius,
        elevated = elevated,
        tintColor = tintColor
    )

    Column(
        modifier = if (onClick != null) {
            glassMod.clickable(onClick = onClick)
        } else {
            glassMod
        },
        content = content
    )
}

/**
 * Translucent Liquid Glass OutlinedTextField colors for crisp readability in Light & Dark modes.
 */
@Composable
fun liquidTextFieldColors(): TextFieldColors {
    val palette = LocalLiquidGlassPalette.current
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = palette.inputFieldContainer,
        unfocusedContainerColor = palette.inputFieldContainer.copy(alpha = palette.inputFieldContainer.alpha * 0.92f),
        disabledContainerColor = palette.inputFieldContainer.copy(alpha = 0.50f),
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = palette.inputFieldBorderUnfocused,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
        focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
        unfocusedLeadingIconColor = MaterialTheme.colorScheme.primary,
        disabledLeadingIconColor = MaterialTheme.colorScheme.primary,
        focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = MaterialTheme.colorScheme.primary
    )
}

/**
 * Translucent Liquid Glass FilterChip colors for sleek capsule chips across all screens.
 */
@Composable
fun liquidFilterChipColors(
    useSecondaryAccent: Boolean = false
): SelectableChipColors {
    val palette = LocalLiquidGlassPalette.current
    val selectedBg = if (useSecondaryAccent) {
        if (palette.isDark) Color(0xFF00897B) else MaterialTheme.colorScheme.secondary
    } else {
        if (palette.isDark) Color(0xFF0277BD) else MaterialTheme.colorScheme.primary
    }
    return FilterChipDefaults.filterChipColors(
        containerColor = palette.chipUnselectedContainer,
        labelColor = MaterialTheme.colorScheme.onSurface,
        iconColor = MaterialTheme.colorScheme.primary,
        selectedContainerColor = selectedBg,
        selectedLabelColor = Color.White,
        selectedLeadingIconColor = Color.White,
        disabledContainerColor = palette.chipUnselectedContainer.copy(alpha = 0.4f),
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    )
}

/**
 * Cohesive Liquid Glass Switch colors for Admin & Kiosk toggles.
 */
@Composable
fun liquidSwitchColors(): SwitchColors {
    val palette = LocalLiquidGlassPalette.current
    return SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = if (palette.isDark) Color(0xFF0284C7) else MaterialTheme.colorScheme.primary,
        checkedBorderColor = palette.pillActiveBorder,
        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
        uncheckedTrackColor = palette.chipUnselectedContainer,
        uncheckedBorderColor = palette.inputFieldBorderUnfocused
    )
}

/**
 * Cohesive Liquid Glass Primary Button colors.
 */
@Composable
fun liquidPrimaryButtonColors(
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = Color.White
): ButtonColors {
    return ButtonDefaults.buttonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = containerColor.copy(alpha = 0.42f),
        disabledContentColor = contentColor.copy(alpha = 0.72f)
    )
}

/**
 * iOS 26 Segmented Liquid Glass Capsule Selector (e.g. Walk-In vs Appointment).
 * Uses comfortable inner padding and spacing so segments never touch bounds or cause mistouches.
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
            .padding(horizontal = 2.dp)
            .liquidGlass(
                shape = RoundedCornerShape(18.dp),
                cornerRadius = 18.dp,
                elevated = false
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.0f else 0.97f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "segmentScale_$option"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                animationSpec = tween(180),
                label = "segmentText_$option"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .liquidPressEffect(pressedScale = 0.95f)
                    .clip(RoundedCornerShape(13.dp))
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
                                    val r = 13.dp.toPx()
                                    val topGloss = Brush.verticalGradient(
                                        colors = listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
                                        startY = 0f,
                                        endY = size.height * 0.5f
                                    )
                                    onDrawWithContent {
                                        drawContent()
                                        drawRoundRect(
                                            brush = topGloss,
                                            cornerRadius = CornerRadius(r, r)
                                        )
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
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
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
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
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
 * Floating iOS 26 Liquid Glass Bottom Capsule Dock with comfortable spacing and margins
 * so tab buttons never touch the dock bounds or cause mistouches.
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
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .testTag("bottom_nav_bar"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(
                    shape = RoundedCornerShape(30.dp),
                    cornerRadius = 30.dp,
                    elevated = true,
                    strokeWidth = 1.6.dp,
                    shadowElevation = 12.dp
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = selectedTab == item.index
                val contentColor by animateColorAsState(
                    targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                    animationSpec = tween(200),
                    label = "dockColor_${item.index}"
                )
                val tabScale by animateFloatAsState(
                    targetValue = if (selected) 1.0f else 0.96f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "dockScale_${item.index}"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .graphicsLayer {
                            scaleX = tabScale
                            scaleY = tabScale
                        }
                        .liquidPressEffect(pressedScale = 0.93f)
                        .clip(RoundedCornerShape(22.dp))
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
                                        val r = 22.dp.toPx()
                                        val topGloss = Brush.verticalGradient(
                                            colors = listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
                                            startY = 0f,
                                            endY = size.height * 0.5f
                                        )
                                        onDrawWithContent {
                                            drawContent()
                                            drawRoundRect(
                                                brush = topGloss,
                                                cornerRadius = CornerRadius(r, r)
                                            )
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
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(width = 44.dp, height = 22.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            if (item.badgeCount > 0) {
                                val badgeBg = if (item.badgeIsWarning) StatusWarning else MaterialTheme.colorScheme.secondary
                                val badgeFg = if (item.badgeIsWarning) Color(0xFF051020) else Color.White
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-3).dp)
                                        .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                                        .background(
                                            color = badgeBg,
                                            shape = RoundedCornerShape(9.dp)
                                        )
                                        .drawWithCache {
                                            val r = size.height / 2f
                                            onDrawWithContent {
                                                drawContent()
                                                drawRoundRect(
                                                    color = Color.White.copy(alpha = 0.90f),
                                                    cornerRadius = CornerRadius(r, r),
                                                    style = Stroke(width = 0.9.dp.toPx())
                                                )
                                            }
                                        }
                                        .padding(horizontal = 4.5.dp, vertical = 1.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (item.badgeCount > 99) "99+" else item.badgeCount.toString(),
                                        color = badgeFg,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            color = contentColor,
                            fontSize = 10.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
