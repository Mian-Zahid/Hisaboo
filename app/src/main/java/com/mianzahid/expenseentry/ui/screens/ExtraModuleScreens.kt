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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mianzahid.expenseentry.model.LoanItem
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
    var selectedLoanId by remember { mutableStateOf<String?>(null) }
    val selectedLoan = viewModel.loans.find { it.id == selectedLoanId }

    if (selectedLoan != null) {
        LoanPersonDetailScreen(
            loan = selectedLoan,
            viewModel = viewModel,
            onBack = { selectedLoanId = null }
        )
        return
    }

    var person by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var loanTypeIndex by remember { mutableIntStateOf(0) }
    var loanToDelete by remember { mutableStateOf<LoanItem?>(null) }

    val totalToReceiveRemaining = viewModel.loans.filter { it.isToReceive }.sumOf { it.remainingAmount }
    val totalToReturnRemaining = viewModel.loans.filter { !it.isToReceive }.sumOf { it.remainingAmount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgObsidian)
    ) {
        HisabooSubTopBar(title = "Loan Khata", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Overall Remaining Summary Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        cornerRadius = 20.dp,
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Text(
                            text = "BAQI LENE HAIN",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CashInGreen
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatRs(totalToReceiveRemaining),
                            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.ExtraBold,
                            color = CashInGreen
                        )
                    }

                    GlassCard(
                        modifier = Modifier.weight(1f),
                        cornerRadius = 20.dp,
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Text(
                            text = "BAQI DENE HAIN",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CashOutRed
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatRs(totalToReturnRemaining),
                            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.ExtraBold,
                            color = CashOutRed
                        )
                    }
                }
            }

            // Add New Person Form
            item {
                GlassCard(cornerRadius = 22.dp) {
                    HisabooTextField(
                        value = person,
                        onValueChange = { person = it },
                        label = "Person Name"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HisabooTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = "Initial Amount (Rs)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    PillSegmentedSwitcher(
                        options = listOf("To receive (Lena)", "To return (Dena)"),
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
                        Text("Add Person to Khata", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (viewModel.loans.isNotEmpty()) {
                item {
                    Text(
                        text = "Tap any person to open Khata & add installments",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }

            items(viewModel.loans, key = { it.id }) { loan ->
                val accent = if (loan.isToReceive) CashInGreen else CashOutRed
                val tag = if (loan.isToReceive) "To receive (Lena)" else "To return (Dena)"
                val settledLabel = if (loan.isToReceive) "Received" else "Paid"

                GlassCard(
                    cornerRadius = 20.dp,
                    modifier = Modifier.clickable { selectedLoanId = loan.id }
                ) {
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
                            TintedIconBadge(
                                icon = Icons.Rounded.Person,
                                tint = accent,
                                size = 42.dp,
                                isCircle = true
                            )
                            Column {
                                Text(
                                    text = loan.person,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = accent
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { loanToDelete = loan },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete Person",
                                    tint = CashOutRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = "Open Khata",
                                tint = TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceGlass)
                            .border(1.dp, BorderGlass, RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                text = formatRs(loan.totalLoanAmount),
                                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(settledLabel, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                text = formatRs(loan.settledAmount),
                                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Remaining", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                text = formatRs(loan.remainingAmount),
                                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.ExtraBold,
                                color = if (loan.remainingAmount == 0.0) CashInGreen else accent
                            )
                        }
                    }
                }
            }
        }
    }

    loanToDelete?.let { target ->
        Dialog(onDismissRequest = { loanToDelete = null }) {
            GlassCard(cornerRadius = 24.dp) {
                Text("Delete ${target.person}?", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This will permanently delete ${target.person} and all their payment entries from Loan Khata.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { loanToDelete = null },
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = TextPrimary)
                    }
                    Button(
                        onClick = {
                            viewModel.deleteLoan(target.id)
                            loanToDelete = null
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CashOutRed,
                            contentColor = TextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoanPersonDetailScreen(
    loan: LoanItem,
    viewModel: HisabooViewModel,
    onBack: () -> Unit
) {
    var entryAmount by remember { mutableStateOf("") }
    var entryNote by remember { mutableStateOf("") }
    var entryDate by remember { mutableStateOf(viewModel.todayFormatted()) }
    var entryModeIndex by remember { mutableIntStateOf(0) } // 0 = Payment Received/Returned, 1 = Add More Loan

    val accent = if (loan.isToReceive) CashInGreen else CashOutRed
    val settledTitle = if (loan.isToReceive) "Total Received" else "Total Returned"
    val modeOptions = if (loan.isToReceive) {
        listOf("Wapas Mila (Received)", "Mazeed Diya (+Loan)")
    } else {
        listOf("Wapas Diya (Paid)", "Mazeed Liya (+Loan)")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgObsidian)
    ) {
        HisabooSubTopBar(title = "${loan.person} • Khata", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Person Summary Card
            item {
                GlassCard(cornerRadius = 24.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (loan.isToReceive) "LENA HAI (TO RECEIVE)" else "DENA HAI (TO RETURN)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accent
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Remaining: ${formatRs(loan.remainingAmount)}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (loan.remainingAmount == 0.0) CashInGreen else accent,
                                style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum")
                            )
                        }
                        if (loan.remainingAmount == 0.0) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(CashInTint)
                                    .border(1.dp, CashInGreen.copy(alpha = 0.4f), CircleShape)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "CLEAR",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CashInGreen
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceGlass)
                            .border(1.dp, BorderGlass, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Loan", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                text = formatRs(loan.totalLoanAmount),
                                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(settledTitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                text = formatRs(loan.settledAmount),
                                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }
            }

            // Add Installment / Payment Entry Form
            item {
                GlassCard(cornerRadius = 22.dp) {
                    Text(
                        text = "New Entry for ${loan.person}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PillSegmentedSwitcher(
                        options = modeOptions,
                        selectedIndex = entryModeIndex,
                        onSelect = { entryModeIndex = it },
                        activeColor = if (entryModeIndex == 0) EmeraldPrimary else AmberAccent,
                        activeTextColor = OnEmeraldPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HisabooTextField(
                        value = entryAmount,
                        onValueChange = { entryAmount = it },
                        label = "Amount (Rs)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HisabooTextField(
                        value = entryNote,
                        onValueChange = { entryNote = it },
                        label = "Note (e.g. 1st installment, Cash, EasyPaisa)"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HisabooTextField(
                        value = entryDate,
                        onValueChange = { entryDate = it },
                        label = "Date (DD/MM/YYYY)"
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val amt = entryAmount.toDoubleOrNull() ?: 0.0
                            viewModel.addLoanPayment(
                                loanId = loan.id,
                                amount = amt,
                                note = entryNote,
                                date = entryDate,
                                isAdditionalLoan = entryModeIndex == 1
                            )
                            entryAmount = ""
                            entryNote = ""
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
                        Text("Save Entry", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // History Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Payment History",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "${loan.payments.size} entries",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }

            if (loan.payments.isEmpty()) {
                item {
                    GlassCard(
                        cornerRadius = 20.dp,
                        contentPadding = PaddingValues(24.dp)
                    ) {
                        Text(
                            text = "No installments recorded yet. Initial amount: ${formatRs(loan.amount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                items(loan.payments, key = { it.id }) { pay ->
                    val entryColor = if (pay.isAdditionalLoan) AmberAccent else EmeraldPrimary
                    val prefix = if (pay.isAdditionalLoan) "+" else "-"

                    GlassCard(
                        cornerRadius = 18.dp,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pay.note,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${pay.date} • ${if (pay.isAdditionalLoan) "Added to Loan" else "Installment"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "$prefix${formatRs(pay.amount)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.Bold,
                                    color = entryColor
                                )
                                IconButton(
                                    onClick = { viewModel.deleteLoanPayment(loan.id, pay.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Delete Entry",
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
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
