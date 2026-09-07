package com.shejan.financebuddy.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shejan.financebuddy.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    title: String = "Reminder Time",
    subtitle: String = "Choose when you want to be reminded",
    otherSlots: List<Triple<Int, Int, Int>> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    // Check if the currently chosen time conflicts with another slot
    val conflictingSlot = remember(timePickerState.hour, timePickerState.minute, otherSlots) {
        otherSlots.find { it.second == timePickerState.hour && it.third == timePickerState.minute }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardDark,
            border = BorderStroke(1.dp, DividerColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon & Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AccentTeal.copy(alpha = 0.15f))
                            .border(1.dp, AccentTeal.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = AccentTeal,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = subtitle,
                            fontSize = 11.5.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Material 3 TimePicker placed directly without nested bounding box
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = CardDarker,
                        clockDialSelectedContentColor = Color.White,
                        clockDialUnselectedContentColor = TextPrimary,
                        selectorColor = AccentTeal,
                        containerColor = Color.Transparent,
                        periodSelectorBorderColor = DividerColor,
                        periodSelectorSelectedContainerColor = AccentTeal,
                        periodSelectorUnselectedContainerColor = CardDarker,
                        periodSelectorSelectedContentColor = Color.White,
                        periodSelectorUnselectedContentColor = TextSecondary,
                        timeSelectorSelectedContainerColor = AccentTeal.copy(alpha = 0.25f),
                        timeSelectorUnselectedContainerColor = CardDarker,
                        timeSelectorSelectedContentColor = AccentTeal,
                        timeSelectorUnselectedContentColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Preset Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        Pair(20, 0) to "8:00 PM",
                        Pair(21, 0) to "9:00 PM",
                        Pair(22, 0) to "10:00 PM",
                        Pair(8, 0) to "8:00 AM"
                    ).forEach { (time, label) ->
                        val isSelected = timePickerState.hour == time.first && timePickerState.minute == time.second
                        Surface(
                            onClick = {
                                timePickerState.hour = time.first
                                timePickerState.minute = time.second
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) AccentTeal.copy(alpha = 0.2f) else CardDarker,
                            border = BorderStroke(1.dp, if (isSelected) AccentTeal else DividerColor),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) AccentTeal else TextSecondary,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Inline Conflict Warning Message inside the dialog
                AnimatedVisibility(
                    visible = conflictingSlot != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ExpenseRed.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Reminder ${conflictingSlot?.first} is already set for this time. Please pick a different time.",
                                color = ExpenseRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons (Cancel / Confirm)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        color = CardDarker,
                        border = BorderStroke(1.dp, DividerColor),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Cancel",
                                color = TextPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            if (conflictingSlot == null) {
                                onConfirm(timePickerState.hour, timePickerState.minute)
                            }
                        },
                        enabled = conflictingSlot == null,
                        shape = RoundedCornerShape(12.dp),
                        color = if (conflictingSlot == null) AccentTeal else CardDarker,
                        border = BorderStroke(1.dp, if (conflictingSlot == null) AccentTeal else DividerColor),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Set Reminder",
                                color = if (conflictingSlot == null) BackgroundDark else TextMuted,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
