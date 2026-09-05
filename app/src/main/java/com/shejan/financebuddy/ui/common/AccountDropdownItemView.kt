package com.shejan.financebuddy.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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

/**
 * Standard professional dropdown item content for Bank, MFS, and Cash accounts.
 *
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
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
                                .background(CardDark)
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
