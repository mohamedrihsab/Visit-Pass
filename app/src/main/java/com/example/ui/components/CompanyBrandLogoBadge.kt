package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import java.io.File

/**
 * High-contrast, crisp white Company & Branch Brand Logo Badge.
 *
 * Guarantees that the official SAIT dark navy & red logo (and custom branch logos)
 * are 100% visible with optimal contrast across Light Mode, Dark Mode, and deep
 * sapphire Liquid Glass hero headers.
 *
 * Includes automatic fallback to `R.drawable.sait_logo` if a custom logoUri fails to load.
 */
@Composable
fun CompanyBrandLogoBadge(
    modifier: Modifier = Modifier,
    logoUri: String? = null,
    branchName: String = "SAIT Group",
    cornerRadius: Dp = 10.dp,
    horizontalPadding: Dp = 6.dp,
    verticalPadding: Dp = 3.dp,
    testTag: String = "company_logo_header"
) {
    val context = LocalContext.current
    val imageModel = remember(logoUri) {
        val uriStr = logoUri?.trim()
        when {
            uriStr.isNullOrBlank() -> R.drawable.sait_logo
            uriStr.startsWith("/") -> File(uriStr)
            uriStr.startsWith("content:") || uriStr.startsWith("file:") || uriStr.startsWith("http") -> Uri.parse(uriStr)
            else -> uriStr
        }
    }

    Surface(
        shape = RoundedCornerShape(cornerRadius),
        color = Color.White,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier.testTag(testTag)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (!logoUri.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageModel)
                        .crossfade(true)
                        .error(R.drawable.sait_logo)
                        .fallback(R.drawable.sait_logo)
                        .build(),
                    contentDescription = "$branchName Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = horizontalPadding, vertical = verticalPadding),
                    contentScale = ContentScale.Fit
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.sait_logo),
                    contentDescription = "$branchName Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = horizontalPadding, vertical = verticalPadding),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
