package com.mianzahid.expenseentry.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ReceiptLong
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
fun CategoryDetailScreen(
    categoryId: String,
    viewModel: HisabooViewModel,
    onBack: () -> Unit
) {
    val category = viewModel.categories.find { it.id == categoryId }
        ?: CategoryItem(categoryId, "Category", TransactionType.CASH_OUT, "bills")

    val entries = viewModel.transactions.filter { it.categoryId == category.id }
    val totalAmount = entries.sumOf { it.amount }
    val (icon, tint) = getCategoryVisuals(category.name, category.type)
    val subtitleLabel = if (category.type == TransactionType.CASH_IN) "Total received" else "Total expenses"

    var showAddEntryDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgObsidian)
    ) {
        HisabooSubTopBar(
            title = category.name,
            leadingIcon = icon,
            leadingTint = tint,
            onBack = onBack
        )

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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "PERMANENT RECORD",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        color = Color(0xFFD1FAE5)
                    )
                    Text(
                        text = formatRs(totalAmount),
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White
                    )
                    Text(
                        text = "${entries.size} entries • $subtitleLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFA7F3D0)
                    )
                }
            }

            item {
                Button(
                    onClick = { showAddEntryDialog = true },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = OnEmeraldPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+ Add entries",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (entries.isEmpty()) {
                item {
                    GlassCard(
                        cornerRadius = 22.dp,
                        contentPadding = PaddingValues(28.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TintedIconBadge(
                                icon = Icons.Rounded.ReceiptLong,
                                tint = EmeraldPrimary,
                                size = 48.dp,
                                iconSize = 24.dp,
                                isCircle = true
                            )
                            Text(
                                text = "No entries yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Tap + Add entries above to log your first ${category.name} record",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(entries, key = { it.id }) { tx ->
                    TransactionRowCard(
                        entry = tx,
                        onDelete = { viewModel.deleteTransaction(tx.id) }
                    )
                }
            }
        }
    }

    if (showAddEntryDialog) {
        QuickTransactionDialog(
            type = category.type,
            categories = listOf(category),
            fixedCategory = category,
            defaultDate = viewModel.todayFormatted(),
            onDismiss = { showAddEntryDialog = false },
            onSave = { catId, catName, amount, note, date ->
                viewModel.addTransaction(catId, catName, amount, note, date, category.type)
                showAddEntryDialog = false
            }
        )
    }
}

@Composable
fun NewCategoryDialog(
    onDismiss: () -> Unit,
    onAdd: (String, TransactionType) -> Unit
) {
    var categoryName by remember { mutableStateOf("") }
    var selectedTypeIndex by remember { mutableIntStateOf(1) }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            cornerRadius = 24.dp,
            containerColor = SurfaceCard,
            contentPadding = PaddingValues(20.dp)
        ) {
            Text(
                text = "New Category",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            HisabooTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                label = "Category name"
            )

            Spacer(modifier = Modifier.height(14.dp))

            PillSegmentedSwitcher(
                options = listOf("Income", "Expense"),
                selectedIndex = selectedTypeIndex,
                onSelect = { selectedTypeIndex = it },
                activeColor = if (selectedTypeIndex == 0) EmeraldPrimary else CashOutRed,
                activeTextColor = OnEmeraldPrimary
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val type = if (selectedTypeIndex == 0) TransactionType.CASH_IN else TransactionType.CASH_OUT
                        onAdd(categoryName, type)
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = OnEmeraldPrimary
                    )
                ) {
                    Text("Add", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun QuickTransactionDialog(
    type: TransactionType,
    categories: List<CategoryItem>,
    fixedCategory: CategoryItem? = null,
    defaultDate: String = "21/11/2026",
    onDismiss: () -> Unit,
    onSave: (String, String, Double, String, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(defaultDate) }
    var selectedCategory by remember {
        mutableStateOf(fixedCategory ?: categories.firstOrNull())
    }

    val accent = if (type == TransactionType.CASH_IN) EmeraldPrimary else CashOutRed
    val title = if (type == TransactionType.CASH_IN) "Add Cash In" else "Add Cash Out"

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            cornerRadius = 24.dp,
            containerColor = SurfaceCard,
            contentPadding = PaddingValues(20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            HisabooTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = "Amount (Rs)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Spacer(modifier = Modifier.height(10.dp))

            HisabooTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = "Note (optional)"
            )

            Spacer(modifier = Modifier.height(10.dp))

            HisabooTextField(
                value = dateText,
                onValueChange = { dateText = it },
                label = "Date (DD/MM/YYYY)"
            )

            if (fixedCategory == null && categories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Select Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val active = selectedCategory?.id == cat.id
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (active) accent else SurfaceGlass)
                                .border(1.dp, if (active) accent else BorderGlass, CircleShape)
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (active) OnEmeraldPrimary else TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        val cat = selectedCategory ?: CategoryItem("default", "General", type, "bills")
                        onSave(cat.id, cat.name, amt, noteText, dateText)
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = OnEmeraldPrimary
                    )
                ) {
                    Text("Save Entry", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
