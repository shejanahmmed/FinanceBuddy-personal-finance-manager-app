package com.shejan.financebuddy.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shejan.financebuddy.data.db.AccountEntity
import com.shejan.financebuddy.ui.theme.*
import java.text.DecimalFormat

val BANK_COLOR_MAP: Map<String, String> = mapOf(
    "Hand Cash" to "#10B981",
    "Petty Cash" to "#059669",
    "Wallet Cash" to "#34D399",
    "BRAC Bank PLC" to "#0096FF",
    "The City Bank PLC" to "#007A33",
    "Eastern Bank PLC (EBL)" to "#003366",
    "Dutch-Bangla Bank PLC (DBBL)" to "#7C5CFC",
    "Prime Bank PLC" to "#FF5722",
    "Mutual Trust Bank PLC" to "#0C2340",
    "Islami Bank Bangladesh PLC (IBBL)" to "#1B5E20",
    "Al-Arafah Islami Bank PLC" to "#2E7D32",
    "Shahjalal Islami Bank PLC" to "#008080",
    "Sonali Bank PLC" to "#00875A",
    "Janata Bank PLC" to "#D97706",
    "Agrani Bank PLC" to "#059669",
    "Rupali Bank PLC" to "#0284C7",
    "Trust Bank PLC" to "#4F46E5",
    "One Bank PLC" to "#0D9488",
    "Meghna Bank PLC" to "#EA580C",
    "NRB Bank PLC" to "#9333EA",
    "bKash" to "#FF5C7C",
    "Nagad" to "#FFBD2E",
    "Rocket" to "#00D4AA",
    "Upay" to "#FFB300",
    "CellFin (IBBL)" to "#4CAF50",
    "Ok Wallet" to "#FF5722",
    "MyCash" to "#3F51B5"
)

fun getInstitutionColor(name: String, fallbackType: String = "", colorHex: String = ""): Color {
    if (colorHex.isNotBlank()) {
        try { return Color(android.graphics.Color.parseColor(colorHex)) } catch (_: Exception) {}
    }
    val mapped = BANK_COLOR_MAP.entries.firstOrNull { (k, _) ->
        name.contains(k, ignoreCase = true) || k.contains(name, ignoreCase = true)
    }?.value
    if (mapped != null) {
        try { return Color(android.graphics.Color.parseColor(mapped)) } catch (_: Exception) {}
    }
    return when (fallbackType.uppercase()) {
        "CASH" -> IncomeGreen
        "MFS" -> Color(0xFFFF5C7C)
        "BANK" -> AccentTeal
        else -> AccentTeal
    }
}

/**
 * Standard professional dropdown item content for Bank, MFS, and Cash accounts.
 *
 * Rendered as an individual card/box inside dropdown menus:
 * Left: Colored institution indicator dot matching bank branding
 * Row 1: Account name (e.g. BRAC Bank PLC, bKash) + Optional Balance / Checkmark
 * Row 2: Small 4-digits box (e.g. "•••• 1234") + Nickname/Subtype badge in the same row
 */
@Composable
fun AccountDropdownItemView(
    account: AccountEntity,
    isSelected: Boolean = false,
    showBalance: Boolean = true,
    currencyFormat: DecimalFormat? = null,
    extraTag: String = "",
    extraTagColor: Color = IncomeGreen,
    showIndicatorDot: Boolean = true,
    modifier: Modifier = Modifier
) {
    val fmt = currencyFormat ?: DecimalFormat("#,##0.00")
    val hasAccNumber = account.accountNumber.isNotBlank()
    val nickname = when {
        account.showAs.isNotBlank() && !account.showAs.equals(account.name, ignoreCase = true) -> account.showAs
        account.accountSubtype.isNotBlank() -> account.accountSubtype
        account.isManaged && account.holderName.isNotBlank() -> account.holderName
        else -> ""
    }
    val instColor = getInstitutionColor(account.name, account.type, account.colorHex)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) AccentTeal.copy(alpha = 0.12f) else CardDark)
            .border(
                1.dp,
                if (isSelected) AccentTeal.copy(alpha = 0.5f) else DividerColor.copy(alpha = 0.6f),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showIndicatorDot) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(instColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // Row 1: Account / Bank Name
                    Text(
                        text = account.name,
                        color = if (isSelected) AccentTeal else TextPrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Row 2: Secondary info - 4 digits box + Nickname / Subtype pill in the same row
                    if (hasAccNumber || nickname.isNotBlank() || extraTag.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (hasAccNumber) {
                                val last4 = account.accountNumber.trim().takeLast(4)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CardDarker)
                                        .border(1.dp, DividerColor, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                ) {
                                    Text(
                                        text = "•••• $last4",
                                        color = TextPrimary,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            if (nickname.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AccentTeal.copy(alpha = 0.12f))
                                        .border(1.dp, AccentTeal.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 1.5.dp)
                                ) {
                                    Text(
                                        text = nickname,
                                        color = AccentTeal,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (extraTag.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(extraTagColor.copy(alpha = 0.12f))
                                        .border(1.dp, extraTagColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 1.5.dp)
                                ) {
                                    Text(
                                        text = extraTag,
                                        color = extraTagColor,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Trailing: Balance and/or Checkmark
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showBalance) {
                    Text(
                        text = "৳${fmt.format(account.balance)}",
                        color = AccentTeal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = AccentTeal,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Standard preset dropdown item view for Bangladeshi Banks, MFS wallets, and Cash options.
 * Rendered as an individual card/box inside dropdown menus.
 */
@Composable
fun PresetDropdownItemView(
    presetName: String,
    presetType: String = "BANK",
    tagText: String = "+ Link",
    modifier: Modifier = Modifier
) {
    val instColor = getInstitutionColor(presetName, presetType)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardDark)
            .border(1.dp, DividerColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(instColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = presetName,
                    color = TextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(instColor.copy(alpha = 0.12f))
                    .border(1.dp, instColor.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = tagText,
                    color = instColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

