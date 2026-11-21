package com.mianzahid.expenseentry.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mianzahid.expenseentry.model.formatRs
import com.mianzahid.expenseentry.ui.components.*
import com.mianzahid.expenseentry.ui.theme.*
import com.mianzahid.expenseentry.viewmodel.HisabooViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlannedPaymentsScreen(
    viewModel: HisabooViewModel,
    onBack: () -> Unit
) {
    val categoryPills = listOf(
        "Bills", "Food", "Other Expense", "Rent",
        "Shopping", "Transport", "Business", "Other Income", "Salary"
    )
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var dayDate by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf("Bills") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgObsidian)
    ) {
        HisabooSubTopBar(title = "Planned Payments", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                GlassCard(cornerRadius = 22.dp) {
                    HisabooTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = "Amount",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HisabooTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = "Note"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HisabooTextField(
                        value = dayDate,
                        onValueChange = { dayDate = it },
                        label = "Date (1-28)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryPills.forEach { cat ->
                            val isSelected = selectedCat == cat
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isSelected) EmeraldPrimary else SurfaceGlass)
                                    .border(1.dp, if (isSelected) EmeraldPrimary else BorderGlass, CircleShape)
                                    .clickable { selectedCat = cat }
                                    .padding(horizontal = 13.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) OnEmeraldPrimary else TextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull() ?: 0.0
                            val day = dayDate.toIntOrNull() ?: 1
                            viewModel.addPlannedPayment(amt, note, day, selectedCat)
                            amount = ""
                            note = ""
                            dayDate = ""
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = OnEmeraldPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(viewModel.plannedPayments, key = { it.id }) { item ->
                GlassCard(cornerRadius = 20.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(item.note, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${item.categoryName} • Day ${item.dayOfMonth} of month",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                        Text(
                            text = formatRs(item.amount),
                            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                            color = AmberAccent
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchScreen(
    viewModel: HisabooViewModel,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val results = if (query.isBlank()) {
        emptyList()
    } else {
        viewModel.transactions.filter {
            it.note.contains(query, ignoreCase = true) ||
                it.categoryName.contains(query, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgObsidian)
    ) {
        HisabooSubTopBar(title = "Search", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search note or category...", color = TextSecondary) },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = EmeraldPrimary)
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceGlass,
                    unfocusedContainerColor = SurfaceGlass,
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = BorderGlass,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (results.isEmpty()) {
                GlassCard(
                    cornerRadius = 22.dp,
                    contentPadding = PaddingValues(32.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TintedIconBadge(
                            icon = Icons.Rounded.Search,
                            tint = BlueAccent,
                            size = 48.dp,
                            iconSize = 24.dp,
                            isCircle = true
                        )
                        Text(
                            text = if (query.isBlank()) "Search your ledger" else "No matching records",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Type a note or category name to filter entries instantly",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(results, key = { it.id }) { tx ->
                        TransactionRowCard(
                            entry = tx,
                            onDelete = { viewModel.deleteTransaction(tx.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RemindersScreen(
    viewModel: HisabooViewModel,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgObsidian)
    ) {
        HisabooSubTopBar(title = "Reminders", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                GlassCard(cornerRadius = 22.dp) {
                    HisabooTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = "Title"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HisabooTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        label = "DD/MM/YYYY",
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.CalendarToday,
                                contentDescription = "Date",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.addReminder(title, dateText)
                            title = ""
                            dateText = ""
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = OnEmeraldPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Add Reminder", fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(viewModel.reminders, key = { it.id }) { rem ->
                GlassCard(cornerRadius = 20.dp) {
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
                                icon = Icons.Rounded.NotificationsActive,
                                tint = AmberAccent,
                                size = 38.dp
                            )
                            Column {
                                Text(rem.title, style = MaterialTheme.typography.titleMedium)
                                Text(rem.dueDate, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoanScreen(
    viewModel: HisabooViewModel,
    onBack: () -> Unit
) {
    var person by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var loanTypeIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgObsidian)
    ) {
        HisabooSubTopBar(title = "Loan", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                GlassCard(cornerRadius = 22.dp) {
                    HisabooTextField(
                        value = person,
                        onValueChange = { person = it },
                        label = "Person"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HisabooTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = "Amount",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    PillSegmentedSwitcher(
                        options = listOf("To receive", "To return"),
                        selectedIndex = loanTypeIndex,
                        onSelect = { loanTypeIndex = it },
                        activeColor = if (loanTypeIndex == 0) EmeraldPrimary else CashOutRed,
                        activeTextColor = OnEmeraldPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull() ?: 0.0
                            viewModel.addLoan(person, amt, loanTypeIndex == 0)
                            person = ""
                            amount = ""
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = OnEmeraldPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Add", fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(viewModel.loans, key = { it.id }) { loan ->
                val accent = if (loan.isToReceive) CashInGreen else CashOutRed
                val tag = if (loan.isToReceive) "To receive" else "To return"
                GlassCard(cornerRadius = 20.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(loan.person, style = MaterialTheme.typography.titleMedium)
                            Text(tag, style = MaterialTheme.typography.labelMedium, color = accent)
                        }
                        Text(
                            text = formatRs(loan.amount),
                            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UtilityBillsScreen(
    viewModel: HisabooViewModel,
    onBack: () -> Unit
) {
    val providers = listOf("LESCO", "KE", "MEPCO", "SNGPL", "SSGC", "PTCL")
    var providerIndex by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgObsidian)
    ) {
        HisabooSubTopBar(title = "Utility Bills", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                GlassCard(cornerRadius = 22.dp) {
                    HisabooTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = "Title"
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SurfaceGlass)
                            .border(1.dp, BorderGlass, CircleShape)
                            .clickable { providerIndex = (providerIndex + 1) % providers.size }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Provider: ${providers[providerIndex]}",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HisabooTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = "Amount",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HisabooTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = "Due date DD/MM/YYYY"
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull() ?: 0.0
                            viewModel.addUtilityBill(title, providers[providerIndex], amt, dueDate)
                            title = ""
                            amount = ""
                            dueDate = ""
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = OnEmeraldPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Add", fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(viewModel.utilityBills, key = { it.id }) { bill ->
                GlassCard(cornerRadius = 20.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(bill.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${bill.provider} • Due: ${bill.dueDate}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                        Text(
                            text = formatRs(bill.amount),
                            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            color = AmberAccent
                        )
                    }
                }
            }
        }
    }
}
