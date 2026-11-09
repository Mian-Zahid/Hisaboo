@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.mianzahid.expenseentry

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Calendar

val Green = Color(0xFF0B5E40)
val GreenLight = Color(0xFF147A55)
val Red = Color(0xFFC81E3A)
val Bg = Color(0xFFF2F2F0)
val CardWhite = Color.White

// Jameel Noori Nastaliq for Urdu text (bundled in res/font).
val UrduFontFamily = FontFamily(Font(R.font.jameel_noori_nastaleeq))

// Nastaliq is tall — Urdu typography uses generous line heights so text never clips.
fun urduTypography(): Typography = Typography(
    displayLarge = TextStyle(fontFamily = UrduFontFamily, lineHeight = 56.sp),
    displayMedium = TextStyle(fontFamily = UrduFontFamily, lineHeight = 48.sp),
    displaySmall = TextStyle(fontFamily = UrduFontFamily, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = UrduFontFamily, lineHeight = 44.sp),
    headlineMedium = TextStyle(fontFamily = UrduFontFamily, lineHeight = 40.sp),
    headlineSmall = TextStyle(fontFamily = UrduFontFamily, lineHeight = 36.sp),
    titleLarge = TextStyle(fontFamily = UrduFontFamily, lineHeight = 36.sp),
    titleMedium = TextStyle(fontFamily = UrduFontFamily, lineHeight = 32.sp),
    titleSmall = TextStyle(fontFamily = UrduFontFamily, lineHeight = 30.sp),
    bodyLarge = TextStyle(fontFamily = UrduFontFamily, lineHeight = 30.sp),
    bodyMedium = TextStyle(fontFamily = UrduFontFamily, lineHeight = 28.sp),
    bodySmall = TextStyle(fontFamily = UrduFontFamily, lineHeight = 26.sp),
    labelLarge = TextStyle(fontFamily = UrduFontFamily, lineHeight = 28.sp),
    labelMedium = TextStyle(fontFamily = UrduFontFamily, lineHeight = 26.sp),
    labelSmall = TextStyle(fontFamily = UrduFontFamily, lineHeight = 24.sp)
)

sealed class Screen {
    object Home : Screen()
    object Categories : Screen()
    object Calculator : Screen()
    data class CategoryDetail(val id: Int) : Screen()
    object Report : Screen()
    object Search : Screen()
    object Planned : Screen()
    object Reminders : Screen()
    object Budget : Screen()
    object Loan : Screen()
    object Bills : Screen()
    object Family : Screen()
    object Backup : Screen()
    object ChangePin : Screen()
}

fun shortcutLabel(vm: AppViewModel, key: String): String {
    if (key.startsWith("category:")) {
        val id = key.removePrefix("category:").toIntOrNull()
        return vm.categories.value.firstOrNull { it.id == id }?.name ?: vm.t("categories")
    }
    return when (key) {
        "report" -> vm.t("monthlyReport")
        "budgets" -> vm.t("budget")
        "planned" -> vm.t("planned")
        "reminders" -> vm.t("reminders")
        "loan" -> vm.t("loan")
        "bills" -> vm.t("bills")
        "family" -> vm.t("family")
        "backup" -> vm.t("backup")
        "search" -> vm.t("search")
        "calculator" -> vm.t("calculator")
        "categories" -> vm.t("categories")
        else -> key
    }
}

fun shortcutToScreen(key: String): Screen = when {
    key == "report" -> Screen.Report
    key == "budgets" -> Screen.Budget
    key == "planned" -> Screen.Planned
    key == "reminders" -> Screen.Reminders
    key == "loan" -> Screen.Loan
    key == "bills" -> Screen.Bills
    key == "family" -> Screen.Family
    key == "backup" -> Screen.Backup
    key == "search" -> Screen.Search
    key == "calculator" -> Screen.Calculator
    key == "categories" -> Screen.Categories
    key.startsWith("category:") -> Screen.CategoryDetail(key.removePrefix("category:").toIntOrNull() ?: 0)
    else -> Screen.Home
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            startCompose()
        } catch (t: Throwable) {
            // v1.2: if Compose cannot start (e.g. a missing library class),
            // do NOT crash — open a plain-Views PIN screen that shows the
            // exact error, so the user can retry and report it.
            showFallback(t)
        }
    }

    private fun startCompose() {
        setContent {
            val ctx = LocalContext.current
            val vm: AppViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    AppViewModel(ctx.applicationContext) as T
            })
            val lang by vm.lang.collectAsState()
            val unlocked by vm.unlocked.collectAsState()
            var screen by remember { mutableStateOf<Screen>(Screen.Home) }
            var backStack by remember { mutableStateOf<List<Screen>>(emptyList()) }
            var showExitConfirm by remember { mutableStateOf(false) }
            fun go(s: Screen) { backStack = backStack + screen; screen = s }
            fun back() { if (backStack.isNotEmpty()) { screen = backStack.last(); backStack = backStack.dropLast(1) } else screen = Screen.Home }

            LaunchedEffect(Unit) { vm.seedIfEmpty() }
            LaunchedEffect(unlocked) { if (unlocked) vm.refresh() }

            val toast by vm.toast.collectAsState()
            LaunchedEffect(toast) {
                if (toast.isNotEmpty()) {
                    Toast.makeText(ctx, toast, Toast.LENGTH_SHORT).show()
                    vm.clearToast()
                }
            }

            MaterialTheme(
                colorScheme = lightColorScheme(primary = Green, secondary = GreenLight),
                typography = if (lang == "ur") urduTypography() else Typography()
            ) {
                if (!unlocked) {
                    LockScreen(vm = vm)
                } else {
                    val isDark = when (vm.theme.collectAsState().value) {
                        "dark" -> true
                        "light" -> false
                        else -> false
                    }
                    BackHandler {
                        if (screen !is Screen.Home || backStack.isNotEmpty()) {
                            back()
                        } else {
                            showExitConfirm = true
                        }
                    }
                    Scaffold(
                        bottomBar = {
                            if (screen is Screen.Home || screen is Screen.Categories || screen is Screen.Calculator) {
                                BottomNav(vm, screen) { s -> screen = s; backStack = emptyList() }
                            }
                        }
                    ) { pad ->
                        Box(Modifier.padding(pad)) {
                            AnimatedContent(
                                targetState = screen,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(250)) + slideInHorizontally(animationSpec = tween(250, easing = FastOutSlowInEasing)) { it / 4 }) togetherWith
                                        (fadeOut(animationSpec = tween(200)) + slideOutHorizontally(animationSpec = tween(200, easing = FastOutSlowInEasing)) { -it / 4 })
                                },
                                label = "screen"
                            ) { s ->
                            when (s) {
                                is Screen.Home -> HomeScreen(vm, onGo = ::go)
                                is Screen.Categories -> CategoriesScreen(vm, onOpen = { go(Screen.CategoryDetail(it)) })
                                is Screen.Calculator -> CalculatorScreen(vm)
                                is Screen.CategoryDetail -> CategoryDetailScreen(vm, s.id, onBack = ::back)
                                is Screen.Report -> SubPage(vm, vm.t("monthlyReport"), ::back) { ReportScreen(vm) }
                                is Screen.Search -> SubPage(vm, vm.t("search"), ::back) { SearchScreen(vm) }
                                is Screen.Planned -> SubPage(vm, vm.t("planned"), ::back) { PlannedScreen(vm) }
                                is Screen.Reminders -> SubPage(vm, vm.t("reminders"), ::back) { RemindersScreen(vm) }
                                is Screen.Budget -> SubPage(vm, vm.t("budget"), ::back) { BudgetScreen(vm) }
                                is Screen.Loan -> SubPage(vm, vm.t("loan"), ::back) { LoanScreen(vm) }
                                is Screen.Bills -> SubPage(vm, vm.t("bills"), ::back) { BillsScreen(vm) }
                                is Screen.Family -> SubPage(vm, vm.t("family"), ::back) { FamilyScreen(vm) }
                                is Screen.Backup -> SubPage(vm, vm.t("backup"), ::back) { BackupScreen(vm) }
                                is Screen.ChangePin -> SubPage(vm, vm.t("changePin"), ::back) { ChangePinScreen(vm) }
                            }
                            }
                        }
                    }
                    if (showExitConfirm) {
                        AlertDialog(
                            onDismissRequest = { showExitConfirm = false },
                            title = { Text(vm.t("exitTitle")) },
                            text = { Text(vm.t("exitText")) },
                            confirmButton = { TextButton(onClick = { finish() }) { Text(vm.t("yes")) } },
                            dismissButton = { TextButton(onClick = { showExitConfirm = false }) { Text(vm.t("no")) } }
                        )
                    }
                }
            }
        }
    }

    // Plain Android Views fallback: no Compose at all. Used only when
    // startCompose() throws on launch. Shows the error (so it can be
    // screenshotted and fixed) and lets the user enter/set the PIN and
    // retry Compose.
    private fun showFallback(error: Throwable?) {
        val prefs = getSharedPreferences("expense_prefs", MODE_PRIVATE)
        val hasPin = prefs.contains("admin_pin_hash")
        val green = android.graphics.Color.parseColor("#0B5E40")
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            setBackgroundColor(android.graphics.Color.parseColor("#F2F2F0"))
        }
        val title = TextView(this).apply {
            text = "Hisaboo"; textSize = 24f; setTextColor(green); gravity = Gravity.CENTER
        }
        val msg = TextView(this).apply {
            text = "App ka design khul nahi saka. Neeche PIN daal kar dobara try karein.\n\nGhalti: " +
                (error?.let { it.javaClass.simpleName + ": " + it.message } ?: "namaloom")
            textSize = 14f; gravity = Gravity.CENTER; setPadding(0, 24, 0, 24)
        }
        val pinInput = EditText(this).apply {
            hint = if (hasPin) "PIN daalein" else "Naya PIN (4-6 digits)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            gravity = Gravity.CENTER
        }
        val status = TextView(this).apply { textSize = 14f; gravity = Gravity.CENTER; setPadding(0, 16, 0, 0) }
        val btn = Button(this).apply {
            text = if (hasPin) "Kholein" else "PIN Set Karein"
            setOnClickListener {
                val pin = pinInput.text.toString()
                if (pin.length < 4) { status.text = "PIN kam se kam 4 digits ka ho"; return@setOnClickListener }
                if (hasPin) {
                    if (prefs.getString("admin_pin_hash", "") == sha256(pin)) {
                        try { startCompose() } catch (t: Throwable) { showFallback(t) }
                    } else status.text = "Ghalat PIN"
                } else {
                    prefs.edit().putString("admin_pin_hash", sha256(pin)).apply()
                    try { startCompose() } catch (t: Throwable) { showFallback(t) }
                }
            }
        }
        root.addView(title); root.addView(msg); root.addView(pinInput); root.addView(btn); root.addView(status)
        setContentView(root)
    }
}

@Composable
fun BottomNav(vm: AppViewModel, current: Screen, onPick: (Screen) -> Unit) {
    Surface(color = Color.White, shadowElevation = 4.dp) {
        Row(
            Modifier.fillMaxWidth().height(72.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(vm.t("home"), "🏠", current is Screen.Home) { onPick(Screen.Home) }
            BottomNavItem(vm.t("categories"), "🗂️", current is Screen.Categories) { onPick(Screen.Categories) }
            BottomNavItem(vm.t("calculator"), "🧮", current is Screen.Calculator) { onPick(Screen.Calculator) }
        }
    }
}

@Composable
fun BottomNavItem(label: String, icon: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.clickable { onClick() }.padding(horizontal = 18.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 24.sp)
        Text(label, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) Green else Color.Gray)
    }
}

@Composable
fun SubPage(vm: AppViewModel, title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(Bg)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            ThreeDotMenu(vm)
        }
        Box(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
            content()
        }
    }
}

@Composable
fun ThreeDotMenu(vm: AppViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val fpEnabled by vm.fingerprintEnabled.collectAsState()
    val theme by vm.theme.collectAsState()
    val langVal by vm.lang.collectAsState()
    val ctx = LocalContext.current
    val activity = ctx as? FragmentActivity
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.clip(CircleShape).background(Green.copy(alpha = 0.12f))
        ) { Icon(Icons.Filled.MoreVert, contentDescription = "Menu", tint = Green) }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            containerColor = Color(0xFFFAF8F2),
            shadowElevation = 8.dp
        ) {
            val items = listOf(
                "report" to vm.t("monthlyReport"), "search" to vm.t("search"),
                "planned" to vm.t("planned"), "reminders" to vm.t("reminders"),
                "budgets" to vm.t("budget"), "loan" to vm.t("loan"),
                "bills" to vm.t("bills"),
                "family" to vm.t("family"), "backup" to vm.t("backup"),
                "calculator" to vm.t("calculator")
            )
            // Navigation items need a callback; handled by parent via a shared nav holder
            items.forEach { (key, label) ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(shortcutIcon(key), fontSize = 18.sp)
                            Spacer(Modifier.width(10.dp))
                            Text(label, fontSize = 15.sp)
                        }
                    },
                    onClick = {
                        expanded = false
                        NavBus.go(shortcutToScreen(key))
                    }
                )
            }
            Divider(color = Green.copy(alpha = 0.15f))
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("\uD83D\uDD10", fontSize = 18.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(vm.t("changePin"), fontSize = 15.sp)
                    }
                },
                onClick = { expanded = false; NavBus.go(Screen.ChangePin) }
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("\uD83D\uDC46", fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                Text(vm.t("fingerprint"), modifier = Modifier.weight(1f), fontSize = 15.sp)
                Switch(checked = fpEnabled, onCheckedChange = { vm.setFingerprint(it) })
            }
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("\uD83C\uDFA8", fontSize = 18.sp)
                        Spacer(Modifier.width(10.dp))
                        Text("${vm.t("theme")}: ${themeName(theme, vm)}", fontSize = 15.sp)
                    }
                },
                onClick = {
                    val next = when (vm.theme.value) { "auto" -> "light"; "light" -> "dark"; else -> "auto" }
                    vm.setTheme(next)
                }
            )
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("\uD83C\uDF10", fontSize = 18.sp)
                        Spacer(Modifier.width(10.dp))
                        Text("${vm.t("language")}: ${langName(langVal, vm)}", fontSize = 15.sp)
                    }
                },
                onClick = {
                    expanded = false
                    val next = when (vm.lang.value) { "en" -> "ur"; "ur" -> "roman"; else -> "en" }
                    vm.setLang(next)
                }
            )
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("\uD83D\uDD12", fontSize = 18.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(vm.t("lock"), fontSize = 15.sp)
                    }
                },
                onClick = { expanded = false; vm.lock() }
            )
            Divider(color = Green.copy(alpha = 0.15f))
            DropdownMenuItem(text = { Text(vm.t("madeBy"), fontSize = 12.sp, color = Color.Gray) }, onClick = {})
        }
    }
}

fun themeName(theme: String, vm: AppViewModel): String = when (theme) {
    "light" -> vm.t("light")
    "dark" -> vm.t("dark")
    else -> vm.t("auto")
}

fun langName(lang: String, vm: AppViewModel): String = when (lang) {
    "ur" -> vm.t("urdu")
    "roman" -> vm.t("romanUrdu")
    else -> vm.t("english")
}

// Simple navigation bus so the 3-dot menu (used in many places) can navigate
object NavBus {
    var go: (Screen) -> Unit = {}
}

@Composable
fun LockScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    val activity = ctx as? FragmentActivity
    val lang by vm.lang.collectAsState()
    val fpEnabled by vm.fingerprintEnabled.collectAsState()
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var settingUp by remember { mutableStateOf(!vm.hasPin()) }
    var error by remember { mutableStateOf("") }
    var joinMode by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var joinName by remember { mutableStateOf("") }
    var joinPin by remember { mutableStateOf("") }
    var joinMsg by remember { mutableStateOf("") }

    fun tryUnlock(p: String) {
        if (p.length < 4) return
        if (vm.tryPin(p)) { pin = ""; error = "" } else { error = vm.t("wrongPin"); pin = "" }
    }

    fun biometric() {
        val act = activity ?: return
        val mgr = BiometricManager.from(act)
        if (mgr.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) != BiometricManager.BIOMETRIC_SUCCESS) {
            vm.showToast(vm.t("biometricNotSupported")); return
        }
        val prompt = BiometricPrompt(act, ContextCompat.getMainExecutor(act),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    vm.unlockAsAdmin()
                }
                override fun onAuthenticationError(code: Int, msg: CharSequence) {}
            })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle(vm.t("fingerprint")).setNegativeButtonText(vm.t("cancel")).build())
    }

    Column(
        Modifier.fillMaxSize().background(Bg).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("💰", fontSize = 56.sp)
        Text(vm.t("appName"), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Green)
        Spacer(Modifier.height(24.dp))
        if (joinMode) {
            OutlinedTextField(value = code, onValueChange = { code = it.uppercase() }, label = { Text(vm.t("enterCode")) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = joinName, onValueChange = { joinName = it }, label = { Text(vm.t("yourName")) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = joinPin, onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) joinPin = it }, label = { Text(vm.t("setPin")) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                if (vm.joinWithInvite(code, joinName, joinPin)) joinMsg = "✓ Joined" else joinMsg = vm.t("wrongPin")
            }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text(vm.t("join")) }
            if (joinMsg.isNotEmpty()) Text(joinMsg, color = Green)
            TextButton(onClick = { joinMode = false }) { Text(vm.t("cancel")) }
        } else if (settingUp) {
            Text(vm.t("setPin"), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            var phase by remember { mutableStateOf(0) } // 0 = new PIN, 1 = confirm
            if (phase == 0) {
                PinDots(pin)
                NumberPad(onDigit = { if (pin.length < 6) pin += it }, onDelete = { pin = pin.dropLast(1) })
                if (pin.length >= 4) {
                    Button(onClick = { phase = 1 }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text(vm.t("confirmPin")) }
                }
            } else {
                Text(vm.t("confirmPin"), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                PinDots(confirm)
                NumberPad(onDigit = { if (confirm.length < 6) { confirm += it; if (confirm.length == pin.length) { if (confirm == pin) { vm.setAdminPin(pin); vm.unlockAsAdmin() } else { error = vm.t("pinMismatch"); pin = ""; confirm = ""; phase = 0 } } } }, onDelete = { confirm = confirm.dropLast(1) })
                TextButton(onClick = { phase = 0; pin = ""; confirm = "" }) { Text(vm.t("cancel")) }
            }
            if (error.isNotEmpty()) Text(error, color = Red)
        } else {
            Text(vm.t("enterPin"), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            PinDots(pin)
            if (error.isNotEmpty()) Text(error, color = Red)
            NumberPad(onDigit = { if (pin.length < 6) { pin += it; tryUnlock(pin) } }, onDelete = { pin = pin.dropLast(1); error = "" })
            Spacer(Modifier.height(12.dp))
            if (fpEnabled) {
                OutlinedButton(onClick = ::biometric, modifier = Modifier.fillMaxWidth()) { Text("👆 ${vm.t("useFingerprint")}") }
            }
            TextButton(onClick = { joinMode = true }) { Text(vm.t("joinWithCode")) }
            Row {
                listOf("en" to "English", "ur" to "اردو", "roman" to "Roman Urdu").forEach { (l, label) ->
                    TextButton(onClick = { vm.setLang(l) }) { Text(label, color = if (lang == l) Green else Color.Gray) }
                }
            }
        }
    }
}

@Composable
fun PinDots(pin: String) {
    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(6) { i ->
            Box(
                Modifier.size(16.dp).clip(CircleShape)
                    .background(if (i < pin.length) Green else Color.LightGray)
            )
        }
    }
}

@Composable
fun NumberPad(onDigit: (String) -> Unit, onDelete: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { d ->
                    Button(onClick = { onDigit(d) }, modifier = Modifier.size(64.dp), shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text(d, fontSize = 22.sp) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Spacer(Modifier.size(64.dp))
            Button(onClick = { onDigit("0") }, modifier = Modifier.size(64.dp), shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text("0", fontSize = 22.sp) }
            Button(onClick = onDelete, modifier = Modifier.size(64.dp), shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text("⌫", fontSize = 20.sp) }
        }
    }
}

@Composable
fun HomeScreen(vm: AppViewModel, onGo: (Screen) -> Unit) {
    NavBus.go = onGo
    val lang by vm.lang.collectAsState()
    val dash = vm.dashboard()
    val entries by vm.entries.collectAsState()
    val categories by vm.categories.collectAsState()
    val shortcuts by vm.shortcuts.collectAsState()
    val isAdmin by vm.isAdmin.collectAsState()
    val viewer by vm.viewer.collectAsState()
    val hadith by vm.dailyHadith.collectAsState()
    LaunchedEffect(Unit) { vm.fetchDailyHadith() }
    var showEntry by remember { mutableStateOf<String?>(null) } // "IN" or "OUT"
    var showShortcutPicker by remember { mutableStateOf<Int?>(null) }

    val topCat = categories.filter { it.type == "OUT" }
        .map { c -> c to entries.filter { e -> e.categoryId == c.id && e.type == "OUT" }.sumOf { it.amount } }
        .maxByOrNull { it.second }

    // Static one-screen layout: no scroll
    Column(
        Modifier.fillMaxSize().background(Bg).padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(vm.t("appName"), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(if (isAdmin) "👑 ${vm.t("adminAccess")}" else "👤 ${viewer?.name ?: ""}", fontSize = 11.sp, color = Green,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFE3F2EA)).padding(horizontal = 8.dp, vertical = 4.dp))
            ThreeDotMenu(vm)
        }
        // Balance card
        Card(
            Modifier.fillMaxWidth().animateContentSize(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Green)
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(vm.t("balanceInHand"), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(fullDate(System.currentTimeMillis(), lang), color = Color.White, fontSize = 11.sp,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 3.dp))
                }
                Text(fmtRs(dash.balance), color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                Text(vm.t("amountWithYou"), color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.15f)).padding(10.dp)) {
                        Column { Text(vm.t("totalIn"), color = Color.White, fontSize = 11.sp); Text(fmtRs(dash.totalIn), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                    }
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.15f)).padding(10.dp)) {
                        Column { Text(vm.t("totalOut"), color = Color.White, fontSize = 11.sp); Text(fmtRs(dash.totalOut), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // Top expense card (slim, one-line label)
        if (topCat != null && topCat.second > 0) {
            Card(
                Modifier.fillMaxWidth().clickable { onGo(Screen.CategoryDetail(topCat.first.id)) },
                shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = CardWhite)
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🔥", fontSize = 26.sp)
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(vm.t("topExpense"), fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(vm.catName(topCat.first), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text("  •  ${entries.count { it.categoryId == topCat.first.id }} ${vm.t("entries")}", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                    Text(fmtRs(topCat.second), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Red)
                    Text(" ›", fontSize = 20.sp, color = Color.Gray)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        // 6 customizable shortcuts 3+3
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0 to 2, 3 to 5).forEach { (a, b) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in a..b) {
                        val key = shortcuts.getOrNull(i) ?: ""
                        Box(
                            Modifier.weight(1f).height(76.dp).clip(RoundedCornerShape(12.dp))
                                .background(Color.White).combinedClickable(
                                    onClick = { if (key.isBlank()) showShortcutPicker = i else onGo(shortcutToScreen(key)) },
                                    onLongClick = { showShortcutPicker = i }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (key.isBlank()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("+", fontSize = 18.sp, color = Color.Gray)
                                    Text(vm.t("addShortcut"), fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.Center)
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(shortcutIcon(key, categories), fontSize = 20.sp)
                                    Text(shortcutLabel(vm, key), fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        DailyHadithCard(vm, hadith)
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { showEntry = "IN" }, modifier = Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Green), shape = RoundedCornerShape(14.dp)) { Text(vm.t("moneyIn"), fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            Button(onClick = { showEntry = "OUT" }, modifier = Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Red), shape = RoundedCornerShape(14.dp)) { Text(vm.t("expense"), fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }

    if (showShortcutPicker != null) {
        ShortcutPicker(vm, showShortcutPicker!!, onPick = { key ->
            val slot = showShortcutPicker!!
            if (shortcuts.indexOf(key) >= 0 && shortcuts.indexOf(key) != slot) {
                vm.showToast(vm.t("alreadyAdded"))
            } else {
                val list = shortcuts.toMutableList()
                while (list.size < 6) list.add("")
                list[slot] = key
                vm.saveShortcuts(list)
                showShortcutPicker = null
            }
        }, onRemove = {
            val list = shortcuts.toMutableList()
            while (list.size < 6) list.add("")
            list[showShortcutPicker!!] = ""
            vm.saveShortcuts(list)
            showShortcutPicker = null
        }, onClose = { showShortcutPicker = null })
    }
    if (showEntry != null) {
        EntrySheet(vm, showEntry!!, onClose = { showEntry = null })
    }
}

// Daily hadith card (Home screen, below the shortcut tiles).
// Green theme; Urdu text in Jameel Noori Nastaliq (RTL, tall lines),
// English text otherwise; reference (book + hadith number) always shown.
@Composable
fun DailyHadithCard(vm: AppViewModel, hadith: DailyHadith) {
    val lang by vm.lang.collectAsState()
    val isUrdu = lang == "ur"
    val text = if (isUrdu) hadith.textUrdu.ifBlank { hadith.textEn } else hadith.textEn.ifBlank { hadith.textUrdu }
    val ref = if (isUrdu) {
        "${vm.t("bukhari")} — ${vm.t("hadith")} ${hadith.number}"
    } else {
        val section = if (hadith.section.isNotBlank()) " · ${hadith.section}" else ""
        "${vm.t("bukhari")}$section — ${vm.t("hadith")} ${hadith.number}"
    }
    AnimatedContent(
        targetState = hadith.number,
        transitionSpec = { fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(300)) },
        label = "hadith"
    ) {
        Card(
            Modifier.fillMaxWidth().animateContentSize(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2EA))
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📖", fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(vm.t("dailyHadith"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Green)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text,
                    fontSize = if (isUrdu) 17.sp else 14.sp,
                    fontFamily = if (isUrdu) UrduFontFamily else null,
                    color = Color(0xFF1B5E20),
                    textAlign = if (isUrdu) TextAlign.End else TextAlign.Start,
                    lineHeight = if (isUrdu) 32.sp else 20.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(ref, fontSize = 11.sp, color = Green, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

fun categoryIcon(name: String): String = when (name.trim().lowercase()) {
    "salary" -> "💰"; "business" -> "💼"; "other income" -> "💵"
    "food" -> "🍔"; "transport" -> "🚗"; "shopping" -> "🛍️"
    "bills" -> "🧾"; "rent" -> "🏠"; "other expense" -> "📦"
    else -> "📁"
}

fun shortcutIcon(key: String, categories: List<Category> = emptyList()): String = when {
    key == "report" -> "📊"; key == "budgets" -> "💰"; key == "planned" -> "🔁"
    key == "reminders" -> "🔔"; key == "loan" -> "🤝"; key == "bills" -> "🧾"
    key == "family" -> "👨‍👩‍👧"; key == "backup" -> "💾"
    key == "search" -> "🔍"; key == "calculator" -> "🧮"; key == "categories" -> "🗂️"
    key.startsWith("category:") -> {
        val id = key.removePrefix("category:").toIntOrNull()
        val name = categories.firstOrNull { it.id == id }?.name ?: ""
        categoryIcon(name)
    }
    else -> "📌"
}

@Composable
fun ShortcutPicker(vm: AppViewModel, slot: Int, onPick: (String) -> Unit, onRemove: () -> Unit, onClose: () -> Unit) {
    val categories by vm.categories.collectAsState()
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(vm.t("chooseShortcut")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                listOf("report","budgets","planned","reminders","loan","bills","family","backup","search","calculator","categories").forEach { k ->
                    Text(shortcutLabel(vm, k), modifier = Modifier.fillMaxWidth().clickable { onPick(k) }.padding(vertical = 8.dp))
                }
                Divider()
                categories.forEach { c ->
                    Text("${categoryIcon(c.name)} ${c.name}", modifier = Modifier.fillMaxWidth().clickable { onPick("category:${c.id}") }.padding(vertical = 8.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onRemove) { Text(vm.t("removeShortcut")) } },
        dismissButton = { TextButton(onClick = onClose) { Text(vm.t("cancel")) } }
    )
}

@Composable
fun EntrySheet(vm: AppViewModel, type: String, categoryId: Int? = null, onClose: () -> Unit) {
    val categories by vm.categories.collectAsState()
    val members by vm.members.collectAsState()
    val isAdmin by vm.isAdmin.collectAsState()
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var catId by remember { mutableStateOf(categoryId ?: categories.firstOrNull { it.type == type }?.id ?: 0) }
    var memberId by remember { mutableStateOf(0) }
    var receiptUri by remember { mutableStateOf("") }
    val ctx = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) receiptUri = uri.toString()
    }
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val spoken = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: ""
            val num = Regex("\\d+").find(spoken)?.value
            if (num != null) amount = num
            if (note.isBlank()) note = spoken.filterNot { it == '₹' || it == '$' || it == '€' || it == '£' || it == '¥' }.trim()
        }
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (type == "IN") vm.t("moneyIn") else vm.t("expense")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = amount, onValueChange = { if (it.all { c -> c.isDigit() }) amount = it }, label = { Text(vm.t("amount")) }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(vm.t("note")) }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }), modifier = Modifier.fillMaxWidth())
                if (categoryId == null) {
                    Text(vm.t("category"), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    categories.filter { it.type == type }.forEach { c ->
                        Row(Modifier.fillMaxWidth().clickable { keyboardController?.hide(); catId = c.id }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = catId == c.id, onClick = { keyboardController?.hide(); catId = c.id })
                            Text(c.name)
                        }
                    }
                }
                if (isAdmin && members.isNotEmpty()) {
                    Text(vm.t("memberName"), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    Row(Modifier.fillMaxWidth().clickable { keyboardController?.hide(); memberId = 0 }.padding(vertical = 4.dp)) {
                        RadioButton(selected = memberId == 0, onClick = { keyboardController?.hide(); memberId = 0 }); Text(vm.t("admin"))
                    }
                    members.forEach { m ->
                        Row(Modifier.fillMaxWidth().clickable { keyboardController?.hide(); memberId = m.id }.padding(vertical = 4.dp)) {
                            RadioButton(selected = memberId == m.id, onClick = { keyboardController?.hide(); memberId = m.id }); Text(m.name)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedButton(onClick = { pickImage.launch("image/*") }, modifier = Modifier.weight(1f)) { Text("📷 ${vm.t("receiptPhoto")}", maxLines = 1) }
                    OutlinedButton(onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ur-PK")
                        }
                        try { speech.launch(intent) } catch (e: Exception) { vm.showToast(vm.t("voiceNotSupported")) }
                    }, modifier = Modifier.weight(1f)) { Text("🎤 ${vm.t("voiceEntry")}", maxLines = 1) }
                }
                if (receiptUri.isNotEmpty()) Text("✓ ${vm.t("receiptPhoto")}", color = Green, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(onClick = {
                keyboardController?.hide()
                val amt = amount.toLongOrNull() ?: 0L
                if (amt > 0 && catId != 0) {
                    val mName = if (memberId == 0) "Admin" else members.firstOrNull { it.id == memberId }?.name ?: "Admin"
                    vm.addEntry(catId, amt, note, System.currentTimeMillis(), memberId, mName, receiptUri)
                    onClose()
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text(vm.t("save")) }
        },
        dismissButton = { TextButton(onClick = onClose) { Text(vm.t("cancel")) } }
    )
}
