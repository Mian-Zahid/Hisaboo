package com.mianzahid.expenseentry.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mianzahid.expenseentry.model.*
import com.mianzahid.expenseentry.ui.components.GlassCard
import com.mianzahid.expenseentry.ui.components.TintedIconBadge
import com.mianzahid.expenseentry.ui.navigation.HisabooRoutes
import com.mianzahid.expenseentry.ui.theme.*
import com.mianzahid.expenseentry.viewmodel.HisabooViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HisabooViewModel,
    onQuickCashIn: () -> Unit,
    onQuickCashOut: () -> Unit,
    onNavigate: (String) -> Unit
) {
    var showHubSheet by remember { mutableStateOf(false) }
    val todayHeaderDate = remember {
        SimpleDateFormat("EEEE, dd MMM yyyy", Locale.US).format(Date())
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgObsidian)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            HomeTopBar(dateSubtitle = todayHeaderDate, onOpenHub = { showHubSheet = true })
        }

        item {
            HeroWalletCard(
                balance = viewModel.balanceInHand,
                cashIn = viewModel.totalCashIn,
                cashOut = viewModel.totalCashOut,
                connectedEmail = viewModel.connectedGoogleEmail,
                onBadgeClick = { onNavigate(HisabooRoutes.BACKUP) }
            )
        }

        item {
            QuickActionRow(
                onCashIn = onQuickCashIn,
                onCashOut = onQuickCashOut,
                onBudget = { onNavigate(HisabooRoutes.BUDGET) },
                onReport = { onNavigate(HisabooRoutes.REPORT) }
            )
        }

        item {
            DailyHadithCard(
                hadith = viewModel.dailyHadith,
                selectedLang = viewModel.selectedHadithLang,
                onSelectLang = { viewModel.selectedHadithLang = it }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "${viewModel.transactions.size} entries",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
        }

        if (viewModel.transactions.isEmpty()) {
            item {
                GlassCard(
                    cornerRadius = 22.dp,
                    contentPadding = PaddingValues(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TintedIconBadge(
                            icon = Icons.Rounded.AccountBalanceWallet,
                            tint = EmeraldPrimary,
                            size = 48.dp,
                            iconSize = 24.dp,
                            isCircle = true
                        )
                        Text(
                            text = "No transactions recorded yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tap + Cash In or - Cash Out above — entries auto-save offline & sync to Gmail when connected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(viewModel.transactions.take(6), key = { it.id }) { tx ->
                TransactionRowCard(
                    entry = tx,
                    onDelete = { viewModel.deleteTransaction(tx.id) }
                )
            }
        }
    }

    if (showHubSheet) {
        ModalBottomSheet(
            onDismissRequest = { showHubSheet = false },
            containerColor = SurfaceCard,
            contentColor = TextPrimary,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(42.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(BorderGlass)
                )
            }
        ) {
            HisabooHubSheetContent(
                viewModel = viewModel,
                onDismiss = { showHubSheet = false },
                onNavigate = { route ->
                    showHubSheet = false
                    onNavigate(route)
                }
            )
        }
    }
}

@Composable
private fun HomeTopBar(dateSubtitle: String, onOpenHub: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(EmeraldGlowTint)
                    .border(1.dp, EmeraldPrimary.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "H",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldPrimary
                )
            }

            Column {
                Text(
                    text = "Hisaboo",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = dateSubtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SurfaceGlass)
                    .border(1.dp, BorderGlass, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.VerifiedUser,
                    contentDescription = "Admin",
                    tint = AmberAccent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Admin",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceGlass)
                    .border(1.dp, BorderGlass, CircleShape)
                    .clickable { onOpenHub() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.GridView,
                    contentDescription = "Hisaboo Hub",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun HeroWalletCard(
    balance: Double,
    cashIn: Double,
    cashOut: Double,
    connectedEmail: String?,
    onBadgeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(HeroWalletGradient)
            .border(1.dp, HeroCardBorder, RoundedCornerShape(26.dp))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BALANCE • IN HAND",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
                color = Color(0xFFD1FAE5)
            )
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x2603261C))
                    .clickable { onBadgeClick() }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (connectedEmail != null) Icons.Rounded.CloudDone else Icons.Rounded.PhoneAndroid,
                    contentDescription = null,
                    tint = Color(0xFFECFDF5),
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = if (connectedEmail != null) "Gmail Sync ON" else "Offline Saved",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFECFDF5)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = formatRs(balance),
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = "Available amount with you",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFA7F3D0)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(Color(0x6606231B))
                    .border(1.dp, Color(0x26FFFFFF), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowDownward,
                    contentDescription = "Cash In",
                    tint = CashInGreen,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "IN: ${formatRs(cashIn)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                    color = CashInGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(Color(0x6606231B))
                    .border(1.dp, Color(0x26FFFFFF), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowUpward,
                    contentDescription = "Cash Out",
                    tint = CashOutRed,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "OUT: ${formatRs(cashOut)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                    color = CashOutRed,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun QuickActionRow(
    onCashIn: () -> Unit,
    onCashOut: () -> Unit,
    onBudget: () -> Unit,
    onReport: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        QuickActionButton(
            label = "+ Cash In",
            icon = Icons.Rounded.Add,
            tint = CashInGreen,
            onClick = onCashIn
        )
        QuickActionButton(
            label = "- Cash Out",
            icon = Icons.Rounded.Remove,
            tint = CashOutRed,
            onClick = onCashOut
        )
        QuickActionButton(
            label = "Budget",
            icon = Icons.Rounded.PieChart,
            tint = AmberAccent,
            onClick = onBudget
        )
        QuickActionButton(
            label = "Report",
            icon = Icons.Rounded.BarChart,
            tint = EmeraldPrimary,
            onClick = onReport
        )
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.14f))
                .border(1.dp, tint.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

@Composable
private fun DailyHadithCard(
    hadith: DailyHadith,
    selectedLang: HadithLanguage,
    onSelectLang: (HadithLanguage) -> Unit
) {
    GlassCard(cornerRadius = 24.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TintedIconBadge(
                    icon = Icons.Rounded.FormatQuote,
                    tint = EmeraldPrimary,
                    size = 34.dp,
                    iconSize = 18.dp,
                    isCircle = true
                )
                Text(
                    text = "Daily Hadith",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SurfaceGlass)
                    .border(1.dp, BorderGlass, CircleShape)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                HadithLanguage.entries.forEach { lang ->
                    val active = lang == selectedLang
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (active) EmeraldPrimary else Color.Transparent)
                            .clickable { onSelectLang(lang) }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = lang.label,
                            fontSize = 11.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            color = if (active) OnEmeraldPrimary else TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val isRtl = selectedLang == HadithLanguage.URDU || selectedLang == HadithLanguage.ARABIC
        val textContent = when (selectedLang) {
            HadithLanguage.URDU -> hadith.urdu
            HadithLanguage.ENGLISH -> hadith.english
            HadithLanguage.ARABIC -> hadith.arabic
        }

        CompositionLocalProvider(
            LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
        ) {
            Text(
                text = textContent,
                fontSize = if (isRtl) 17.sp else 14.sp,
                lineHeight = if (isRtl) 28.sp else 21.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                textAlign = if (isRtl) TextAlign.Right else TextAlign.Left,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = hadith.reference,
            style = MaterialTheme.typography.labelSmall,
            color = EmeraldPrimary
        )
    }
}

@Composable
fun TransactionRowCard(
    entry: TransactionEntry,
    onDelete: () -> Unit
) {
    val (icon, tint) = getCategoryVisuals(entry.categoryName, entry.type)
    val amountColor = if (entry.type == TransactionType.CASH_IN) CashInGreen else CashOutRed
    val prefix = if (entry.type == TransactionType.CASH_IN) "+" else "-"

    GlassCard(
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                TintedIconBadge(icon = icon, tint = tint, size = 42.dp)
                Column {
                    Text(
                        text = entry.note,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${entry.categoryName} • ${entry.date}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "$prefix${formatRs(entry.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private data class HubModuleItem(
    val title: String,
    val icon: ImageVector,
    val tint: Color,
    val route: String
)

@Composable
private fun HisabooHubSheetContent(
    viewModel: HisabooViewModel,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val modules = listOf(
        HubModuleItem("Monthly Report", Icons.Rounded.Assessment, EmeraldPrimary, HisabooRoutes.REPORT),
        HubModuleItem("Search", Icons.Rounded.Search, BlueAccent, HisabooRoutes.SEARCH),
        HubModuleItem("Planned Payments", Icons.Rounded.EventRepeat, AmberAccent, HisabooRoutes.PLANNED_PAYMENTS),
        HubModuleItem("Reminders", Icons.Rounded.NotificationsActive, CashOutRed, HisabooRoutes.REMINDERS),
        HubModuleItem("Budget", Icons.Rounded.PieChart, EmeraldPrimary, HisabooRoutes.BUDGET),
        HubModuleItem("Loan", Icons.Rounded.Handshake, CashInGreen, HisabooRoutes.LOAN),
        HubModuleItem("Utility Bills", Icons.Rounded.Bolt, AmberAccent, HisabooRoutes.UTILITY_BILLS),
        HubModuleItem("Family", Icons.Rounded.Groups, BlueAccent, HisabooRoutes.FAMILY),
        HubModuleItem("Backup / Export", Icons.Rounded.CloudUpload, EmeraldPrimary, HisabooRoutes.BACKUP),
        HubModuleItem("Calculator", Icons.Rounded.Calculate, TextPrimary, HisabooRoutes.TOOLS)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Hisaboo Hub",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            modules.chunked(4).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { mod ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(SurfaceGlass)
                                .border(1.dp, BorderGlass, RoundedCornerShape(18.dp))
                                .clickable { onNavigate(mod.route) }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TintedIconBadge(
                                icon = mod.icon,
                                tint = mod.tint,
                                size = 38.dp,
                                iconSize = 20.dp
                            )
                            Text(
                                text = mod.title,
                                fontSize = 11.sp,
                                lineHeight = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    repeat(4 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        GlassCard(
            cornerRadius = 22.dp,
            containerColor = SurfaceGlass,
            contentPadding = PaddingValues(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TintedIconBadge(
                        icon = Icons.Rounded.Fingerprint,
                        tint = EmeraldPrimary,
                        size = 36.dp,
                        iconSize = 20.dp,
                        isCircle = true
                    )
                    Text(
                        text = "Fingerprint Lock",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }

                Switch(
                    checked = viewModel.fingerprintEnabled,
                    onCheckedChange = { viewModel.toggleFingerprint(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OnEmeraldPrimary,
                        checkedTrackColor = EmeraldPrimary,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = SurfaceCard
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(SurfaceCard)
                        .border(1.dp, BorderGlass, CircleShape)
                        .clickable { onNavigate(HisabooRoutes.CHANGE_PIN) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Pin,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Change PIN",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1.1f)
                        .clip(CircleShape)
                        .background(SurfaceCard)
                        .border(1.dp, BorderGlass, CircleShape)
                        .clickable { viewModel.toggleLanguage() }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Language,
                        contentDescription = null,
                        tint = BlueAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Language: ${viewModel.appLanguage}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CashOutTint)
                        .border(1.dp, CashOutRed.copy(alpha = 0.4f), CircleShape)
                        .clickable {
                            onDismiss()
                            viewModel.isAppLocked = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "Lock App",
                        tint = CashOutRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Text(
            text = "Made by Mian Zahid Sharif",
            fontSize = 11.sp,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
    }
}
