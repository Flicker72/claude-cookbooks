package com.holidate.app.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue

/**
 * Shows a profile photo when we received one in the beacon, otherwise a deterministic
 * gradient + initial derived from the node id, so every person still looks distinct offline.
 */
@Composable
fun ProfilePhoto(
    base64: String?,
    seed: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
) {
    val bitmap = remember(base64) { base64?.let { decodeBase64Image(it) } }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(shape),
        )
    } else {
        val colors = gradientFor(seed)
        Box(
            modifier = modifier
                .clip(shape)
                .background(Brush.linearGradient(colors)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = seed.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 72.sp,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InterestChips(interests: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        interests.forEach { interest ->
            Surface(
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(vertical = 4.dp).wrapContentSize(),
            ) {
                Text(
                    text = interest,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

private fun decodeBase64Image(base64: String): android.graphics.Bitmap? = try {
    val bytes = Base64.decode(base64, Base64.NO_WRAP)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
} catch (_: Exception) {
    null
}

private val palettes = listOf(
    listOf(Color(0xFFFF5A6E), Color(0xFFFF9776)),
    listOf(Color(0xFF14B8A6), Color(0xFF3B82F6)),
    listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)),
    listOf(Color(0xFFF59E0B), Color(0xFFEF4444)),
    listOf(Color(0xFF10B981), Color(0xFF84CC16)),
)

private fun gradientFor(seed: String): List<Color> =
    palettes[(seed.hashCode().absoluteValue) % palettes.size]
