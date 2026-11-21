package com.mianzahid.expenseentry.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mianzahid.expenseentry.model.*
import com.mianzahid.expenseentry.ui.components.*
import com.mianzahid.expenseentry.ui.theme.*
import com.mianzahid.expenseentry.viewmodel.HisabooViewModel

@Composable
fun MonthlyReportScreen(
    viewModel: HisabooViewModel,
    onBack: () -> Unit
) {
    val sixMonths = listOf("Jun", "Jul", "Aug", "Sep", "Oct", "Nov")
    val permanentCategories = listOf(
        "Bills" to TransactionType.CASH_OUT,
        "Food" to TransactionType.CASH_OUT,
        "Other Expense" to TransactionType.CASH_OUT,
        "Rent" to TransactionType.CASH_OUT,
        "Shopping" to TransactionType.CASH_OUT,
        "Transport" to TransactionType.CASH_OUT,
        "Business" to TransactionType.CASH_IN,
        "Other Income" to TransactionType.CASH_IN,
        "Salary" to TransactionType.CASH_IN
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgObsidian)
    ) {
        HisabooSubTopBar(title = "Monthly Report", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(HeroWalletGradient)
                        .border(1.dp, HeroCardBorder, RoundedCornerShape(26.dp))
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "THIS MONTH • BALANCE IN HAND",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        color = Color(0xFFD1FAE5)
                    )
                    Text(
                        text = formatRs(viewModel.balanceInHand),
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(Color(0x6606231B))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.ArrowDownward, null, tint = CashInGreen, modifier = Modifier.size(16.dp))
                            Text(
                                text = "IN: ${formatRs(viewModel.totalCashIn)}",
                                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                                color = CashInGreen
                            )
                        }
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(Color(0x6606231B))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.ArrowUpward, null, tint = CashOutRed, modifier = Modifier.size(16.dp))
                            Text(
                                text = "OUT: ${formatRs(viewModel.totalCashOut)}",
                                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                                color = CashOutRed
                            )
                        }
                    }
                }
            }

            item {
                GlassCard(cornerRadius = 22.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "6 Month Overview",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(CashInGreen))
                                Text("In", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(CashOutRed))
                                Text("Out", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        sixMonths.forEach { month ->
                            val isNov = month == "Nov"
                            val mIn = if (isNov) viewModel.totalCashIn else 0.0
                            val mOut = if (isNov) viewModel.totalCashOut else 0.0
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = month,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isNov) EmeraldPrimary else TextSecondary,
                                    modifier = Modifier.width(34.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .background(SurfaceGlass)
                                ) {
                                    if (mIn > 0 || mOut > 0) {
                                        val ratio = (mIn / (mIn + mOut)).toFloat().coerceIn(0.1f, 1f)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(ratio)
                                                .fillMaxHeight()
                                                .clip(CircleShape)
                                                .background(CashInGreen)
                                        )
                                    }
                                }
                                Text(
                                    text = "${formatRs(mIn)} / ${formatRs(mOut)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            item {
                GlassCard(cornerRadius = 22.dp) {
                    Text(
                        text = "Permanent Record",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        permanentCategories.forEach { (name, type) ->
                            val matching = viewModel.transactions.filter {
                                it.categoryName.equals(name, ignoreCase = true)
                            }
                            val total = matching.sumOf { it.amount }
                            val (icon, tint) = getCategoryVisuals(name, type)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    TintedIconBadge(icon = icon, tint = tint, size = 34.dp, iconSize = 17.dp)
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextPrimary
                                    )
                                }
                                Text(
                                    text = "${matching.size} • ${formatRs(total)}",
                                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                    color = if (type == TransactionType.CASH_IN) CashInGreen else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetScreen(
    viewModel: HisabooViewModel,
    onBack: () -> Unit
) {
    val budgetCategories = listOf("Bills", "Food", "Other Expense", "Rent", "Shopping", "Transport")
    var editingCategory by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgObsidian)
    ) {
        HisabooSubTopBar(
            title = "Budget",
            subtitle = "This month",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(budgetCategories) { catName ->
                val catObj = viewModel.categories.find { it.name.equals(catName, ignoreCase = true) }
                val catKey = catObj?.id ?: catName.lowercase()
                val spent = viewModel.transactions
                    .filter { it.categoryName.equals(catName, ignoreCase = true) }
                    .sumOf { it.amount }
                val budgetLimit = viewModel.categoryBudgets[catKey]
                val (icon, tint) = getCategoryVisuals(catName, TransactionType.CASH_OUT)
                val progress = if (budgetLimit != null && budgetLimit > 0) {
                    (spent / budgetLimit).toFloat().coerceIn(0f, 1f)
                } else 0f

                GlassCard(cornerRadius = 22.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            TintedIconBadge(icon = icon, tint = tint, size = 42.dp)
                            Column {
                                Text(
                                    text = catName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                val statusText = if (budgetLimit == null) {
                                    "Total expenses: ${formatRs(spent)} • No budget set"
                                } else {
                                    "Total expenses: ${formatRs(spent)} / ${formatRs(budgetLimit)}"
                                }
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                    color = TextSecondary
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = { editingCategory = catName },
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = OnEmeraldPrimary
                                ),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(
                                    text = "Set Budget",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = { viewModel.setBudgetForCategory(catKey, 0.0) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Clear Budget",
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(SurfaceGlass)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(if (progress >= 0.9f) CashOutRed else EmeraldPrimary)
                        )
                    }
                }
            }
        }
    }

    editingCategory?.let { catName ->
        val catObj = viewModel.categories.find { it.name.equals(catName, ignoreCase = true) }
        val catKey = catObj?.id ?: catName.lowercase()
        var budgetText by remember {
            mutableStateOf(viewModel.categoryBudgets[catKey]?.toInt()?.toString() ?: "")
        }

        Dialog(onDismissRequest = { editingCategory = null }) {
            GlassCard(cornerRadius = 24.dp) {
                Text("Set Monthly Budget: $catName", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(14.dp))
                HisabooTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it },
                    label = "Budget limit (Rs)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { editingCategory = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Button(
                        onClick = {
                            val amt = budgetText.toDoubleOrNull() ?: 0.0
                            viewModel.setBudgetForCategory(catKey, amt)
                            editingCategory = null
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = OnEmeraldPrimary
                        )
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
