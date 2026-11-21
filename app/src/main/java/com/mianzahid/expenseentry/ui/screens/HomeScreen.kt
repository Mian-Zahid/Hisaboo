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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mianzahid.expenseentry.model.Transaction
import com.mianzahid.expenseentry.model.TransactionType
import com.mianzahid.expenseentry.ui.navigation.HisabooRoutes
import com.mianzahid.expenseentry.ui.theme.*
import com.mianzahid.expenseentry.viewmodel.HisabooViewModel
import com.mianzahid.expenseentry.viewmodel.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HisabooViewModel,
    onQuickCashIn: () -> Unit,
    onQuickCashOut: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val transactions = viewModel.transactions
    val totalIn = viewModel.totalCashIn()
    val totalOut = viewModel.totalCashOut()
    val netBalance = viewModel.netBalance()

    var showHubSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgObsidian)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 14.dp,
                bottom = 110.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hisaboo",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = viewModel.todayFormatted(),
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search Shortcut
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(SurfaceGlass)
                                .border(1.dp, BorderGlass, CircleShape)
                                .clickable { onNavigate(HisabooRoutes.SEARCH) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // 4-Dots Hub Menu Button
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(SurfaceGlass)
                                .border(1.dp, BorderGlass, CircleShape)
                                .clickable { showHubSheet = true },
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

            // Net Balance Hero Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF102A22),
                                    Color(0xFF0C1814)
                                )
                            )
                        )
                        .border(1.dp, EmeraldPrimary.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column {
                            Text(
                                text = "NET BALANCE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatCurrency(netBalance),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Cash In Summary Pill
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(BgObsidian.copy(alpha = 0.55f))
                                    .border(1.dp, CashInGreen.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowDownward,
                                        contentDescription = null,
                                        tint = CashInGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Cash In",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatCurrency(totalIn),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CashInGreen,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Cash Out Summary Pill
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(BgObsidian.copy(alpha = 0.55f))
                                    .border(1.dp, CashOutRed.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowUpward,
                                        contentDescription = null,
                                        tint = CashOutRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Cash Out",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatCurrency(totalOut),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CashOutRed,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Quick Action Buttons (+ Cash In / - Cash Out)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onQuickCashIn,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = OnEmeraldPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cash In",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onQuickCashOut,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CashOutRed.copy(alpha = 0.16f),
                            contentColor = CashOutRed
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            CashOutRed.copy(alpha = 0.45f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Remove,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cash Out",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Recent Transactions Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Entries",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${transactions.size} total",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            if (transactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(SurfaceGlass)
                            .border(1.dp, BorderGlass, RoundedCornerShape(20.dp))
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.ReceiptLong,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No entries yet",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Tap + Cash In or - Cash Out to record your first entry",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(transactions, key = { it.id }) { tx ->
                    TransactionRowCard(
                        transaction = tx,
                        onClick = { editingTransaction = tx }
                    )
                }
            }
        }
    }

    // 4-Dots Hub Bottom Sheet
    if (showHubSheet) {
        ModalBottomSheet(
            onDismissRequest = { showHubSheet = false },
            containerColor = SurfaceGlass,
            contentColor = TextPrimary,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            HisabooHubSheetContent(
                onSelectModule = { route ->
                    showHubSheet = false
                    onNavigate(route)
                }
            )
        }
    }

    // Edit / Delete Transaction Dialog
    editingTransaction?.let { tx ->
        EditTransactionDialog(
            transaction = tx,
            categories = viewModel.categories.filter { it.type == tx.type },
            onDismiss = { editingTransaction = null },
            onSave = { updated ->
                viewModel.updateTransaction(updated)
                editingTransaction = null
            },
            onDelete = {
                viewModel.deleteTransaction(tx.id)
                editingTransaction = null
            }
        )
    }
}

@Composable
private fun TransactionRowCard(
    transaction: Transaction,
    onClick: () -> Unit
) {
    val isCashIn = transaction.type == TransactionType.CASH_IN
    val accentColor = if (isCashIn) CashInGreen else CashOutRed

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceGlass)
            .border(1.dp, BorderGlass, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCashIn) Icons.Rounded.SouthWest else Icons.Rounded.NorthEast,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.categoryName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (transaction.note.isNotBlank()) {
                    Text(
                        text = transaction.note,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = transaction.date,
                    fontSize = 11.sp,
                    color = TextSecondary.copy(alpha = 0.75f)
                )
            }
        }

        Text(
            text = (if (isCashIn) "+ " else "- ") + formatCurrency(transaction.amount),
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = accentColor
        )
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
    onSelectModule: (String) -> Unit
) {
    val modules = listOf(
        HubModuleItem("Monthly Report", Icons.Rounded.Assessment, EmeraldPrimary, HisabooRoutes.REPORT),
        HubModuleItem("Search", Icons.Rounded.Search, BlueAccent, HisabooRoutes.SEARCH),
        HubModuleItem("Planned Payments", Icons.Rounded.EventRepeat, AmberAccent, HisabooRoutes.PLANNED_PAYMENTS),
        HubModuleItem("Reminders", Icons.Rounded.NotificationsActive, CashOutRed, HisabooRoutes.REMINDERS),
        HubModuleItem("Budget", Icons.Rounded.PieChart, EmeraldPrimary, HisabooRoutes.BUDGET),
        HubModuleItem("Utility Bills", Icons.Rounded.Bolt, AmberAccent, HisabooRoutes.UTILITY_BILLS),
        HubModuleItem("Family", Icons.Rounded.Groups, BlueAccent, HisabooRoutes.FAMILY),
        HubModuleItem("Backup / Export", Icons.Rounded.CloudUpload, EmeraldPrimary, HisabooRoutes.BACKUP)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hisaboo Hub",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            IconButton(onClick = { onSelectModule(HisabooRoutes.CHANGE_PIN) }) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Change PIN",
                    tint = TextSecondary
                )
            }
        }

        // Clean 4x2 Grid (8 items)
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            modules.chunked(4).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { item ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(BgObsidian.copy(alpha = 0.65f))
                                .border(1.dp, BorderGlass, RoundedCornerShape(16.dp))
                                .clickable { onSelectModule(item.route) }
                                .padding(vertical = 14.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(item.tint.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = item.tint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = item.title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}
