package com.mianzahid.expenseentry.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Sync
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
import com.mianzahid.expenseentry.ui.components.GlassCard
import com.mianzahid.expenseentry.ui.components.HisabooTextField
import com.mianzahid.expenseentry.ui.components.PillSegmentedSwitcher
import com.mianzahid.expenseentry.ui.theme.*
import com.mianzahid.expenseentry.viewmodel.HisabooViewModel
import java.util.Locale

@Composable
fun ToolsScreen(viewModel: HisabooViewModel) {
    var selectedToolIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgObsidian)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Financial Tools",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )

        PillSegmentedSwitcher(
            options = listOf("Calculator", "Currency", "Units"),
            selectedIndex = selectedToolIndex,
            onSelect = { selectedToolIndex = it }
        )

        when (selectedToolIndex) {
            0 -> StandardCalculatorSection()
            1 -> CurrencyConverterSection(viewModel = viewModel)
            2 -> UnitsConverterSection()
        }
    }
}

@Composable
private fun StandardCalculatorSection() {
    var displayValue by remember { mutableStateOf("0") }
    var firstOperand by remember { mutableStateOf<Double?>(null) }
    var pendingOperator by remember { mutableStateOf<String?>(null) }
    var resetOnNextDigit by remember { mutableStateOf(false) }

    fun formatCalcNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.4f", value).trimEnd('0').trimEnd('.')
        }
    }

    fun onKeyPress(key: String) {
        when (key) {
            "C" -> {
                displayValue = "0"
                firstOperand = null
                pendingOperator = null
                resetOnNextDigit = false
            }
            "+", "-", "*", "/" -> {
                firstOperand = displayValue.toDoubleOrNull() ?: 0.0
                pendingOperator = key
                resetOnNextDigit = true
            }
            "=" -> {
                val op = pendingOperator ?: return
                val first = firstOperand ?: return
                val second = displayValue.toDoubleOrNull() ?: 0.0
                val result = when (op) {
                    "+" -> first + second
                    "-" -> first - second
                    "*" -> first * second
                    "/" -> if (second != 0.0) first / second else 0.0
                    else -> second
                }
                displayValue = formatCalcNumber(result)
                firstOperand = null
                pendingOperator = null
                resetOnNextDigit = true
            }
            "." -> {
                if (resetOnNextDigit) {
                    displayValue = "0."
                    resetOnNextDigit = false
                } else if (!displayValue.contains(".")) {
                    displayValue += "."
                }
            }
            else -> {
                if (resetOnNextDigit || displayValue == "0") {
                    displayValue = key
                    resetOnNextDigit = false
                } else if (displayValue.length < 14) {
                    displayValue += key
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard(
            modifier = Modifier.height(115.dp),
            cornerRadius = 22.dp,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STANDARD",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = EmeraldPrimary
                )
                if (pendingOperator != null && firstOperand != null) {
                    Text(
                        text = "${formatCalcNumber(firstOperand!!)}$pendingOperator",
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = displayValue,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.End,
                maxLines = 1,
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.fillMaxWidth()
            )
        }

        val rows = listOf(
            listOf("7", "8", "9", "/"),
            listOf("4", "5", "6", "*"),
            listOf("1", "2", "3", "-"),
            listOf("0", ".", "C", "+")
        )

        rows.forEach { rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowKeys.forEach { key ->
                    val isOperator = key in listOf("/", "*", "-", "+")
                    val isClear = key == "C"

                    val bgColor = when {
                        isClear -> CashOutTint
                        isOperator -> EmeraldGlowTint
                        else -> SurfaceCard
                    }
                    val borderColor = when {
                        isClear -> CashOutRed.copy(alpha = 0.45f)
                        isOperator -> EmeraldPrimary.copy(alpha = 0.45f)
                        else -> BorderGlass
                    }
                    val textColor = when {
                        isClear -> CashOutRed
                        isOperator -> EmeraldPrimary
                        else -> TextPrimary
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(62.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(bgColor)
                            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
                            .clickable { onKeyPress(key) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum")
                        )
                    }
                }
            }
        }

        Button(
            onClick = { onKeyPress("=") },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldPrimary,
                contentColor = OnEmeraldPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "=",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun CurrencyConverterSection(viewModel: HisabooViewModel) {
    val usdToPkrRate = viewModel.usdToPkrRate
    var isUsdToPkr by remember { mutableStateOf(true) }
    var inputAmount by remember { mutableStateOf("1") }
    var showEditRateDialog by remember { mutableStateOf(false) }

    val fromCode = if (isUsdToPkr) "USD" else "PKR"
    val toCode = if (isUsdToPkr) "PKR" else "USD"
    val numericInput = inputAmount.toDoubleOrNull() ?: 0.0
    val converted = if (isUsdToPkr) {
        numericInput * usdToPkrRate
    } else {
        if (usdToPkrRate > 0) numericInput / usdToPkrRate else 0.0
    }
    val formattedResult = String.format(Locale.US, "%,.2f %s", converted, toCode)
    val isLive = viewModel.rateStatusText.startsWith("Live updated")

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isLive) CashInGreen else AmberAccent)
            )
            Text(
                text = viewModel.rateStatusText,
                style = MaterialTheme.typography.labelMedium,
                color = if (isLive) CashInGreen else TextSecondary
            )
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassCard(cornerRadius = 22.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FROM ($fromCode)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        Text(
                            text = "1 USD = ${String.format(Locale.US, "%.2f", usdToPkrRate)} PKR",
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    HisabooTextField(
                        value = inputAmount,
                        onValueChange = { inputAmount = it },
                        label = "Enter $fromCode amount",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }

                GlassCard(
                    cornerRadius = 22.dp,
                    containerColor = SurfaceGlass
                ) {
                    Text(
                        text = "CONVERTED AMOUNT",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formattedResult,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CashInGreen,
                        style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum")
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(EmeraldPrimary)
                    .border(3.dp, BgObsidian, CircleShape)
                    .clickable { isUsdToPkr = !isUsdToPkr },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SwapVert,
                    contentDescription = "Swap Currencies",
                    tint = OnEmeraldPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { viewModel.fetchLiveUsdRate() },
                enabled = !viewModel.isFetchingRate,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = OnEmeraldPrimary
                ),
                modifier = Modifier
                    .weight(1.35f)
                    .height(48.dp)
            ) {
                if (viewModel.isFetchingRate) {
                    CircularProgressIndicator(
                        color = OnEmeraldPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Updating...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                } else {
                    Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Update Rates Now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            OutlinedButton(
                onClick = { showEditRateDialog = true },
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = SurfaceGlass,
                    contentColor = TextPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Edit Rate", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }

    if (showEditRateDialog) {
        var rateInput by remember { mutableStateOf(String.format(Locale.US, "%.2f", usdToPkrRate)) }
        Dialog(onDismissRequest = { showEditRateDialog = false }) {
            GlassCard(cornerRadius = 24.dp) {
                Text("Edit USD to PKR Rate", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))
                HisabooTextField(
                    value = rateInput,
                    onValueChange = { rateInput = it },
                    label = "PKR per 1 USD",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showEditRateDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Button(
                        onClick = {
                            rateInput.toDoubleOrNull()?.let {
                                viewModel.updateUsdRate(it)
                                viewModel.rateStatusText = "Manual rate set"
                            }
                            showEditRateDialog = false
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

private data class UnitOption(
    val label: String,
    val symbol: String,
    val toBaseFactor: Double
)

@Composable
private fun UnitsConverterSection() {
    val categoryNames = listOf(
        "Length",
        "Weight",
        "Gold",
        "Area",
        "Volume",
        "Temperature",
        "Speed",
        "Data",
        "Time"
    )

    val unitCatalog: Map<String, List<UnitOption>> = remember {
        mapOf(
            // Base: Meter (m)
            "Length" to listOf(
                UnitOption("Meter (m)", "m", 1.0),
                UnitOption("Kilometer (km)", "km", 1000.0),
                UnitOption("Centimeter (cm)", "cm", 0.01),
                UnitOption("Millimeter (mm)", "mm", 0.001),
                UnitOption("Inch (in)", "in", 0.0254),
                UnitOption("Foot (ft)", "ft", 0.3048),
                UnitOption("Yard / Gaz (yd)", "yd", 0.9144),
                UnitOption("Mile (mi)", "mi", 1609.344)
            ),
            // Base: Kilogram (kg)
            "Weight" to listOf(
                UnitOption("Kilogram (kg)", "kg", 1.0),
                UnitOption("Gram (g)", "g", 0.001),
                UnitOption("Milligram (mg)", "mg", 0.000001),
                UnitOption("Maund / Mann (40 kg)", "maund", 40.0),
                UnitOption("Pound (lb)", "lb", 0.45359237),
                UnitOption("Ounce (oz)", "oz", 0.02834952),
                UnitOption("Metric Ton (t)", "ton", 1000.0)
            ),
            // Base: Gram (g) — Pakistan Sarafa Gold Standard (1 Tola = 11.664g = 12 Masha = 96 Ratti)
            "Gold" to listOf(
                UnitOption("Tola (11.664 g)", "tola", 11.6638038),
                UnitOption("Gram (g)", "g", 1.0),
                UnitOption("Masha (1/12 Tola)", "masha", 0.97198365),
                UnitOption("Ratti (1/96 Tola)", "ratti",
