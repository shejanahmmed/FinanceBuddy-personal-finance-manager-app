package com.shejan.financebuddy.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shejan.financebuddy.ui.theme.*
import java.util.Locale

/**
 * Loads an [ImageBitmap] from a URI string. Returns null when the URI is empty or invalid.
 * This is a `@Composable` function so it uses [remember] to avoid reloading on every recomposition.
 */
@Composable
fun rememberBitmapFromUri(uriString: String?): ImageBitmap? {
    val context = LocalContext.current
    return remember(uriString) {
        if (uriString.isNullOrEmpty()) null
        else {
            try {
                val uri = android.net.Uri.parse(uriString)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    android.graphics.BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * A circular avatar composable for a payee / recipient.
 * Shows the uploaded profile photo when available; otherwise falls back to the
 * initial letter of [name] on a deterministic color background.
 *
 * @param name      The payee's display name (used for initial + bg color derivation).
 * @param imageUri  Optional URI string pointing to the payee's profile photo.
 * @param size      Diameter of the avatar circle.
 * @param fontSize  Font size of the fallback initial letter.
 */
@Composable
fun PayeeAvatar(
    name: String,
    imageUri: String?,
    size: Dp = 40.dp,
    fontSize: TextUnit = 16.sp
) {
    val initial = name.trim().take(1).uppercase(Locale.ROOT)
    val avatarBg = remember(name) {
        val hash = name.hashCode()
        val palette = listOf(
            AccentTeal, AccentBlue, TransferYellow, IncomeGreen,
            Color(0xFF9C27B0), Color(0xFFE91E63)
        )
        palette[Math.abs(hash) % palette.size]
    }
    val bitmap = rememberBitmapFromUri(imageUri)

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(avatarBg.copy(alpha = 0.12f))
            .border(1.dp, avatarBg.copy(alpha = 0.25f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initial,
                color = avatarBg,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
