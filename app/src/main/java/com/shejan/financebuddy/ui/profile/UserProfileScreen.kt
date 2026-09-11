package com.shejan.financebuddy.ui.profile

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shejan.financebuddy.data.PreferencesManager
import com.shejan.financebuddy.ui.theme.AccentTeal
import com.shejan.financebuddy.ui.theme.BackgroundDark
import com.shejan.financebuddy.ui.theme.CardDark
import com.shejan.financebuddy.ui.theme.CardDarker
import com.shejan.financebuddy.ui.theme.DividerColor
import com.shejan.financebuddy.ui.theme.ExpenseRed
import com.shejan.financebuddy.ui.theme.GradientEnd
import com.shejan.financebuddy.ui.theme.GradientStart
import com.shejan.financebuddy.ui.theme.IncomeGreen
import com.shejan.financebuddy.ui.theme.TextMuted
import com.shejan.financebuddy.ui.theme.TextPrimary
import com.shejan.financebuddy.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    preferencesManager: PreferencesManager,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val profileName by preferencesManager.profileName.collectAsState(initial = "User")
    val profileImagePath by preferencesManager.profileImagePath.collectAsState(initial = "")

    var isEditingName by remember { mutableStateOf(false) }
    var nameInput by remember(profileName) { mutableStateOf(profileName) }

    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val path = saveUriToInternalStorage(context, uri)
            if (path != null) {
                scope.launch {
                    preferencesManager.setProfileImagePath(path)
                    Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val profileBitmap = remember(profileImagePath) {
        if (profileImagePath.isNotEmpty()) {
            try {
                val file = File(profileImagePath)
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)
                } else null
            } catch (_: Exception) {
                null
            }
        } else null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ── Top App Bar ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "User Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Personal details & account settings",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            // ── Scrollable Content ───────────────────────────────────
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = 36.dp)
            ) {
                // ── Hero Profile Avatar Card ─────────────────────────
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CardDark),
                        border = BorderStroke(1.dp, DividerColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Avatar with camera badge
                            Box(
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(104.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(colors = listOf(GradientStart.copy(alpha = 0.3f), GradientEnd.copy(alpha = 0.3f))))
                                        .border(2.dp, Brush.linearGradient(colors = listOf(GradientStart, GradientEnd)), CircleShape)
                                        .clickable {
                                            pickMedia.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (profileBitmap != null) {
                                        Image(
                                            bitmap = profileBitmap.asImageBitmap(),
                                            contentDescription = "Profile Photo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Default Profile",
                                            tint = AccentTeal,
                                            modifier = Modifier.size(54.dp)
                                        )
                                    }
                                }

                                // Camera edit badge button
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(GradientEnd)
                                        .border(2.dp, CardDark, CircleShape)
                                        .clickable {
                                            pickMedia.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Change Photo",
                                        tint = BackgroundDark,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Display Name & Edit Field
                            if (isEditingName) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    OutlinedTextField(
                                        value = nameInput,
                                        onValueChange = { nameInput = it },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AccentTeal,
                                            unfocusedBorderColor = DividerColor,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            cursorColor = AccentTeal
                                        ),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = {
                                            val trimmed = nameInput.trim()
                                            if (trimmed.isNotBlank()) {
                                                scope.launch {
                                                    preferencesManager.setProfileName(trimmed)
                                                    isEditingName = false
                                                    focusManager.clearFocus()
                                                }
                                            }
                                        })
                                    )

                                    IconButton(
                                        onClick = {
                                            val trimmed = nameInput.trim()
                                            if (trimmed.isNotBlank()) {
                                                scope.launch {
                                                    preferencesManager.setProfileName(trimmed)
                                                    isEditingName = false
                                                    focusManager.clearFocus()
                                                    Toast.makeText(context, "Name updated", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(AccentTeal)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Save",
                                            tint = BackgroundDark,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = profileName.ifBlank { "User" },
                                        fontSize = 21.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { isEditingName = true },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Name",
                                            tint = AccentTeal,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Local Account Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AccentTeal.copy(alpha = 0.12f))
                                    .border(1.dp, AccentTeal.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "🇧🇩 BDT (৳) • Offline Storage",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AccentTeal
                                )
                            }

                            // Remove Photo Option if custom photo exists
                            if (profileImagePath.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            scope.launch {
                                                preferencesManager.setProfileImagePath("")
                                                Toast.makeText(context, "Profile photo removed", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = ExpenseRed.copy(alpha = 0.8f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Remove Photo",
                                        fontSize = 11.5.sp,
                                        color = ExpenseRed.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // ── Privacy Assurance Card ───────────────────────────
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardDarker),
                        border = BorderStroke(1.dp, DividerColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(IncomeGreen.copy(alpha = 0.15f))
                                    .border(1.dp, IncomeGreen.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = IncomeGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "100% On-Device Privacy",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Your profile picture, personal details, and finances stay strictly on this device.",
                                    fontSize = 11.5.sp,
                                    color = TextMuted,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Copies a Uri content to the app's secure filesDir and returns its absolute path.
 */
private fun saveUriToInternalStorage(context: Context, uri: Uri): String? {
    val resolver = context.contentResolver
    val mimeType = resolver.getType(uri)
    val extension = when (mimeType) {
        "image/png" -> "png"
        "image/gif" -> "gif"
        "image/webp" -> "webp"
        else -> "jpg"
    }

    val fileName = "profile_pic_${System.currentTimeMillis()}.$extension"
    val file = File(context.filesDir, fileName)

    return try {
        context.filesDir.listFiles()?.forEach { f ->
            if (f.name.startsWith("profile_pic_")) {
                f.delete()
            }
        }

        resolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
