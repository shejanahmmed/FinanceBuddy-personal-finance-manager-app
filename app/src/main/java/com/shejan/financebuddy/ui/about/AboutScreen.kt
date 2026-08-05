package com.shejan.financebuddy.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.shejan.financebuddy.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shejan.financebuddy.ui.theme.*

@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = BackgroundDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // ── Top Bar ─────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CardDarker)
                        .border(1.dp, DividerColor, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = "About FinanceBuddy",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            // ── App Hero Card ───────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                CardDark,
                                CardDarker
                            )
                        )
                    )
                    .border(1.dp, AccentTeal.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App Logo Badge
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(2.dp, AccentTeal.copy(alpha = 0.5f), CircleShape)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.financebuddy),
                            contentDescription = "FinanceBuddy Logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "FinanceBuddy",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Personal Finance Manager for Bangladesh 🇧🇩",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = AccentTeal,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(DividerColor.copy(alpha = 0.4f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "v1.0.0 (Beta)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentTeal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Section 1: Overview & Privacy ───────────────────
            Text(
                text = "OVERVIEW",
                color = AccentTeal,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardDark)
                    .border(1.dp, DividerColor, RoundedCornerShape(18.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "FinanceBuddy is a smart, privacy-first personal finance manager tailored specifically for Bangladeshi users. Track incomes, daily expenses, bank transfers, SMS notifications, loans, and investment portfolios seamlessly.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )

                HorizontalDivider(color = DividerColor)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = IncomeGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "100% Offline & Private",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "All financial records stay strictly stored on your local device. No cloud sync, no tracking.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Section 2: Key Features ──────────────────────────
            Text(
                text = "KEY FEATURES",
                color = AccentTeal,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardDark)
                    .border(1.dp, DividerColor, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FeatureRowItem(
                    icon = Icons.Default.AccountBalance,
                    iconColor = AccentTeal,
                    title = "Bangladeshi Banks & MFS Support",
                    subtitle = "Built-in integration for BRAC, City, EBL, DBBL, IBBL, bKash, Nagad, Rocket, Upay, CellFin & more."
                )

                HorizontalDivider(color = DividerColor)

                FeatureRowItem(
                    icon = Icons.Default.Sms,
                    iconColor = AccentBlue,
                    title = "Automatic SMS Parsing",
                    subtitle = "Instantly detect debit/credit SMS notifications from banks and convert them into pending transactions."
                )

                HorizontalDivider(color = DividerColor)

                FeatureRowItem(
                    icon = Icons.Default.PieChart,
                    iconColor = AccentPurple,
                    title = "Budgeting & Savings Goals",
                    subtitle = "Set category spending caps, track balance trends, and monitor savings targets with progress analytics."
                )

                HorizontalDivider(color = DividerColor)

                FeatureRowItem(
                    icon = Icons.Default.Lock,
                    iconColor = IncomeGreen,
                    title = "App Lock & Biometric Protection",
                    subtitle = "Secure your app with 6-digit PIN lock, Fingerprint authentication, and screenshot prevention."
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Section 3: Technical Specifications ──────────────
            Text(
                text = "APP SPECIFICATIONS",
                color = AccentTeal,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardDark)
                    .border(1.dp, DividerColor, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SpecInfoRow(label = "App Version", value = "v1.0.0 (Beta)")
                SpecInfoRow(label = "Developer", value = "Shejan Ahmmed")
                SpecInfoRow(label = "UI Framework", value = "Jetpack Compose (Kotlin DSL)")
                SpecInfoRow(label = "Database Engine", value = "Room SQLite (Encrypted)")
                SpecInfoRow(label = "Minimum Android OS", value = "Android 7.0 (API 24)")
                SpecInfoRow(label = "Default Currency", value = "Bangladeshi Taka (৳ BDT)")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Section 4: Contributors ──────────────────────────
            Text(
                text = "CONTRIBUTORS",
                color = AccentTeal,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardDark)
                    .border(1.dp, DividerColor, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ContributorItem(
                    name = "Izaz Ahmmed Tuhin",
                    role = "Contributor & Core Developer",
                    githubUrl = "https://github.com/izaz-swe",
                    linkedInUrl = "https://www.linkedin.com/in/izaz-ahmmed-tuhin-273a97249/"
                )

                HorizontalDivider(color = DividerColor)

                ContributorItem(
                    name = "Farjan Ahmmed",
                    role = "Contributor & Core Developer",
                    githubUrl = "https://github.com/shejanahmmed",
                    linkedInUrl = "https://www.linkedin.com/in/farjanahmmed/"
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Footer ─────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Crafted with care in Bangladesh 🇧🇩",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Copyright © 2026 FinanceBuddy. All rights reserved.",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ContributorItem(
    name: String,
    role: String,
    githubUrl: String,
    linkedInUrl: String
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = role,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // GitHub Icon Button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(CardDarker)
                    .border(1.dp, DividerColor, CircleShape)
                    .clickable {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(githubUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = GitHubIconVector,
                    contentDescription = "GitHub Profile",
                    tint = TextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // LinkedIn Icon Button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(CardDarker)
                    .border(1.dp, AccentBlue.copy(alpha = 0.3f), CircleShape)
                    .clickable {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(linkedInUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = LinkedInIconVector,
                    contentDescription = "LinkedIn Profile",
                    tint = Color(0xFF0A66C2),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private val GitHubIconVector: ImageVector
    get() = ImageVector.Builder(
        name = "GitHub",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = androidx.compose.ui.graphics.vector.PathParser().parsePathString(
            "M12 2A10 10 0 0 0 2 12c0 4.42 2.87 8.17 6.84 9.5.5.08.66-.23.66-.5v-1.69c-2.77.6-3.36-1.34-3.36-1.34-.46-1.16-1.11-1.47-1.11-1.47-.91-.62.07-.6.07-.6 1 .07 1.53 1.03 1.53 1.03.87 1.52 2.34 1.07 2.91.83.1-.65.35-1.09.63-1.34-2.22-.25-4.55-1.11-4.55-4.92 0-1.11.38-2 1.03-2.71-.1-.25-.45-1.29.1-2.64 0 0 .84-.27 2.75 1.02.79-.22 1.65-.33 2.5-.33.85 0 1.71.11 2.5.33 1.91-1.29 2.75-1.02 2.75-1.02.55 1.35.2 2.39.1 2.64.65.71 1.03 1.6 1.03 2.71 0 3.82-2.34 4.66-4.57 4.91.36.31.69.92.69 1.85V21c0 .27.16.59.67.5C19.14 20.16 22 16.42 22 12A10 10 0 0 0 12 2z"
        ).toNodes(),
        fill = androidx.compose.ui.graphics.SolidColor(Color.White)
    ).build()

private val LinkedInIconVector: ImageVector
    get() = ImageVector.Builder(
        name = "LinkedIn",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = androidx.compose.ui.graphics.vector.PathParser().parsePathString(
            "M19 3a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h14m-.5 15.5v-5.3a3.26 3.26 0 0 0-3.26-3.26c-.85 0-1.84.52-2.28 1.3v-1.11h-2.79v8.37h2.79v-4.93c0-.77.62-1.4 1.39-1.4a1.4 1.4 0 0 1 1.4 1.4v4.93h2.75M6.46 10.9v8.37H9.25V10.9H6.46M7.86 6.72a1.47 1.47 0 1 0 0 2.94 1.47 1.47 0 0 0 0-2.94z"
        ).toNodes(),
        fill = androidx.compose.ui.graphics.SolidColor(Color.White)
    ).build()

@Composable
private fun FeatureRowItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.12f))
                .border(1.dp, iconColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun SpecInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
