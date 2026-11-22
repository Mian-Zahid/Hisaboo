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
    val isUrdu = viewModel.appLanguage == "اردو"
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
        HisabooSubTopBar(
            title = if (isUrdu) "طے شدہ ادائیگیاں" else "Planned Payments",
            onBack = onBack
        )

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
                        label = if (isUrdu) "رقم" else "Amount",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HisabooTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = if (isUrdu) "تفصیل" else "Note"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HisabooTextField(
                        value = dayDate,
                        onValueChange = { dayDate = it },
                        label = if (isUrdu) "تاریخ (1-28)" else "Date (1-28)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isUrdu) "قسم" else "Category",
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
                        Text(if (isUrdu) "محفوظ کریں" else "Save", fontWeight = FontWeight.Bold)
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
                                "${item.categoryName} • Day ${item.dayOfMonth}",
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
    val isUrdu = viewModel.appLanguage == "اردو"
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
        HisabooSubTopBar(title = if (isUrdu) "تلاش" else "Search", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = {
                    Text(
                        if (isUrdu) "تفصیل یا قسم تلاش کریں..." else "Search note or category...",
                        color = TextSecondary
                    )
                },
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
                            text = if (query.isBlank()) {
                                if (isUrdu) "اپنا کھاتہ تلاش کریں" else "Search your ledger"
                            } else {
                                if (isUrdu) "کوئی ریکارڈ نہیں ملا" else "No matching records"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isUrdu) {
                                "فوری تلاش کے لیے تفصیل یا قسم لکھیں"
                            } else {
                                "Type a note or category name to filter entries instantly"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
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
    val isUrdu = viewModel.appLanguage == "اردو"
    var title by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgObsidian)
    ) {
        HisabooSubTopBar(title = if (isUrdu) "یاد دہانیاں" else "Reminders", onBack = onBack)

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
                        label = if (isUrdu) "عنوان" else "Title"
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
                        Text(if (isUrdu) "یاد دہانی شامل کریں" else "Add Reminder", fontWeight = FontWeight.Bold)
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
    val isUrdu = viewModel.appLanguage == "اردو"
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
        HisabooSubTopBar(
            title = if (isUrdu) "قرض کھاتہ" else "Loan Ledger",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
                            text = if (isUrdu) "باقی لینے ہیں" else "TO RECEIVE",
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
                            text = if (isUrdu) "باقی دینے ہیں" else "TO RETURN",
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

            item {
                GlassCard(cornerRadius = 22.dp) {
                    HisabooTextField(
                        value = person,
                        onValueChange = { person = it },
                        label = if (isUrdu) "فرد کا نام" else "Person Name"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HisabooTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = if (isUrdu) "ابتدائی رقم (روپے)" else "Initial Amount (Rs)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    PillSegmentedSwitcher(
                        options = if (isUrdu) {
                            listOf("وصول کرنے ہیں", "واپس کرنے ہیں")
                        } else {
                            listOf("To receive", "To return")
                        },
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
                        Text(
                            text = if (isUrdu) "کھاتہ میں شامل کریں" else "Add Person",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (viewModel.loans.isNotEmpty()) {
                item {
                    Text(
                        text = if (isUrdu) {
                            "تفصیل اور قسطیں درج کرنے کے لیے کسی بھی فرد پر ٹیپ کریں"
                        } else {
                            "Tap any person to open ledger & add installments"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }

            items(viewModel.loans, key = { it.id }) { loan ->
                val accent = if (loan.isToReceive) CashInGreen else CashOutRed
                val tag = if (loan.isToReceive) {
                    if (isUrdu) "وصول کرنے ہیں" else "To receive"
                } else {
                    if (isUrdu) "واپس کرنے ہیں" else "To return"
                }
                val settledLabel = if (loan.isToReceive) {
                    if (isUrdu) "وصول شدہ" else "Received"
                } else {
                    if (isUrdu) "ادا شدہ" else "Paid"
                }

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
                                contentDescription = "Open Ledger",
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
                            Text(
                                text = if (isUrdu) "کل رقم" else "Total",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = formatRs(loan.totalLoanAmount),
                                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = settledLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = formatRs(loan.settledAmount),
                                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isUrdu) "باقی" else "Remaining",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
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
                Text(
                    text = if (isUrdu) "${target.person} کو حذف کریں؟" else "Delete ${target.person}?",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isUrdu) {
                        "اس سے ${target.person} اور ان کی تمام قسطوں کا ریکارڈ مستقل طور پر حذف ہو جائے گا۔"
                    } else {
                        "This will permanently delete ${target.person} and all their payment entries from Loan Ledger."
                    },
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
                        Text(if (isUrdu) "منسوخ" else "Cancel", color = TextPrimary)
                    }
                    Button(
                        onClick = {
                            viewModel.deleteLoan(target.id)
                            loanToDelete = null
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CashOutRed,
                            contentColor = OnEmeraldPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isUrdu) "حذف کریں" else "Delete", fontWeight = FontWeight.Bold)
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
    val isUrdu = viewModel.appLanguage == "اردو"
    var entryAmount by remember { mutableStateOf("") }
    var entryNote by remember { mutableStateOf("") }
    var entryDate by remember { mutableStateOf(viewModel.todayFormatted()) }
    var entryModeIndex by remember { mutableIntStateOf(0) }

    val accent = if (loan.isToReceive) CashInGreen else CashOutRed
    val settledTitle = if (loan.isToReceive) {
        if (isUrdu) "کل وصول شدہ" else "Total Received"
    } else {
        if (isUrdu) "کل ادا شدہ" else "Total Returned"
    }
    val modeOptions = if (loan.isToReceive) {
        if (isUrdu) listOf("رقم وصول ہوئی", "مزید قرض دیا (+)") else listOf("Payment Received", "Add More Loan (+)")
    } else {
        if (isUrdu) listOf("رقم واپس کی", "مزید قرض لیا (+)") else listOf("Payment Returned", "Add More Loan (+)")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgObsidian)
    ) {
        HisabooSubTopBar(
            title = if (isUrdu) "${loan.person} • کھاتہ" else "${loan.person} • Ledger",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                GlassCard(cornerRadius = 24.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (loan.isToReceive) {
                                    if (isUrdu) "وصول کرنے ہیں" else "TO RECEIVE"
                                } else {
                                    if (isUrdu) "واپس کرنے ہیں" else "TO RETURN"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accent
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isUrdu) {
                                    "باقی: ${formatRs(loan.remainingAmount)}"
                                } else {
                                    "Remaining: ${formatRs(loan.remainingAmount)}"
                                },
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
                                    text = if (isUrdu) "مکمل" else "SETTLED",
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
                            Text(
                                text = if (isUrdu) "کل قرض" else "Total Loan",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = formatRs(loan.totalLoanAmount),
                                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = settledTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
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

            item {
                GlassCard(cornerRadius = 22.dp) {
                    Text(
                        text = if (isUrdu) "${loan.person} کے لیے نیا اندراج" else "New Entry for ${loan.person}",
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
                        label = if (isUrdu) "رقم (روپے)" else "Amount (Rs)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HisabooTextField(
                        value = entryNote,
                        onValueChange = { entryNote = it },
                        label = if (isUrdu) "تفصیل (مثلاً پہلی قسط، نقد)" else "Note (e.g. 1st installment, Cash)"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HisabooTextField(
                        value = entryDate,
                        onValueChange = { entryDate = it },
                        label = if (isUrdu) "تاریخ (DD/MM/YYYY)" else "Date (DD/MM/YYYY)"
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
                        Text(if (isUrdu) "اندراج محفوظ کریں" else "Save Entry", fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUrdu) "ادائیگیوں کی تاریخ" else "Payment History",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isUrdu) "${loan.payments.size} اندراجات" else "${loan.payments.size} entries",
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
                            text = if (isUrdu) {
                                "ابھی تک کوئی قسط درج نہیں ہوئی۔ ابتدائی رقم: ${formatRs(loan.amount)}"
                            } else {
                                "No installments recorded yet. Initial amount: ${formatRs(loan.amount)}"
                            },
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
                                    text = if (isUrdu) {
                                        "${pay.date} • ${if (pay.isAdditionalLoan) "مزید قرض" else "قسط"}"
                                    } else {
                                        "${pay.date} • ${if (pay.isAdditionalLoan) "Added to Loan" else "Installment"}"
                                    },
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
    val isUrdu = viewModel.appLanguage == "اردو"
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
        HisabooSubTopBar(
            title = if (isUrdu) "یوٹیلیٹی بلز" else "Utility Bills",
            onBack = onBack
        )

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
                        label = if (isUrdu) "عنوان" else "Title"
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
                            text = if (isUrdu) "ادارہ: ${providers[providerIndex]}" else "Provider: ${providers[providerIndex]}",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HisabooTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = if (isUrdu) "رقم" else "Amount",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HisabooTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = if (isUrdu) "آخری تاریخ DD/MM/YYYY" else "Due date DD/MM/YYYY"
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
                        Text(if (isUrdu) "شامل کریں" else "Add", fontWeight = FontWeight.Bold)
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
