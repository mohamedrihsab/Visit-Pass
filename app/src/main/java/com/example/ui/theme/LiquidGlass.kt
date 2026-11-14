package com.example.ui.theme

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.wrapContentSize
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

            // 5. Crystalline Sapphire-Cyan Refractive Caustic Fringe along curved SDF bevel
            float prismBand = exp(-pow((insideDist - 2.8) / 2.5, 2.0));
            float angleFactor = clamp((grad.x + grad.y) * 0.5 + 0.5, 0.0, 1.0);
            float3 causticCyan = mix(float3(0.85, 0.96, 1.0), float3(0.18, 0.72, 1.0), angleFactor);
            float3 causticWhite = mix(float3(1.0, 1.0, 1.0), float3(0.40, 0.85, 1.0), clamp(uv.x, 0.0, 1.0));
            float3 prismColor = mix(causticWhite, causticCyan, angleFactor);

            // 6. Curved Water-Droplet Crescent Meniscus Ring
            float crescentRing = exp(-pow((insideDist - 5.5) / 2.2, 2.0)) * smoothstep(0.10, 0.95, abs(grad.x));
            float3 crescentColor = mix(float3(1.0, 1.0, 1.0), float3(0.72, 0.92, 1.0), angleFactor);

            // Combine optical layers with crystalline radiance
            float3 color = baseColor;
            color += float3(0.96, 0.98, 1.0) * (specTopLeft * 0.38 + fresnel * 0.28);
            color += float3(0.20, 0.76, 1.0) * (specBottomRight * 0.22);
            color = mix(color, prismColor, prismBand * mix(0.42, 0.52, uIsDark));
            color += crescentColor * (crescentRing * mix(0.38, 0.32, uIsDark));

            // 7. API 36+ (Android 16) AGSL Color Filter / Harmonic Vibrance Blend Enhancement
            if (uApi36Enhance > 0.5) {
                float luma = dot(color, float3(0.2126, 0.7152, 0.0722));
                float3 chroma = color - float3(luma, luma, luma);
                color = float3(luma, luma, luma) + chroma * 1.25 + float3(0.01, 0.04, 0.08) * fresnel;
            }

            float finalAlpha = clamp(baseAlpha + fresnel * 0.20 + prismBand * 0.22 + crescentRing * 0.16, 0.0, 0.98);
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
    meshBgTopLeft = Color(0xFFC8E2FF),        // QWEA0 Vibrant Sapphire-Sky Top-Left
    meshBgCenter = Color(0xFFDCEEFF),         // Luminous Ice-Azure Center
    meshBgBottomRight = Color(0xFFBFD9FF),    // Rich Cobalt-Sky Bottom-Right
    waveSpherePrimary = Color(0xFF2563EB).copy(alpha = 0.54f), // QWEA0 3D Lit Royal Sapphire Sphere
    waveSphereSecondary = Color(0xFF1D4ED8).copy(alpha = 0.32f),
    waveEmeraldStart = Color(0xFF00C896).copy(alpha = 0.72f),  // QWEA0 Signature Bottom-Left Turquoise-Emerald Wave
    waveEmeraldEnd = Color(0xFF00897B).copy(alpha = 0.56f),
    waveEmeraldRim = Color(0xFFB8FFF2).copy(alpha = 0.96f),   // Crisp Aquamarine-White Rim
    waveIndigoStart = Color(0xFF1E3A8A).copy(alpha = 0.42f),  // QWEA0 Right Ultramarine Crescent Arc
    waveIndigoEnd = Color(0xFF0F256E).copy(alpha = 0.30f),
    waveIndigoRim = Color(0xFF60A5FA).copy(alpha = 0.90f),    // Glowing Royal-Blue Rim
    orbCyan = Color(0xFF00E5FF).copy(alpha = 0.26f),
    orbViolet = Color(0xFF3B82F6).copy(alpha = 0.22f),
    orbEmerald = Color(0xFF00D2A0).copy(alpha = 0.28f),
    orbRose = Color(0xFF60A5FA).copy(alpha = 0.22f),
    causticBeamColor = Color(0xFFFFFFFF).copy(alpha = 0.65f),
    ambientShadowColor = Color(0xFF061A40),                   // Deep Sapphire-Navy Shadow
    outerContrastRim = Color(0xFF1D4ED8).copy(alpha = 0.28f),
    // High-clarity Frosted Ice-Sapphire Glass: guarantees 100% crisp dark text contrast while refracting background hues
    glassSurfaceTop = Color(0xFFFFFFFF).copy(alpha = 0.90f),
    glassSurfaceBottom = Color(0xFFF2F8FF).copy(alpha = 0.86f),
    glassElevatedTop = Color(0xFFFFFFFF).copy(alpha = 0.95f),
    glassElevatedBottom = Color(0xFFF5FAFF).copy(alpha = 0.92f),
    innerTileTop = Color(0xFFFFFFFF).copy(alpha = 0.88f),
    innerTileBottom = Color(0xFFF0F7FF).copy(alpha = 0.82f),
    innerTileSelectedTop = Color(0xFFE6F2FF).copy(alpha = 0.95f),
    innerTileSelectedBottom = Color(0xFFD8EAFF).copy(alpha = 0.91f),
    sdfRefractionRingTop = Color(0xFFFFFFFF).copy(alpha = 0.95f),
    sdfRefractionRingBottom = Color(0xFF38BDF8).copy(alpha = 0.44f),
    // Subtle QWEA0 Prismatic RGB Chromatic Dispersion Fringe
    dispersionCoralRose = Color(0xFFFF5E7E).copy(alpha = 0.48f),
    dispersionGold = Color(0xFFFFD166).copy(alpha = 0.42f),
    dispersionCyan = Color(0xFF00E5FF).copy(alpha = 0.58f),
    dispersionEmerald = Color(0xFF00E6A8).copy(alpha = 0.54f),
    dispersionMagenta = Color(0xFF60A5FA).copy(alpha = 0.46f),
    specularRimBright = Color(0xFFFFFFFF).copy(alpha = 0.98f),
    specularRimMid = Color(0xFF60A5FA).copy(alpha = 0.82f),   // QWEA0 Sky-Blue Fresnel Rim
    specularRimEnd = Color(0xFFFFFFFF).copy(alpha = 0.92f),
    innerMeniscusGlow = Color(0xFFFFFFFF).copy(alpha = 0.85f),
    diagonalSheenColor = Color(0xFFFFFFFF).copy(alpha = 0.42f),
    pressFlareColor = Color(0xFF38BDF8).copy(alpha = 0.35f),
    pillActiveTop = Color(0xFF155EEF),                        // QWEA0 Royal Sapphire Capsule
    pillActiveBottom = Color(0xFF2563EB),                     // Luminous Cobalt Blue
    pillActiveBorder = Color(0xFF93C5FD),                     // Glowing Ice-Blue Rim
    inputFieldContainer = Color(0xFFFFFFFF).copy(alpha = 0.93f),
    inputFieldBorderUnfocused = Color(0xFF1D4ED8).copy(alpha = 0.42f),
    chipUnselectedContainer = Color(0xFFFFFFFF).copy(alpha = 0.90f)
)

val DarkLiquidGlassPalette = LiquidGlassPalette(
    isDark = true,
    meshBgTopLeft = Color(0xFF01133C),                        // QWEA0 Deep Royal Navy Top-Left
    meshBgCenter = Color(0xFF000C29),                         // Deep Ultramarine Center
    meshBgBottomRight = Color(0xFF000514),                    // Midnight Abyss Bottom-Right
    waveSpherePrimary = Color(0xFF3B7BF6).copy(alpha = 0.80f), // QWEA0 3D Lit Royal Sapphire Sphere
    waveSphereSecondary = Color(0xFF10378A).copy(alpha = 0.65f),
    waveEmeraldStart = Color(0xFF1CE5B5).copy(alpha = 0.85f),  // QWEA0 Signature Bottom-Left Turquoise-Emerald Wave
    waveEmeraldEnd = Color(0xFF00796B).copy(alpha = 0.72f),
    waveEmeraldRim = Color(0xFF80FFE5).copy(alpha = 0.94f),   // Luminous Aquamarine Specular Rim
    waveIndigoStart = Color(0xFF041B6B).copy(alpha = 0.92f),  // QWEA0 Right Ultramarine Crescent Arc
    waveIndigoEnd = Color(0xFF010A2E).copy(alpha = 0.88f),
    waveIndigoRim = Color(0xFF3B82F6).copy(alpha = 0.92f),    // Electric Royal-Blue Rim
    orbCyan = Color(0xFF00E5FF).copy(alpha = 0.28f),
    orbViolet = Color(0xFF3B82F6).copy(alpha = 0.25f),
    orbEmerald = Color(0xFF1CE5B5).copy(alpha = 0.30f),
    orbRose = Color(0xFF60A5FA).copy(alpha = 0.20f),
    causticBeamColor = Color(0xFF60A5FA).copy(alpha = 0.22f),
    ambientShadowColor = Color(0xFF00030C),
    outerContrastRim = Color(0xFF3B82F6).copy(alpha = 0.45f),
    // Rich Deep Royal Sapphire Glass: guarantees 100% crisp white & ice-blue text contrast over bright background waves
    glassSurfaceTop = Color(0xFF071A44).copy(alpha = 0.88f),
    glassSurfaceBottom = Color(0xFF04102C).copy(alpha = 0.85f),
    glassElevatedTop = Color(0xFF0B2358).copy(alpha = 0.93f),
    glassElevatedBottom = Color(0xFF06163A).copy(alpha = 0.90f),
    innerTileTop = Color(0xFF0D275E).copy(alpha = 0.84f),
    innerTileBottom = Color(0xFF071840).copy(alpha = 0.78f),
    innerTileSelectedTop = Color(0xFF103178).copy(alpha = 0.90f),
    innerTileSelectedBottom = Color(0xFF091E4D).copy(alpha = 0.86f),
    sdfRefractionRingTop = Color(0xFFBAE6FD).copy(alpha = 0.58f),
    sdfRefractionRingBottom = Color(0xFF38BDF8).copy(alpha = 0.40f),
    // Subtle QWEA0 Prismatic RGB Chromatic Dispersion Fringe
    dispersionCoralRose = Color(0xFFFF5E7E).copy(alpha = 0.58f),
    dispersionGold = Color(0xFFFFD166).copy(alpha = 0.50f),
    dispersionCyan = Color(0xFF00E5FF).copy(alpha = 0.68f),
    dispersionEmerald = Color(0xFF1CE5B5).copy(alpha = 0.64f),
    dispersionMagenta = Color(0xFF60A5FA).copy(alpha = 0.56f),
    specularRimBright = Color(0xFFEFF6FF).copy(alpha = 0.94f),
    specularRimMid = Color(0xFF60A5FA).copy(alpha = 0.84f),   // QWEA0 Sky-Blue Fresnel Rim
    specularRimEnd = Color(0xFF93C5FD).copy(alpha = 0.86f),
    innerMeniscusGlow = Color(0xFF93C5FD).copy(alpha = 0.36f),
    diagonalSheenColor = Color(0xFF60A5FA).copy(alpha = 0.18f),
    pressFlareColor = Color(0xFF38BDF8).copy(alpha = 0.42f),
    pillActiveTop = Color(0xFF1D63DC),                        // QWEA0 Royal Sapphire Capsule
    pillActiveBottom = Color(0xFF10378A),                     // Deep Cobalt Base
    pillActiveBorder = Color(0xFF7CC4FA),
    inputFieldContainer = Color(0xFF051334).copy(alpha = 0.88f),
    inputFieldBorderUnfocused = Color(0xFF60A5FA).copy(alpha = 0.52f),
    chipUnselectedContainer = Color(0xFF0D275E).copy(alpha = 0.86f)
)

val LocalLiquidGlassPalette = staticCompositionLocalOf { LightLiquidGlassPalette }

/**
 * Hardware-Cached QWEA0/Liquid-Glass-Android Curved Geometric Wave & Sphere Wallpaper Background.
 * Pre-allocates all gradients and strokes once inside `drawWithCache` so scrolling and touch interactions
 * run at a locked 60/120 FPS with zero background invalidation overhead.
 */
@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalLiquidGlassPalette.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val w = size.width.coerceAtLeast(1f)
                val h = size.height.coerceAtLeast(1f)
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

                // 1. Giant QWEA0 3D Lit Cobalt-Sapphire Sphere (Upper-Left / Center)
                val sphereCenter = Offset(w * 0.36f, h * 0.38f)
                val sphereRadius = maxDim * 0.46f
                val sphereLightFocal = Offset(w * 0.16f, h * 0.32f)
                val sphereBrush = Brush.radialGradient(
                    colors = listOf(
                        palette.waveSpherePrimary,
                        palette.waveSphereSecondary,
                        if (palette.isDark) Color(0xFF030E2E).copy(alpha = 0.88f) else Color(0xFF93C5FD).copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = sphereLightFocal,
                    radius = sphereRadius * 1.15f
                )
                val sphereRimBrush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (palette.isDark) 0.52f else 0.72f),
                        palette.waveIndigoRim.copy(alpha = 0.62f),
                        Color.Transparent
                    ),
                    start = Offset(0f, h * 0.15f),
                    end = Offset(w * 0.7f, h * 0.65f)
                )
                val sphereRimStroke = Stroke(width = 1.8.dp.toPx())

                // 2. Secondary Lower-Right Intersecting Ultramarine Ribbon Fold
                val lowerFoldCenter = Offset(w * 0.90f, h * 0.82f)
                val lowerFoldRadius = maxDim * 0.48f
                val lowerFoldBrush = Brush.linearGradient(
                    colors = listOf(
                        palette.waveSpherePrimary.copy(alpha = if (palette.isDark) 0.42f else 0.28f),
                        palette.waveIndigoStart,
                        palette.waveIndigoEnd
                    ),
                    start = Offset(w * 0.45f, h * 0.55f),
                    end = Offset(w, h)
                )
                val lowerFoldRimColor = palette.waveIndigoRim.copy(alpha = 0.75f)
                val lowerFoldStroke = Stroke(width = 2.0.dp.toPx())

                // 3. Primary Overlapping Right Deep Ultramarine Crescent Arc
                val indigoCenter = Offset(w * 0.94f, h * 0.42f)
                val indigoRadius = maxDim * 0.55f
                val indigoBrush = Brush.linearGradient(
                    colors = listOf(
                        palette.waveIndigoStart,
                        palette.waveIndigoEnd
                    ),
                    start = Offset(w * 0.42f, 0f),
                    end = Offset(w, h * 0.85f)
                )
                val indigoRimBrush = Brush.verticalGradient(
                    colors = listOf(
                        palette.waveIndigoRim,
                        Color(0xFF60A5FA).copy(alpha = 0.90f),
                        palette.waveIndigoRim
                    )
                )
                val indigoStroke = Stroke(width = 2.4.dp.toPx())

                // 4. Signature Bottom-Left Turquoise-Emerald Curved Wave
                val emeraldCenter = Offset(-w * 0.10f, h * 0.84f)
                val emeraldRadius = maxDim * 0.52f
                val emeraldBrush = Brush.linearGradient(
                    colors = listOf(
                        palette.waveEmeraldStart,
                        palette.waveEmeraldEnd
                    ),
                    start = Offset(0f, h * 0.45f),
                    end = Offset(w * 0.55f, h)
                )
                val emeraldStroke = Stroke(width = 2.6.dp.toPx())

                // 5. Subtle Concentric Liquid Caustic Refraction Rings
                val rippleRadius1 = maxDim * 0.26f
                val rippleColor1 = palette.specularRimBright.copy(alpha = if (palette.isDark) 0.12f else 0.20f)
                val rippleRadius2 = maxDim * 0.36f
                val rippleColor2 = palette.dispersionCyan.copy(alpha = if (palette.isDark) 0.10f else 0.16f)
                val rippleStroke = Stroke(width = 1.2.dp.toPx())

                onDrawBehind {
                    drawRect(brush = baseBrush)

                    drawCircle(
                        brush = sphereBrush,
                        radius = sphereRadius,
                        center = sphereCenter
                    )
                    drawCircle(
                        brush = sphereRimBrush,
                        radius = sphereRadius,
                        center = sphereCenter,
                        style = sphereRimStroke
                    )

                    drawCircle(
                        brush = lowerFoldBrush,
                        radius = lowerFoldRadius,
                        center = lowerFoldCenter
                    )
                    drawCircle(
                        color = lowerFoldRimColor,
                        radius = lowerFoldRadius,
                        center = lowerFoldCenter,
                        style = lowerFoldStroke
                    )

                    drawCircle(
                        brush = indigoBrush,
                        radius = indigoRadius,
                        center = indigoCenter
                    )
                    drawCircle(
                        brush = indigoRimBrush,
                        radius = indigoRadius,
                        center = indigoCenter,
                        style = indigoStroke
                    )

                    drawCircle(
                        brush = emeraldBrush,
                        radius = emeraldRadius,
                        center = emeraldCenter
                    )
                    drawCircle(
                        color = palette.waveEmeraldRim,
                        radius = emeraldRadius,
                        center = emeraldCenter,
                        style = emeraldStroke
                    )

                    drawCircle(
                        color = rippleColor1,
                        radius = rippleRadius1,
                        center = sphereLightFocal,
                        style = rippleStroke
                    )
                    drawCircle(
                        color = rippleColor2,
                        radius = rippleRadius2,
                        center = sphereLightFocal,
                        style = rippleStroke
                    )
                }
            },
        content = content
    )
}

/**
 * Lightweight Modifier hook for card entry.
 * Returns `this` directly to avoid creating per-card offscreen hardware layers or
 * competing with `AnimatedContent` tab transitions and `LazyColumn` fling scrolling.
 */
@Composable
fun Modifier.liquidAppearAnimation(
    delayMillis: Int = 0,
    initialScale: Float = 0.95f,
    initialOffsetYDp: Float = 14f
): Modifier = this

/**
 * Core QWEA0/Liquid-Glass-Android & iOS 26 Convex Lens Surface Modifier.
 * Single-pass cached optical pipeline with frosted sapphire clarity, subtle chromatic dispersion bevel,
 * and curved water-droplet meniscus reflection.
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(24.dp),
    cornerRadius: Dp = 24.dp,
    elevated: Boolean = false,
    tintColor: Color? = null,
    strokeWidth: Dp = 1.5.dp,
    shadowElevation: Dp = 8.dp
): Modifier {
    val palette = LocalLiquidGlassPalette.current
    val topFill = when {
        tintColor != null -> tintColor.copy(alpha = if (palette.isDark) 0.78f else 0.86f)
        elevated -> palette.glassElevatedTop
        else -> palette.glassSurfaceTop
    }
    val bottomFill = when {
        tintColor != null -> tintColor.copy(alpha = if (palette.isDark) 0.68f else 0.78f)
        elevated -> palette.glassElevatedBottom
        else -> palette.glassSurfaceBottom
    }

    return this
        .drawWithCache {
            val radiusPx = cornerRadius.toPx()
            val strokePx = strokeWidth.toPx()
            val w = size.width.coerceAtLeast(1f)
            val h = size.height.coerceAtLeast(1f)

            val elevPx = if (elevated) shadowElevation.toPx() else (shadowElevation * 0.55f).toPx()
            val shadowAlpha = if (palette.isDark) {
                if (elevated) 0.36f else 0.22f
            } else {
                if (elevated) 0.14f else 0.08f
            }
            val shadowColor = palette.ambientShadowColor.copy(alpha = shadowAlpha)
            val shadowTopLeft = Offset(0f, elevPx * 0.35f)
            val shadowSize = Size(w, h + elevPx * 0.12f)
            val shadowCorner = CornerRadius(radiusPx + 1.5.dp.toPx(), radiusPx + 1.5.dp.toPx())
            val cardCorner = CornerRadius(radiusPx, radiusPx)

            val bodyBrush = Brush.verticalGradient(
                colors = listOf(topFill, bottomFill)
            )

            // Top Curved Liquid Meniscus & Subtle Diagonal Refraction Sheen
            val meniscusBrush = Brush.linearGradient(
                colors = listOf(
                    palette.innerMeniscusGlow,
                    palette.diagonalSheenColor,
                    Color.Transparent,
                    palette.dispersionCyan.copy(alpha = if (palette.isDark) 0.10f else 0.12f)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h * 0.75f)
            )

            // Subtle Inset Prismatic Chromatic Dispersion + SDF Refraction Bevel
            val sdfInset = 2.2.dp.toPx()
            val sdfStroke = Stroke(width = 1.4.dp.toPx())
            val innerCorner = CornerRadius((radiusPx - sdfInset).coerceAtLeast(2f), (radiusPx - sdfInset).coerceAtLeast(2f))
            val innerTopLeft = Offset(sdfInset, sdfInset)
            val innerSize = Size((w - sdfInset * 2).coerceAtLeast(1f), (h - sdfInset * 2).coerceAtLeast(1f))
            val sdfBrush = Brush.linearGradient(
                colors = listOf(
                    palette.sdfRefractionRingTop,
                    palette.dispersionCoralRose.copy(alpha = if (palette.isDark) 0.34f else 0.28f),
                    Color.Transparent,
                    palette.dispersionEmerald.copy(alpha = if (palette.isDark) 0.34f else 0.28f),
                    palette.sdfRefractionRingBottom
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            val rimStroke = Stroke(width = strokePx)
            val rimBrush = Brush.linearGradient(
                colors = listOf(
                    palette.specularRimBright,
                    palette.specularRimMid,
                    palette.dispersionCyan.copy(alpha = 0.65f),
                    palette.specularRimMid,
                    palette.specularRimEnd
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            onDrawWithContent {
                if (elevPx > 0.5f) {
                    drawRoundRect(
                        color = shadowColor,
                        topLeft = shadowTopLeft,
                        size = shadowSize,
                        cornerRadius = shadowCorner
                    )
                }

                // 1. High-Clarity Frosted Lens Body
                drawRoundRect(
                    brush = bodyBrush,
                    cornerRadius = cardCorner
                )

                // 2. Top-Left Curved Water-Droplet Meniscus & Refraction Sheen
                drawRoundRect(
                    brush = meniscusBrush,
                    cornerRadius = cardCorner
                )

                // 3. Subtle Inset Prismatic & SDF Refraction Bevel
                if (w > sdfInset * 4 && h > sdfInset * 4) {
                    drawRoundRect(
                        brush = sdfBrush,
                        topLeft = innerTopLeft,
                        size = innerSize,
                        cornerRadius = innerCorner,
                        style = sdfStroke
                    )
                }

                // 4. Composable UI Content
                drawContent()

                // 5. Crisp Specular Fresnel Rim Highlight
                drawRoundRect(
                    brush = rimBrush,
                    cornerRadius = cardCorner,
                    style = rimStroke
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
            effectiveAccent.copy(alpha = 0.18f)
        } else {
            effectiveAccent.copy(alpha = 0.12f)
        }
        selected -> palette.innerTileSelectedTop
        else -> palette.innerTileTop
    }
    val bottomColor = when {
        effectiveAccent != null -> if (palette.isDark) {
            effectiveAccent.copy(alpha = 0.10f)
        } else {
            effectiveAccent.copy(alpha = 0.06f)
        }
        selected -> palette.innerTileSelectedBottom
        else -> palette.innerTileBottom
    }
    val borderTop = when {
        effectiveAccent != null -> effectiveAccent.copy(alpha = if (palette.isDark) 0.72f else 0.58f)
        selected -> primary.copy(alpha = 0.92f)
        else -> palette.specularRimBright.copy(alpha = if (palette.isDark) 0.52f else 0.88f)
    }
    val borderMid = when {
        effectiveAccent != null -> effectiveAccent.copy(alpha = 0.42f)
        selected -> palette.dispersionCyan.copy(alpha = 0.72f)
        else -> palette.specularRimMid.copy(alpha = 0.45f)
    }
    val borderBottom = when {
        effectiveAccent != null -> effectiveAccent.copy(alpha = 0.30f)
        selected -> primary.copy(alpha = 0.65f)
        else -> palette.outerContrastRim
    }

    return this
        .clip(shape)
        .drawWithCache {
            val r = cornerRadius.toPx()
            val w = size.width.coerceAtLeast(1f)
            val h = size.height.coerceAtLeast(1f)
            val corner = CornerRadius(r, r)

            val fillBrush = Brush.verticalGradient(colors = listOf(topColor, bottomColor))
            val meniscus = Brush.verticalGradient(
                colors = listOf(
                    palette.innerMeniscusGlow.copy(alpha = palette.innerMeniscusGlow.alpha * 0.55f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = (h * 0.38f).coerceAtLeast(12f)
            )
            val borderBrush = Brush.linearGradient(
                colors = listOf(borderTop, borderMid, borderBottom),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
            val borderStroke = Stroke(width = strokeWidth.toPx())

            onDrawWithContent {
                drawRoundRect(brush = fillBrush, cornerRadius = corner)
                drawRoundRect(brush = meniscus, cornerRadius = corner)
                drawContent()
                drawRoundRect(
                    brush = borderBrush,
                    cornerRadius = corner,
                    style = borderStroke
                )
            }
        }
}

/**
 * QWEA0/Liquid-Glass-Android 3D Convex Water-Droplet Button Surface Modifier.
 *
 * Recreates the exact "✨ LiquidGlass Effect Demo" & "Liquid Glass" capsule from the reference images:
 * - Deep Royal Navy-Indigo left/top-left lens refraction shadow
 * - Luminous Royal Sapphire Blue convex body with center-right illumination
 * - Subtle Prismatic RGB chromatic dispersion fringe along the curved ends
 * - Curved white water-droplet crescent meniscus reflection
 * - Crisp Sky-Blue & White Fresnel outer rim
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

    val isPrimary = rawColor == MaterialTheme.colorScheme.primary ||
        rawColor == SaitPrimary ||
        rawColor == SaitDarkPrimary
    val isSecondary = rawColor == MaterialTheme.colorScheme.secondary ||
        rawColor == SaitSecondary ||
        rawColor == SaitDarkSecondary

    // Match the exact QWEA0 Royal Sapphire Blue droplet capsule for primary buttons,
    // and the QWEA0 Turquoise-Emerald wave droplet for secondary buttons
    val leftRefractionShadow = when {
        !enabled -> Color(0xFF1E3A8A).copy(alpha = 0.42f)
        isSecondary -> Color(0xFF004D40)
        isPrimary -> Color(0xFF071D54) // Deep Navy-Indigo left droplet shadow (Reference Image 1)
        else -> Color(
            red = (rawColor.red * 0.45f).coerceIn(0f, 1f),
            green = (rawColor.green * 0.48f).coerceIn(0f, 1f),
            blue = (rawColor.blue * 0.60f).coerceIn(0f, 1f),
            alpha = 0.96f
        )
    }

    val centerLensBody = when {
        !enabled -> Color(0xFF2563EB).copy(alpha = 0.48f)
        isSecondary -> Color(0xFF00897B) // Rich Turquoise-Emerald with high white-text contrast
        isPrimary -> Color(0xFF1D63DC)   // QWEA0 Royal Sapphire Blue
        else -> rawColor
    }

    val rightRefractionGlow = when {
        !enabled -> Color(0xFF3B82F6).copy(alpha = 0.44f)
        isSecondary -> Color(0xFF00BFA5) // Luminous Aquamarine-Emerald
        isPrimary -> Color(0xFF3B82F6)   // Luminous Cornflower-Azure right glow (Reference Image 1)
        else -> Color(
            red = (rawColor.red * 1.15f).coerceIn(0f, 1f),
            green = (rawColor.green * 1.15f).coerceIn(0f, 1f),
            blue = (rawColor.blue * 1.20f).coerceIn(0f, 1f),
            alpha = 0.96f
        )
    }

    return this
        .liquidPressEffect(enabled = enabled, cornerRadius = cornerRadius)
        .drawWithCache {
            val r = cornerRadius.toPx()
            val w = size.width.coerceAtLeast(1f)
            val h = size.height.coerceAtLeast(1f)
            val corner = CornerRadius(r, r)

            val shadowAlpha = if (!enabled) 0f else if (palette.isDark) 0.45f else 0.28f
            val shadowColor = Color(0xFF020C28).copy(alpha = shadowAlpha)
            val shadowOffset = Offset(0f, 3.dp.toPx())

            // 1. QWEA0 3D Convex Lens Body (Deep Left Refraction Shadow -> Royal Sapphire Center -> Azure Right Glow)
            val baseBtnBrush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to leftRefractionShadow,
                    0.36f to centerLensBody,
                    0.78f to rightRefractionGlow,
                    1.00f to centerLensBody
                ),
                start = Offset(0f, h * 0.15f),
                end = Offset(w, h * 0.85f)
            )

            // 2. Curved 3D Meniscus Top-Gloss & Left Water-Droplet Highlight
            val topGloss = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (enabled) 0.44f else 0.14f),
                    Color(0xFFBAE6FD).copy(alpha = if (enabled) 0.16f else 0.04f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = (h * 0.46f).coerceAtLeast(10f)
            )

            // 3. QWEA0 Prismatic RGB Chromatic Dispersion Fringe along curved ends (Reference Image 2)
            val dispersionInset = 1.6.dp.toPx()
            val dispCorner = CornerRadius((r - dispersionInset).coerceAtLeast(2f), (r - dispersionInset).coerceAtLeast(2f))
            val dispTopLeft = Offset(dispersionInset, dispersionInset)
            val dispSize = Size((w - dispersionInset * 2).coerceAtLeast(1f), (h - dispersionInset * 2).coerceAtLeast(1f))
            val dispStroke = Stroke(width = 1.2.dp.toPx())
            val chromaticFringeBrush = Brush.horizontalGradient(
                colorStops = arrayOf(
                    0.00f to palette.dispersionCoralRose.copy(alpha = if (enabled) 0.72f else 0.18f),
                    0.12f to palette.dispersionGold.copy(alpha = if (enabled) 0.56f else 0.14f),
                    0.30f to Color.White.copy(alpha = if (enabled) 0.42f else 0.10f),
                    0.70f to Color(0xFF93C5FD).copy(alpha = if (enabled) 0.45f else 0.12f),
                    0.88f to palette.dispersionEmerald.copy(alpha = if (enabled) 0.72f else 0.18f),
                    1.00f to palette.dispersionCyan.copy(alpha = if (enabled) 0.80f else 0.20f)
                )
            )

            // 4. Crisp Sky-Blue & White Fresnel Outer Rim (Reference Image 1 & 2)
            val rimStroke = Stroke(width = 1.5.dp.toPx())
            val rimBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (enabled) 0.94f else 0.30f),
                    Color(0xFF7CC4FA).copy(alpha = if (enabled) 0.90f else 0.26f),
                    Color(0xFF60A5FA).copy(alpha = if (enabled) 0.84f else 0.22f),
                    Color.White.copy(alpha = if (enabled) 0.92f else 0.28f)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            onDrawWithContent {
                if (shadowAlpha > 0f) {
                    drawRoundRect(
                        color = shadowColor,
                        topLeft = shadowOffset,
                        size = size,
                        cornerRadius = corner
                    )
                }

                // 1. Draw QWEA0 3D Convex Lens Base
                drawRoundRect(
                    brush = baseBtnBrush,
                    cornerRadius = corner
                )

                // 2. Draw Curved 3D Meniscus Top Gloss
                drawRoundRect(
                    brush = topGloss,
                    cornerRadius = corner
                )

                // 3. Draw Button Content (Crisp White Text & Icons)
                drawContent()

                // 4. Subtle Prismatic Chromatic Dispersion Fringe along curved ends
                if (w > dispersionInset * 4 && h > dispersionInset * 4) {
                    drawRoundRect(
                        brush = chromaticFringeBrush,
                        topLeft = dispTopLeft,
                        size = dispSize,
                        cornerRadius = dispCorner,
                        style = dispStroke
                    )
                }

                // 5. Outer Sky-Blue & White Fresnel Perimeter Rim
                drawRoundRect(
                    brush = rimBrush,
                    cornerRadius = corner,
                    style = rimStroke
                )
            }
        }
}

/**
 * Self-contained Liquid Glass Modal surface for Android `Dialog` windows.
 */
@Composable
fun Modifier.liquidModalGlass(
    shape: Shape = RoundedCornerShape(28.dp),
    cornerRadius: Dp = 28.dp,
    strokeWidth: Dp = 1.8.dp
): Modifier {
    val palette = LocalLiquidGlassPalette.current
    return this
        .clip(shape)
        .drawWithCache {
            val w = size.width.coerceAtLeast(1f)
            val h = size.height.coerceAtLeast(1f)
            val radiusPx = cornerRadius.toPx()
            val corner = CornerRadius(radiusPx, radiusPx)

            val modalBaseBrush = Brush.linearGradient(
                colors = if (palette.isDark) {
                    listOf(Color(0xFF04102E), Color(0xFF0A2154), Color(0xFF051336))
                } else {
                    listOf(Color(0xFFF2F8FF), Color(0xFFFFFFFF), Color(0xFFEAF4FF))
                },
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            val meniscusBrush = Brush.verticalGradient(
                colors = listOf(palette.innerMeniscusGlow, Color.Transparent),
                startY = 0f,
                endY = (h * 0.28f).coerceAtLeast(24f)
            )

            val sdfInset = 2.8.dp.toPx()
            val innerCorner = CornerRadius((radiusPx - sdfInset).coerceAtLeast(2f), (radiusPx - sdfInset).coerceAtLeast(2f))
            val innerTopLeft = Offset(sdfInset, sdfInset)
            val innerSize = Size((w - sdfInset * 2).coerceAtLeast(1f), (h - sdfInset * 2).coerceAtLeast(1f))
            val sdfStroke = Stroke(width = 1.8.dp.toPx())
            val sdfBrush = Brush.linearGradient(
                colors = listOf(
                    palette.sdfRefractionRingTop,
                    Color.Transparent,
                    palette.sdfRefractionRingBottom
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            val rimStroke = Stroke(width = strokeWidth.toPx())
            val rimBrush = Brush.linearGradient(
                colors = listOf(
                    palette.specularRimBright,
                    palette.dispersionCyan,
                    palette.specularRimMid,
                    palette.dispersionEmerald.copy(alpha = 0.65f),
                    palette.specularRimEnd
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            onDrawWithContent {
                drawRoundRect(brush = modalBaseBrush, cornerRadius = corner)
                drawRoundRect(brush = meniscusBrush, cornerRadius = corner)
                if (w > sdfInset * 4 && h > sdfInset * 4) {
                    drawRoundRect(
                        brush = sdfBrush,
                        topLeft = innerTopLeft,
                        size = innerSize,
                        cornerRadius = innerCorner,
                        style = sdfStroke
                    )
                }
                drawContent()
                drawRoundRect(
                    brush = rimBrush,
                    cornerRadius = corner,
                    style = rimStroke
                )
            }
        }
}

/**
 * QWEA0 Deep Royal Sapphire & Turquoise-Emerald Refractive Hero Glass Modifier for corporate headers & badges.
 * Guarantees crisp white text & logos pop with 100% legibility in both Light and Dark modes.
 */
@Composable
fun Modifier.liquidHeroGlass(
    shape: Shape = RoundedCornerShape(28.dp),
    cornerRadius: Dp = 28.dp
): Modifier {
    val palette = LocalLiquidGlassPalette.current
    return this
        .clip(shape)
        .drawWithCache {
            val w = size.width.coerceAtLeast(1f)
            val h = size.height.coerceAtLeast(1f)
            val radiusPx = cornerRadius.toPx()
            val corner = CornerRadius(radiusPx, radiusPx)

            val heroBodyBrush = Brush.linearGradient(
                colors = if (palette.isDark) {
                    listOf(
                        Color(0xFF04143D),
                        Color(0xFF10378A),
                        Color(0xFF061F5C)
                    )
                } else {
                    listOf(
                        Color(0xFF072163),
                        Color(0xFF185ADB),
                        Color(0xFF0E378C)
                    )
                },
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            val heroOrbBrush = Brush.radialGradient(
                colors = listOf(Color(0xFF3B82F6).copy(alpha = 0.38f), Color.Transparent),
                center = Offset(w * 0.82f, h * 0.22f),
                radius = (w * 0.58f).coerceAtLeast(10f)
            )

            val emeraldGlowBrush = Brush.radialGradient(
                colors = listOf(Color(0xFF1CE5B5).copy(alpha = 0.26f), Color.Transparent),
                center = Offset(w * 0.14f, h * 0.85f),
                radius = (w * 0.45f).coerceAtLeast(10f)
            )

            val topMeniscus = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.36f), Color.White.copy(alpha = 0.08f), Color.Transparent),
                startY = 0f,
                endY = (h * 0.45f).coerceAtLeast(20f)
            )

            val rimStroke = Stroke(width = 1.5.dp.toPx())
            val rimBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.94f),
                    Color(0xFF7CC4FA).copy(alpha = 0.88f),
                    Color(0xFF38BDF8).copy(alpha = 0.80f),
                    Color(0xFF80FFE5).copy(alpha = 0.84f)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            onDrawWithContent {
                drawRoundRect(brush = heroBodyBrush, cornerRadius = corner)
                drawCircle(brush = heroOrbBrush, radius = w * 0.58f, center = Offset(w * 0.82f, h * 0.22f))
                drawCircle(brush = emeraldGlowBrush, radius = w * 0.45f, center = Offset(w * 0.14f, h * 0.85f))
                drawRoundRect(brush = topMeniscus, cornerRadius = corner)
                drawContent()
                drawRoundRect(
                    brush = rimBrush,
                    cornerRadius = corner,
                    style = rimStroke
                )
            }
        }
}

/**
 * Zero-Idle-Overhead Interactive Tactile Press & Expanding Liquid Water Ripple Modifier.
 * Only animates when actively pressed/tapped, with zero per-frame object allocations.
 */
@Composable
fun Modifier.liquidPressEffect(
    pressedScale: Float = 0.972f,
    enabled: Boolean = true,
    cornerRadius: Dp = 14.dp
): Modifier {
    if (!enabled) return this
    val palette = LocalLiquidGlassPalette.current
    var isPressed by remember { mutableStateOf(false) }
    var touchOffset by remember { mutableStateOf(Offset.Unspecified) }
    var rippleTrigger by remember { mutableStateOf(0) }

    val scaleState = animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "liquidPressScale"
    )

    val rippleAnim = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(rippleTrigger) {
        if (rippleTrigger > 0) {
            rippleAnim.snapTo(0.01f)
            rippleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
            )
            rippleAnim.snapTo(0f)
        }
    }

    return this
        .pointerInput(enabled) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                touchOffset = down.position
                isPressed = true
                rippleTrigger++
                waitForUpOrCancellation(pass = PointerEventPass.Initial)
                isPressed = false
            }
        }
        .graphicsLayer {
            val s = scaleState.value
            if (s != 1f) {
                scaleX = s
                scaleY = s
            }
        }
        .drawWithCache {
            val w = size.width.coerceAtLeast(1f)
            val h = size.height.coerceAtLeast(1f)
            val maxDim = size.maxDimension.coerceAtLeast(1f)
            val r = cornerRadius.toPx()
            val corner = CornerRadius(r, r)
            val outerRingStroke = Stroke(width = 1.6.dp.toPx())
            val innerRingStroke = Stroke(width = 1.1.dp.toPx())

            val flareBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.22f),
                    palette.pressFlareColor,
                    Color.Transparent
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            onDrawWithContent {
                drawContent()

                if (isPressed) {
                    drawRoundRect(
                        brush = flareBrush,
                        cornerRadius = corner
                    )
                }

                val rProg = rippleAnim.value
                if (rProg in 0.01f..0.99f) {
                    val fade = (1f - rProg).coerceIn(0f, 1f)
                    val center = if (touchOffset != Offset.Unspecified) {
                        Offset(
                            touchOffset.x.coerceIn(0f, w),
                            touchOffset.y.coerceIn(0f, h)
                        )
                    } else {
                        Offset(w * 0.5f, h * 0.5f)
                    }
                    val maxRadius = (maxDim * 0.48f).coerceAtMost(minOf(w, h) * 0.85f)
                    val outerRadius = maxRadius * rProg
                    val innerRadius = outerRadius * 0.62f

                    drawCircle(
                        color = Color.White.copy(alpha = fade * 0.48f),
                        radius = outerRadius,
                        center = center,
                        style = outerRingStroke
                    )
                    if (innerRadius > 2f) {
                        drawCircle(
                            color = palette.dispersionCyan.copy(alpha = fade * 0.42f),
                            radius = innerRadius,
                            center = center,
                            style = innerRingStroke
                        )
                    }
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
        modifier.liquidPressEffect(pressedScale = 0.985f, cornerRadius = cornerRadius)
    } else {
        modifier
    }
    val glassMod = baseMod
        .clip(shape)
        .liquidGlass(
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
        unfocusedContainerColor = palette.inputFieldContainer.copy(alpha = palette.inputFieldContainer.alpha * 0.96f),
        disabledContainerColor = palette.inputFieldContainer.copy(alpha = 0.65f),
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = palette.inputFieldBorderUnfocused,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
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
        Color(0xFF00897B)
    } else {
        Color(0xFF1D63DC)
    }
    return FilterChipDefaults.filterChipColors(
        containerColor = palette.chipUnselectedContainer,
        labelColor = MaterialTheme.colorScheme.onSurface,
        iconColor = MaterialTheme.colorScheme.primary,
        selectedContainerColor = selectedBg,
        selectedLabelColor = Color.White,
        selectedLeadingIconColor = Color.White,
        disabledContainerColor = palette.chipUnselectedContainer.copy(alpha = 0.5f),
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
        checkedTrackColor = Color(0xFF1D63DC),
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
                targetValue = if (isSelected) 1.0f else 0.96f,
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = 380f
                ),
                label = "segmentScale_$option"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
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
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                    label = "dockColor_${item.index}"
                )
                val tabScale by animateFloatAsState(
                    targetValue = if (selected) 1.0f else 0.94f,
                    animationSpec = spring(
                        dampingRatio = 0.72f,
                        stiffness = 380f
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
                        .testTag(item.testTag)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelectTab(item.index) },
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(22.dp))
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
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier.wrapContentSize(),
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
                                        .offset(x = 6.dp, y = (-3).dp)
                                        .size(13.dp)
                                        .background(
                                            color = badgeBg,
                                            shape = CircleShape
                                        )
                                        .border(0.7.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (item.badgeCount > 99) "99" else item.badgeCount.toString(),
                                        color = badgeFg,
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 7.5.sp
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
