package com.mianzahid.expenseentry.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.mianzahid.expenseentry.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

enum class TransactionType {
    CASH_IN, CASH_OUT
}

data class CategoryItem(
    val id: String,
    val name: String,
    val type: TransactionType,
    val iconName: String,
    val isDefault: Boolean = true
)

data class TransactionEntry(
    val id: String,
    val categoryId: String,
    val categoryName: String,
    val amount: Double,
    val note: String,
    val date: String,
    val type: TransactionType
)

data class PlannedPaymentItem(
    val id: String,
    val amount: Double,
    val note: String,
    val dayOfMonth: Int,
    val categoryName: String
)

data class ReminderItem(
    val id: String,
    val title: String,
    val dueDate: String,
    val isCompleted: Boolean = false
)

data class LoanPaymentRecord(
    val id: String,
    val amount: Double,
    val note: String,
    val date: String,
    val isAdditionalLoan: Boolean = false // false = payment returned/received, true = more loan added
)

data class LoanItem(
    val id: String,
    val person: String,
    val amount: Double,
    val isToReceive: Boolean, // true = To receive, false = To return
    val payments: List<LoanPaymentRecord> = emptyList()
) {
    val totalLoanAmount: Double
        get() = amount + payments.filter { it.isAdditionalLoan }.sumOf { it.amount }

    val settledAmount: Double
        get() = payments.filter { !it.isAdditionalLoan }.sumOf { it.amount }

    val remainingAmount: Double
        get() = (totalLoanAmount - settledAmount).coerceAtLeast(0.0)
}

data class UtilityBillItem(
    val id: String,
    val title: String,
    val provider: String,
    val amount: Double,
    val dueDate: String
)

data class FamilyMemberItem(
    val id: String,
    val name: String,
    val pin: String
)

enum class HadithLanguage(val label: String) {
    URDU("اردو"),
    ENGLISH("English"),
    ARABIC("العربية")
}

data class DailyHadith(
    val arabic: String,
    val urdu: String,
    val english: String,
    val reference: String
)

fun formatRs(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
        minimumFractionDigits = 0
    }
    return "Rs ${formatter.format(amount)}"
}

fun getCategoryVisuals(name: String, type: TransactionType): Pair<ImageVector, Color> {
    return when (name.lowercase()) {
        "business" -> Icons.Rounded.Storefront to EmeraldPrimary
        "other income" -> Icons.Rounded.AccountBalanceWallet to CashInGreen
        "salary" -> Icons.Rounded.Payments to BlueAccent
        "bills" -> Icons.Rounded.ReceiptLong to AmberAccent
        "food" -> Icons.Rounded.Restaurant to CashOutRed
        "other expense" -> Icons.Rounded.ShoppingBag to TextSecondary
        "rent" -> Icons.Rounded.HomeWork to BlueAccent
        "shopping" -> Icons.Rounded.LocalMall to EmeraldPrimary
        "transport" -> Icons.Rounded.DirectionsCar to AmberAccent
        else -> if (type == TransactionType.CASH_IN) {
            Icons.Rounded.TrendingUp to CashInGreen
        } else {
            Icons.Rounded.TrendingDown to CashOutRed
        }
    }
}
