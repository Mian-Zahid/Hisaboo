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
import androidx.compose.ui.text.font.FontFamily
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
                        .width(42.
