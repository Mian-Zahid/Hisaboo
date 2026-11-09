package com.mianzahid.expenseentry

import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

@Composable
fun CategoriesScreen(vm: AppViewModel, onOpen: (Int) -> Unit) {
    val categories by vm.categories.collectAsState()
    val entries by vm.entries.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newType by remember { mutableStateOf("OUT") }
    var confirmDelete by remember { mutableStateOf<Category?>(null) }
    val isAdmin by vm.isAdmin.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(Modifier.fillMaxSize().background(Bg).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(vm.t("categories"), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Button(onClick = { showAdd = true }, colors = ButtonDefaults.buttonColors(containerColor = Green), shape = RoundedCornerShape(20.dp)) { Text(vm.t("newCategory")) }
        }
        Text(vm.t("permanentRecord"), fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(categories) { c ->
                val list = entries.filter { it.categoryId == c.id }
                val total = list.sumOf { it.amount }
                Card(Modifier.clickable { onOpen(c.id) }, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = CardWhite)) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${categoryIcon(c.name)} ${vm.catName(c)}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            if (isAdmin) Text("🗑", modifier = Modifier.clickable { confirmDelete = c })
                        }
                        Text(fmtRs(total), color = if (c.type == "IN") Green else Red, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${list.size} ${vm.t("entries")} →", fontSize = 12.sp, color = Color.Gray)
                        Text(if (c.type == "IN") vm.t("totalReceived") else vm.t("totalExpenses"), fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
    if (showAdd) {
        AlertDialog(onDismissRequest = { showAdd = false }, title = { Text(vm.t("newCategory")) },
            text = {
                Column {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text(vm.t("categoryName")) }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }), modifier = Modifier.fillMaxWidth())
                    Row { RadioButton(selected = newType == "IN", onClick = { keyboardController?.hide(); newType = "IN" }); Text(vm.t("income")); RadioButton(selected = newType == "OUT", onClick = { keyboardController?.hide(); newType = "OUT" }); Text(vm.t("expenseType")) }
                }
            },
            confirmButton = { Button(onClick = { keyboardController?.hide(); if (newName.isNotBlank()) { vm.addCategory(newName.trim(), newType); newName = ""; showAdd = false } }, colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text(vm.t("add")) } },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text(vm.t("cancel")) } })
    }
    if (confirmDelete != null) {
        AlertDialog(onDismissRequest = { confirmDelete = null }, title = { Text(vm.t("confirmDelete")) },
            text = { Text(vm.t("deleteCategoryConfirm")) },
            confirmButton = { Button(onClick = { vm.deleteCategory(confirmDelete!!.id); confirmDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text(vm.t("delete")) } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text(vm.t("cancel")) } })
    }
}

@Composable
fun CategoryDetailScreen(vm: AppViewModel, catId: Int, onBack: () -> Unit) {
    val categories by vm.categories.collectAsState()
    val entries by vm.entries.collectAsState()
    val cat = categories.firstOrNull { it.id == catId } ?: run { onBack(); return }
    val list = entries.filter { it.categoryId == catId }
    val total = list.sumOf { it.amount }
    var showEntry by remember { mutableStateOf(false) }
    var confirmDeleteEntry by remember { mutableStateOf<Entry?>(null) }

    Column(Modifier.fillMaxSize().background(Bg)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            Text("${categoryIcon(cat.name)} ${vm.catName(cat)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            ThreeDotMenu(vm)
        }
        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Green)) {
            Column(Modifier.padding(14.dp)) {
                Text(vm.t("permanentRecord"), color = Color.White, fontSize = 12.sp)
                Text(fmtRs(total), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("${list.size} ${vm.t("entries")} • ${if (cat.type == "IN") vm.t("totalReceived") else vm.t("totalExpenses")}", color = Color.White.copy(alpha = 0.85f))
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { showEntry = true }, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("+ ${vm.t("add")} ${vm.t("entries")}") }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
            if (list.isEmpty()) Text(vm.t("noEntries"), color = Color.Gray)
            list.forEach { e ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CardWhite)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (e.type == "IN") "↓" else "↑", fontSize = 20.sp, color = if (e.type == "IN") Green else Red)
                        Column(Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(if (e.note.isNotBlank()) vm.noteDisplay(e.note) else vm.catName(cat), fontWeight = FontWeight.Bold)
                            Text("${shortDate(e.dateMillis, vm.lang.value)} • ${vm.memberDisplayName(e.memberName)}", fontSize = 12.sp, color = Color.Gray)
                            if (e.receiptUri.isNotEmpty()) Text("📷 ${vm.t("receiptPhoto")}", fontSize = 11.sp, color = Green)
                        }
                        Text((if (e.type == "IN") "+ " else "− ") + fmtRs(e.amount), fontWeight = FontWeight.Bold, color = if (e.type == "IN") Green else Red)
                        TextButton(onClick = { confirmDeleteEntry = e }) { Text(vm.t("delete"), color = Red, fontSize = 12.sp) }
                    }
                }
            }
        }
    }
    if (showEntry) EntrySheet(vm, cat.type, categoryId = catId, onClose = { showEntry = false })
    if (confirmDeleteEntry != null) {
        AlertDialog(onDismissRequest = { confirmDeleteEntry = null }, title = { Text(vm.t("confirmDelete")) },
            text = { Text(fmtRs(confirmDeleteEntry!!.amount)) },
            confirmButton = { Button(onClick = { vm.deleteEntry(confirmDeleteEntry!!.id); confirmDeleteEntry = null }, colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text(vm.t("delete")) } },
            dismissButton = { TextButton(onClick = { confirmDeleteEntry = null }) { Text(vm.t("cancel")) } })
    }
}

// ---------------- Calculator ----------------
@Composable
fun CalculatorScreen(vm: AppViewModel) {
    var tab by remember { mutableStateOf(0) } // 0 calc, 1 currency, 2 units
    Column(Modifier.fillMaxSize().background(Bg).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(vm.t("calculator"), vm.t("currency"), "Units").forEachIndexed { i, label ->
                Button(onClick = { tab = i }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (tab == i) Green else Color.LightGray, contentColor = if (tab == i) Color.White else Color.Black)) { Text(label, fontSize = 12.sp, maxLines = 1) }
            }
        }
        Spacer(Modifier.height(8.dp))
        when (tab) {
            0 -> ArithmeticCalc()
            1 -> CurrencyCalc(vm)
            else -> UnitConverter(vm)
        }
    }
}

@Composable
fun ArithmeticCalc() {
    var display by remember { mutableStateOf("0") }
    var expr by remember { mutableStateOf("") }
    fun press(k: String) {
        when (k) {
            "C" -> { display = "0"; expr = "" }
            "=" -> {
                try {
                    val v = evalExpr(expr.ifBlank { display })
                    display = if (v % 1.0 == 0.0) v.toLong().toString() else "%.2f".format(v)
                    expr = ""
                } catch (_: Exception) { display = "Error" }
            }
            else -> { expr += k; display = expr }
        }
    }
    Column {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardWhite)) {
            Text(display, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }
        Spacer(Modifier.height(8.dp))
        val keys = listOf(listOf("7","8","9","/"), listOf("4","5","6","*"), listOf("1","2","3","-"), listOf("0",".","C","+"), listOf("="))
        keys.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { k ->
                    Button(onClick = { press(k) }, modifier = Modifier.weight(1f).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = if (k == "=") Green else Color.White, contentColor = if (k == "=") Color.White else Color.Black)) { Text(k, fontSize = 20.sp) }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

fun evalExpr(s: String): Double {
    // simple left-to-right with * / precedence via shunting-yard-lite
    val tokens = Regex("(\\d+\\.?\\d*|[+\\-*/])").findAll(s).map { it.value }.toList()
    if (tokens.isEmpty()) return 0.0
    val vals = ArrayDeque<Double>(); val ops = ArrayDeque<String>()
    fun prec(o: String) = if (o == "+" || o == "-") 1 else 2
    fun apply() {
        val b = vals.removeLast(); val a = vals.removeLast(); val o = ops.removeLast()
        vals.addLast(when (o) { "+" -> a + b; "-" -> a - b; "*" -> a * b; "/" -> a / b; else -> b })
    }
    for (t in tokens) {
        if (t[0].isDigit() || t[0] == '.') vals.addLast(t.toDouble())
        else { while (ops.isNotEmpty() && prec(ops.last()) >= prec(t)) apply(); ops.addLast(t) }
    }
    while (ops.isNotEmpty()) apply()
    return vals.lastOrNull() ?: 0.0
}

@Composable
fun CurrencyCalc(vm: AppViewModel) {
    val rates by vm.rates.collectAsState()
    val live by vm.ratesLive.collectAsState()
    val updated by vm.ratesUpdated.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    var amount by remember { mutableStateOf("1000") }
    var from by remember { mutableStateOf("USD") }
    var to by remember { mutableStateOf("PKR") }
    var editMode by remember { mutableStateOf(false) }
    var editCur by remember { mutableStateOf("USD") }
    var editVal by remember { mutableStateOf("") }
    val currencies = listOf("PKR","USD","EUR","GBP","AED","SAR","INR","CNY","JPY","AUD","CAD")
    val amt = amount.toDoubleOrNull() ?: 0.0
    val pkr = amt * (rates[from] ?: 1.0)
    val result = if ((rates[to] ?: 1.0) == 0.0) 0.0 else pkr / (rates[to] ?: 1.0)

    LaunchedEffect(Unit) { vm.fetchRates() }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text(if (live) "● ${vm.t("liveRates")}" else "○ ${vm.t("offlineRates")}", color = if (live) Green else Color.Gray, fontSize = 12.sp)
        if (updated.isNotEmpty()) Text("${vm.t("lastUpdated")}: $updated", fontSize = 12.sp, color = Color.Gray)
        OutlinedTextField(value = amount, onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amount = it }, label = { Text(vm.t("amount")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }), modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CurrencyDrop(currencies, from, { from = it }, Modifier.weight(1f))
            IconButton(onClick = { val t = from; from = to; to = t }, modifier = Modifier.align(Alignment.CenterVertically)) { Icon(Icons.Filled.SwapHoriz, contentDescription = "Swap") }
            CurrencyDrop(currencies, to, { to = it }, Modifier.weight(1f))
        }
        Card(Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = Green)) {
            Text("$amount $from = ${"%,.2f".format(result)} $to", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(14.dp))
        }
        Button(onClick = { vm.fetchRates() }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text(vm.t("updateRates"), fontWeight = FontWeight.Bold, maxLines = 1) }
        OutlinedButton(onClick = { if (!editMode) editVal = (rates[editCur] ?: 0.0).toString(); editMode = !editMode }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text(vm.t("editRates"), maxLines = 1) }
        if (editMode) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                CurrencyDrop(currencies, editCur, { editCur = it; editVal = (rates[it] ?: 0.0).toString() }, Modifier.weight(1f))
                OutlinedTextField(value = editVal, onValueChange = { editVal = it }, label = { Text("PKR / 1 $editCur") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }), modifier = Modifier.weight(1f))
                Button(onClick = {
                    val v = editVal.trim().toDoubleOrNull()
                    if (v != null && v > 0) { keyboardController?.hide(); vm.setRate(editCur, v); editVal = ""; editMode = false }
                    else vm.showToast(vm.t("enterValidNumber"))
                }) { Text(vm.t("save")) }
            }
        }
    }
}

@Composable
fun CurrencyDrop(list: List<String>, value: String, onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    var ex by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { ex = true }, modifier = Modifier.fillMaxWidth()) { Text(value) }
        DropdownMenu(expanded = ex, onDismissRequest = { ex = false }) {
            list.forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = { onPick(c); ex = false }) }
        }
    }
}

@Composable
fun UnitConverter(vm: AppViewModel) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val groups = mapOf(
        "Length" to listOf("m" to 1.0, "km" to 1000.0, "cm" to 0.01, "mm" to 0.001, "ft" to 0.3048, "in" to 0.0254, "yd" to 0.9144, "mi" to 1609.34),
        "Weight" to listOf("kg" to 1.0, "g" to 0.001, "mg" to 0.000001, "lb" to 0.453592, "oz" to 0.0283495, "t" to 1000.0),
        "Volume" to listOf("L" to 1.0, "mL" to 0.001, "m³" to 1000.0, "gal" to 3.78541, "cup" to 0.24),
        "Area" to listOf("m²" to 1.0, "km²" to 1_000_000.0, "ft²" to 0.092903, "acre" to 4046.86, "ha" to 10000.0),
        "Speed" to listOf("km/h" to 1.0, "m/s" to 3.6, "mph" to 1.60934),
        "Time" to listOf("s" to 1.0, "min" to 60.0, "h" to 3600.0, "day" to 86400.0),
        "Pressure" to listOf("Pa" to 1.0, "kPa" to 1000.0, "bar" to 100000.0, "psi" to 6894.76),
        "Energy" to listOf("J" to 1.0, "kJ" to 1000.0, "cal" to 4.184, "kWh" to 3_600_000.0),
        "Power" to listOf("W" to 1.0, "kW" to 1000.0, "hp" to 745.7),
        "Data" to listOf("B" to 1.0, "KB" to 1024.0, "MB" to 1048576.0, "GB" to 1073741824.0, "TB" to 1099511627776.0),
        "Fuel" to listOf("km/L" to 1.0, "L/100km" to 1.0, "mpg" to 0.425144)
    )
    var group by remember { mutableStateOf("Length") }
    var amount by remember { mutableStateOf("1") }
    var from by remember { mutableStateOf(groups["Length"]!![0].first) }
    var to by remember { mutableStateOf(groups["Length"]!![1].first) }
    val units = groups[group] ?: emptyList()
    val amt = amount.toDoubleOrNull() ?: 0.0
    val result = if (group == "Fuel") {
        if (from == "L/100km" && to == "km/L") 100.0 / amt else if (from == "km/L" && to == "L/100km") 100.0 / amt else amt
    } else {
        val base = amt * (units.first { it.first == from }.second)
        base / units.first { it.first == to }.second
    }
    var tempC by remember { mutableStateOf("0") }
    var tempTo by remember { mutableStateOf("F") }

    Column {
        Text(unitGroupLabel(vm, group), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
        UnitDrop(groups.keys.toList() + "Temperature", group, { g ->
            if (g == "Temperature") {
                group = g
            } else {
                group = g; from = groups[g]!![0].first; to = groups[g]!![1].first
            }
        }, Modifier.fillMaxWidth(), label = { unitGroupLabel(vm, it) })
        Spacer(Modifier.height(8.dp))
        if (group == "Temperature") {
            OutlinedTextField(value = tempC, onValueChange = { tempC = it }, label = { Text("°C") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }), modifier = Modifier.fillMaxWidth())
            val c = tempC.toDoubleOrNull() ?: 0.0
            Text("°F: ${"%.1f".format(c * 9 / 5 + 32)}   •   K: ${"%.1f".format(c + 273.15)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        } else {
            OutlinedTextField(value = amount, onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amount = it }, label = { Text(vm.t("amount")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }), modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UnitDrop(units.map { it.first }, from, { from = it }, Modifier.weight(1f))
                IconButton(onClick = { val t = from; from = to; to = t }, modifier = Modifier.align(Alignment.CenterVertically)) { Icon(Icons.Filled.SwapHoriz, contentDescription = "Swap") }
                UnitDrop(units.map { it.first }, to, { to = it }, Modifier.weight(1f))
            }
            Card(Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = Green)) {
                Text("$amount $from = ${"%.4f".format(result)} $to", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(14.dp))
            }
        }
    }
}

@Composable
// Translated display name for a unit-converter group key (stored key stays English).
fun unitGroupLabel(vm: AppViewModel, key: String): String = when (key) {
    "Length" -> vm.t("unitLength")
    "Weight" -> vm.t("unitWeight")
    "Volume" -> vm.t("unitVolume")
    "Area" -> vm.t("unitArea")
    "Speed" -> vm.t("unitSpeed")
    "Time" -> vm.t("unitTime")
    "Pressure" -> vm.t("unitPressure")
    "Energy" -> vm.t("unitEnergy")
    "Power" -> vm.t("unitPower")
    "Data" -> vm.t("unitData")
    "Fuel" -> vm.t("unitFuel")
    "Temperature" -> vm.t("unitTemperature")
    else -> key
}

@Composable
fun UnitDrop(list: List<String>, value: String, onPick: (String) -> Unit, modifier: Modifier = Modifier, label: (String) -> String = { it }) {
    var ex by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { ex = true }, modifier = Modifier.fillMaxWidth()) { Text(label(value)) }
        DropdownMenu(expanded = ex, onDismissRequest = { ex = false }) {
            list.forEach { c -> DropdownMenuItem(text = { Text(label(c)) }, onClick = { onPick(c); ex = false }) }
        }
    }
}

// ---------------- Report / Search ----------------
@Composable
fun ReportScreen(vm: AppViewModel) {
    val entries by vm.entries.collectAsState()
    val categories by vm.categories.collectAsState()
    val dash = vm.dashboard()
    Column {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Green)) {
            Column(Modifier.padding(14.dp)) {
                Text(vm.t("thisMonth"), color = Color.White, fontSize = 14.sp)
                Text("${vm.t("totalIn")}: ${fmtRs(dash.monthIn)}", color = Color.White)
                Text("${vm.t("totalOut")}: ${fmtRs(dash.monthOut)}", color = Color.White)
                Text("${vm.t("balanceInHand")}: ${fmtRs(dash.balance)}", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
        // Simple bar chart: last 6 months OUT vs IN
        val cal = Calendar.getInstance()
        val months = (5 downTo 0).map { off ->
            val c = Calendar.getInstance().apply { add(Calendar.MONTH, -off) }
            c
        }
        val maxV = months.maxOfOrNull { c ->
            entries.filter { e -> val ec = Calendar.getInstance().apply { timeInMillis = e.dateMillis }; ec.get(Calendar.MONTH) == c.get(Calendar.MONTH) && ec.get(Calendar.YEAR) == c.get(Calendar.YEAR) }.sumOf { it.amount }
        } ?: 1L
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardWhite)) {
            Column(Modifier.padding(12.dp)) {
                Text("6 ${vm.t("month")}", fontWeight = FontWeight.Bold)
                months.forEach { c ->
                    val mIn = entries.filter { e -> e.type == "IN" && Calendar.getInstance().apply { timeInMillis = e.dateMillis }.get(Calendar.MONTH) == c.get(Calendar.MONTH) }.sumOf { it.amount }
                    val mOut = entries.filter { e -> e.type == "OUT" && Calendar.getInstance().apply { timeInMillis = e.dateMillis }.get(Calendar.MONTH) == c.get(Calendar.MONTH) }.sumOf { it.amount }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(Strings.get(vm.lang.value, listOf("jan","feb","mar","apr","may","jun","jul","aug","sep","oct","nov","dec")[c.get(Calendar.MONTH)]), modifier = Modifier.width(40.dp), fontSize = 12.sp)
                        Canvas(Modifier.weight(1f).height(14.dp)) {
                            val w = size.width
                            drawRect(Green, topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(w * (mIn.toFloat() / maxV.toFloat().coerceAtLeast(1f)), 6f))
                            drawRect(Red, topLeft = Offset(0f, 8f), size = androidx.compose.ui.geometry.Size(w * (mOut.toFloat() / maxV.toFloat().coerceAtLeast(1f)), 6f))
                        }
                        Text(fmtRs(mOut), fontSize = 11.sp)
                    }
                }
                Text("■ ${vm.t("in")}  ■ ${vm.t("out")}", fontSize = 11.sp, color = Color.Gray)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(vm.t("permanentRecord"), fontWeight = FontWeight.Bold)
        categories.forEach { c ->
            val list = entries.filter { it.categoryId == c.id }
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(vm.catName(c))
                Text("${list.size} • ${fmtRs(list.sumOf { it.amount })}", color = Color.Gray)
            }
        }
    }
}

@Composable
fun SearchScreen(vm: AppViewModel) {
    val entries by vm.entries.collectAsState()
    val categories by vm.categories.collectAsState()
    var q by remember { mutableStateOf("") }
    val filtered = entries.filter {
        val cat = categories.firstOrNull { c -> c.id == it.categoryId }?.name ?: ""
        it.note.contains(q, ignoreCase = true) || cat.contains(q, ignoreCase = true) || it.memberName.contains(q, ignoreCase = true)
    }
    Column {
        OutlinedTextField(value = q, onValueChange = { q = it }, label = { Text(vm.t("searchHint")) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        if (q.isBlank()) Text(vm.t("searchHint"), color = Color.Gray)
        filtered.forEach { e ->
            val cat = categories.firstOrNull { it.id == e.categoryId }?.let { vm.catName(it) } ?: ""
            Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), colors = CardDefaults.cardColors(containerColor = CardWhite)) {
                Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text(if (e.note.isNotBlank()) vm.noteDisplay(e.note) else cat, fontWeight = FontWeight.Bold); Text("${shortDate(e.dateMillis, vm.lang.value)} • $cat • ${vm.memberDisplayName(e.memberName)}", fontSize = 12.sp, color = Color.Gray) }
                    Text((if (e.type == "IN") "+ " else "− ") + fmtRs(e.amount), color = if (e.type == "IN") Green else Red, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ---------------- Planned / Reminders / Budget ----------------
@Composable
fun PlannedScreen(vm: AppViewModel) {
    val rec by vm.recurring.collectAsState()
    val categories by vm.categories.collectAsState()
    var catId by remember { mutableStateOf(categories.firstOrNull()?.id ?: 0) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var day by remember { mutableStateOf("1") }
    Column {
        rec.forEach { r ->
            val cat = categories.firstOrNull { it.id == r.categoryId }?.let { vm.catName(it) } ?: ""
            Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), colors = CardDefaults.cardColors(containerColor = CardWhite)) {
                Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text(cat, fontWeight = FontWeight.Bold); Text("${fmtRs(r.amount)} • ${vm.t("everyMonth")} • ${vm.t("day")} ${r.dayOfMonth}", fontSize = 12.sp, color = Color.Gray) }
                    TextButton(onClick = { vm.deleteRecurring(r.id) }) { Text(vm.t("delete"), color = Red) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(vm.t("add"), fontWeight = FontWeight.Bold)
        OutlinedTextField(value = amount, onValueChange = { if (it.all { c -> c.isDigit() }) amount = it }, label = { Text(vm.t("amount")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(vm.t("note")) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = day, onValueChange = { if (it.all { c -> c.isDigit() }) day = it }, label = { Text("${vm.t("date")} (1-28)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        categories.forEach { c ->
            Row(Modifier.clickable { catId = c.id }.padding(vertical = 4.dp)) { RadioButton(selected = catId == c.id, onClick = { catId = c.id }); Text(vm.catName(c)) }
        }
        Button(onClick = { val a = amount.toLongOrNull() ?: 0L; val d = day.toIntOrNull() ?: 1; if (a > 0 && catId != 0) { vm.addRecurring(catId, a, note, d.coerceIn(1, 28)); amount = ""; note = "" } }, colors = ButtonDefaults.buttonColors(containerColor = Green), modifier = Modifier.fillMaxWidth()) { Text(vm.t("save")) }
    }
}

@Composable
fun RemindersScreen(vm: AppViewModel) {
    val rem by vm.reminders.collectAsState()
    var title by remember { mutableStateOf("") }
    var dateStr by remember { mutableStateOf("") }
    Column {
        rem.forEach { r ->
            Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), colors = CardDefaults.cardColors(containerColor = CardWhite)) {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = r.done, onCheckedChange = { vm.toggleReminder(r) })
                    Column(Modifier.weight(1f)) { Text(r.title, fontWeight = FontWeight.Bold); Text(shortDate(r.dateMillis, vm.lang.value), fontSize = 12.sp, color = Color.Gray) }
                    TextButton(onClick = { vm.deleteReminder(r.id) }) { Text(vm.t("delete"), color = Red) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(vm.t("title")) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = dateStr, onValueChange = { dateStr = it }, label = { Text("DD/MM/YYYY") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            val parts = dateStr.split("/").mapNotNull { it.toIntOrNull() }
            if (title.isNotBlank() && parts.size == 3) {
                val cal = Calendar.getInstance().apply { set(parts[2], parts[1] - 1, parts[0], 9, 0, 0) }
                vm.addReminder(title, cal.timeInMillis); title = ""; dateStr = ""
            }
        }, colors = ButtonDefaults.buttonColors(containerColor = Green), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text(vm.t("addReminder")) }
    }
}

@Composable
fun BudgetScreen(vm: AppViewModel) {
    val categories by vm.categories.collectAsState()
    val entries by vm.entries.collectAsState()
    val budgets by vm.budgets.collectAsState()
    var editing by remember { mutableStateOf<Category?>(null) }
    var amt by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf<Category?>(null) }
    Column {
        Text(vm.t("thisMonth"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        categories.filter { it.type == "OUT" }.forEach { c ->
            val spent = entries.filter { it.categoryId == c.id && it.type == "OUT" }.sumOf { it.amount }
            val b = budgets.firstOrNull { it.categoryId == c.id }
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = CardWhite)) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(vm.catName(c), fontWeight = FontWeight.Bold)
                        Row {
                            TextButton(onClick = { editing = c; amt = b?.amount?.toString() ?: "" }) { Text(vm.t("setBudget")) }
                            TextButton(onClick = { confirmDelete = c }) { Text("🗑", fontSize = 16.sp) }
                        }
                    }
                    Text("${vm.t("totalExpenses")}: ${fmtRs(spent)}" + (if (b != null) " / ${fmtRs(b.amount)}" else " • ${vm.t("noBudget")}"), fontSize = 13.sp)
                    if (b != null) {
                        val frac = (spent.toFloat() / b.amount.coerceAtLeast(1)).coerceIn(0f, 1f)
                        LinearProgressIndicator(progress = frac, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), color = if (spent > b.amount) Red else if (spent > b.amount * 0.8) Color(0xFFFF9800) else Green)
                        if (spent > b.amount) Text(vm.t("overBudget"), color = Red, fontSize = 12.sp)
                        else if (spent > b.amount * 0.8) Text(vm.t("riskOverspend"), color = Color(0xFFFF9800), fontSize = 12.sp)
                    }
                }
            }
        }
    }
    if (editing != null) {
        AlertDialog(onDismissRequest = { editing = null }, title = { Text("${vm.t("setBudget")} — ${vm.catName(editing!!)}") },
            text = { OutlinedTextField(value = amt, onValueChange = { if (it.all { c -> c.isDigit() }) amt = it }, label = { Text(vm.t("amount")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()) },
            confirmButton = { Button(onClick = { amt.toLongOrNull()?.let { vm.setBudget(editing!!.id, it) }; editing = null }, colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text(vm.t("save")) } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text(vm.t("cancel")) } })
    }
    if (confirmDelete != null) {
        AlertDialog(onDismissRequest = { confirmDelete = null }, title = { Text(vm.t("confirmDelete")) },
            text = { Text(vm.catName(confirmDelete!!)) },
            confirmButton = { Button(onClick = { vm.deleteCategory(confirmDelete!!.id); confirmDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text(vm.t("delete")) } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text(vm.t("cancel")) } })
    }
}

// ---------------- Loan / Bills ----------------
@Composable
fun LoanScreen(vm: AppViewModel) {
    val list by vm.udhar.collectAsState()
    var person by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("lend") }
    val keyboardController = LocalSoftwareKeyboardController.current
    Column {
        list.forEach { u ->
            Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), colors = CardDefaults.cardColors(containerColor = CardWhite)) {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(u.personName, fontWeight = FontWeight.Bold)
                        Text(if (u.kind == "lend") vm.t("toReceive") else vm.t("toReturn"), fontSize = 12.sp, color = Color.Gray)
                        Text(shortDate(u.dateMillis, vm.lang.value), fontSize = 12.sp, color = Color.Gray)
                    }
                    Text(fmtRs(u.amount), fontWeight = FontWeight.Bold)
                    TextButton(onClick = { if (u.status == "pending") vm.markUdharPaid(u) else vm.deleteUdhar(u.id) }) { Text(if (u.status == "pending") vm.t("paid") else vm.t("delete"), color = if (u.status == "pending") Green else Red) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = person, onValueChange = { person = it }, label = { Text(vm.t("person")) }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = amount, onValueChange = { if (it.all { c -> c.isDigit() }) amount = it }, label = { Text(vm.t("amount")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }), modifier = Modifier.fillMaxWidth())
        Row { RadioButton(selected = kind == "lend", onClick = { keyboardController?.hide(); kind = "lend" }); Text(vm.t("toReceive")); RadioButton(selected = kind == "borrow", onClick = { keyboardController?.hide(); kind = "borrow" }); Text(vm.t("toReturn")) }
        Button(onClick = { keyboardController?.hide(); val a = amount.toLongOrNull() ?: 0L; if (person.isNotBlank() && a > 0) { vm.addUdhar(person, a, kind); person = ""; amount = "" } }, colors = ButtonDefaults.buttonColors(containerColor = Green), modifier = Modifier.fillMaxWidth()) { Text(vm.t("add")) }
    }
}

@Composable
fun BillsScreen(vm: AppViewModel) {
    val list by vm.bills.collectAsState()
    var name by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf("LESCO") }
    var amount by remember { mutableStateOf("") }
    var due by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val providers = listOf("LESCO", "K-Electric", "SNGPL", "SSGC", "WASA", "PTCL", "Internet", "Other")
    Column {
        list.forEach { b ->
            val overdue = b.status == "pending" && b.dueMillis < System.currentTimeMillis()
            Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), colors = CardDefaults.cardColors(containerColor = CardWhite)) {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(b.name, fontWeight = FontWeight.Bold)
                        Text("${b.provider} • ${shortDate(b.dueMillis, vm.lang.value)}", fontSize = 12.sp, color = Color.Gray)
                        if (overdue) Text(vm.t("overdue"), color = Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(fmtRs(b.amount), fontWeight = FontWeight.Bold, color = Red)
                    TextButton(onClick = { if (b.status == "pending") vm.markBillPaid(b) else vm.deleteBill(b.id) }) { Text(if (b.status == "pending") vm.t("paid") else vm.t("delete"), color = if (b.status == "pending") Green else Red) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(vm.t("title")) }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }), modifier = Modifier.fillMaxWidth())
        var ex by remember { mutableStateOf(false) }
        Box { OutlinedButton(onClick = { keyboardController?.hide(); ex = true }, modifier = Modifier.fillMaxWidth()) { Text(provider) }; DropdownMenu(expanded = ex, onDismissRequest = { ex = false }) { providers.forEach { p -> DropdownMenuItem(text = { Text(p) }, onClick = { provider = p; ex = false }) } } }
        OutlinedTextField(value = amount, onValueChange = { if (it.all { c -> c.isDigit() }) amount = it }, label = { Text(vm.t("amount")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = due, onValueChange = { due = it }, label = { Text("${vm.t("dueDate")} DD/MM/YYYY") }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }), modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            keyboardController?.hide()
            val a = amount.toLongOrNull() ?: 0L
            val parts = due.split("/").mapNotNull { it.toIntOrNull() }
            if (name.isNotBlank() && a > 0 && parts.size == 3) {
                val cal = Calendar.getInstance().apply { set(parts[2], parts[1] - 1, parts[0], 23, 59, 0) }
                vm.addBill(name, provider, a, cal.timeInMillis); name = ""; amount = ""; due = ""
            }
        }, colors = ButtonDefaults.buttonColors(containerColor = Green), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text(vm.t("add")) }
    }
}

// ---------------- Family / Backup / Change PIN ----------------
@Composable
fun FamilyScreen(vm: AppViewModel) {
    val members by vm.members.collectAsState()
    val invites by vm.invites.collectAsState()
    val isAdmin by vm.isAdmin.collectAsState()
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var lastCode by remember { mutableStateOf("") }
    val ctx = LocalContext.current
    Column {
        Text(vm.t("family"), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        members.forEach { m ->
            Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), colors = CardDefaults.cardColors(containerColor = CardWhite)) {
                Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(m.name, fontWeight = FontWeight.Bold)
                    if (isAdmin) TextButton(onClick = { vm.deleteMember(m.id) }) { Text(vm.t("delete"), color = Red) }
                }
            }
        }
        if (members.isEmpty()) Text(vm.t("noMembers"), color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        if (isAdmin) {
            Button(onClick = { lastCode = vm.createInvite() }, colors = ButtonDefaults.buttonColors(containerColor = Green), modifier = Modifier.fillMaxWidth()) { Text(vm.t("createInvite")) }
            if (lastCode.isNotEmpty()) {
                Card(Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = Green)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${vm.t("inviteCode")}: $lastCode", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(vm.t("valid24h"), color = Color.White)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            OutlinedButton(onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Join my Hisaboo family! Code: $lastCode (${vm.t("valid24h")})")
                                }
                                ctx.startActivity(Intent.createChooser(intent, vm.t("shareWhatsApp")))
                            }) { Text(vm.t("shareWhatsApp"), color = Color.White) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(vm.t("addMember"), fontWeight = FontWeight.Bold)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(vm.t("yourName")) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = pin, onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it }, label = { Text(vm.t("setPin")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            Button(onClick = { if (name.isNotBlank() && pin.length >= 4) { vm.addMember(name, pin); name = ""; pin = "" } }, colors = ButtonDefaults.buttonColors(containerColor = Green), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text(vm.t("add")) }
            Spacer(Modifier.height(8.dp))
            invites.forEach { inv ->
                val expired = System.currentTimeMillis() > inv.expiresAt
                val status = when { inv.status == "revoked" -> vm.t("revoked"); inv.status == "used" -> "${vm.t("used")} (${inv.usedBy})"; expired -> vm.t("expired"); else -> vm.t("waiting") }
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${inv.code} • $status", fontSize = 13.sp)
                    if (inv.status == "active" && !expired) TextButton(onClick = { vm.revokeInvite(inv.id) }) { Text(vm.t("revoke"), color = Red) }
                }
            }
        }
    }
}

@Composable
fun BackupScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    val lastBackup by vm.lastBackup.collectAsState()
    var msg by remember { mutableStateOf("") }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val text = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                msg = if (vm.restoreFromJson(text)) "✓ ${vm.t("restore")}" else "Invalid backup file"
            } catch (e: Exception) { msg = "Restore failed" }
        }
    }
    Column {
        Text(vm.t("backup"), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("${vm.t("lastBackup")}: ${if (lastBackup.isEmpty()) vm.t("never") else lastBackup}", color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        Button(onClick = { msg = if (vm.saveServerBackup()) vm.t("backupSaved") else "Backup failed" }, colors = ButtonDefaults.buttonColors(containerColor = Green), modifier = Modifier.fillMaxWidth()) { Text(vm.t("backupNow")) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = {
            val json = vm.buildBackupJson()
            val f = File(ctx.cacheDir, "expense-backup.json"); f.writeText(json)
            val uri = FileProvider.getUriForFile(ctx, "com.mianzahid.expenseentry.fileprovider", f)
            val intent = Intent(Intent.ACTION_SEND).apply { type = "application/json"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            ctx.startActivity(Intent.createChooser(intent, vm.t("downloadBackup")))
        }, modifier = Modifier.fillMaxWidth()) { Text(vm.t("downloadBackup")) }
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            val json = vm.loadSavedBackupJson()
            msg = if (json != null && vm.restoreFromJson(json)) "✓ ${vm.t("restore")}" else "No saved backup"
        }, colors = ButtonDefaults.buttonColors(containerColor = Green), modifier = Modifier.fillMaxWidth()) { Text(vm.t("restoreSaved")) }
        OutlinedButton(onClick = { pickFile.launch("application/json") }, modifier = Modifier.fillMaxWidth()) { Text(vm.t("restoreFile")) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = {
            val csv = vm.exportCsv()
            val f = File(ctx.cacheDir, "expense-export.csv"); f.writeText(csv)
            val uri = FileProvider.getUriForFile(ctx, "com.mianzahid.expenseentry.fileprovider", f)
            val intent = Intent(Intent.ACTION_SEND).apply { type = "text/csv"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            ctx.startActivity(Intent.createChooser(intent, vm.t("exportExcel")))
        }, modifier = Modifier.fillMaxWidth()) { Text(vm.t("exportExcel")) }
        OutlinedButton(onClick = {
            val doc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = doc.startPage(pageInfo)
            val paint = android.graphics.Paint().apply { textSize = 14f }
            var y = 40f
            page.canvas.drawText("Hisaboo — ${vm.t("monthlyReport")}", 20f, y, paint); y += 30
            vm.entries.value.take(40).forEach { e ->
                page.canvas.drawText("${shortDate(e.dateMillis, "en")}  ${e.type}  ${fmtRs(e.amount)}  ${e.note}", 20f, y, paint); y += 22
            }
            doc.finishPage(page)
            val f = File(ctx.cacheDir, "expense-report.pdf")
            FileOutputStream(f).use { doc.writeTo(it) }
            doc.close()
            val uri = FileProvider.getUriForFile(ctx, "com.mianzahid.expenseentry.fileprovider", f)
            val intent = Intent(Intent.ACTION_SEND).apply { type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            ctx.startActivity(Intent.createChooser(intent, vm.t("exportPdf")))
        }, modifier = Modifier.fillMaxWidth()) { Text(vm.t("exportPdf")) }
        Spacer(Modifier.height(8.dp))
        Text(vm.t("gmailBackup"), fontWeight = FontWeight.Bold)
        Text(vm.t("gmailNote"), fontSize = 12.sp, color = Color.Gray)
        if (msg.isNotEmpty()) Text(msg, color = Green, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun ChangePinScreen(vm: AppViewModel) {
    var old by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    Column {
        OutlinedTextField(value = old, onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) old = it }, label = { Text(vm.t("oldPin")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = newPin, onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPin = it }, label = { Text(vm.t("newPin")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        Button(onClick = { msg = if (vm.changeAdminPin(old, newPin)) "✓ PIN changed" else vm.t("wrongPin") }, colors = ButtonDefaults.buttonColors(containerColor = Green), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text(vm.t("save")) }
        if (msg.isNotEmpty()) Text(msg, modifier = Modifier.padding(top = 8.dp))
    }
}
