package com.mianzahid.expenseentry.ui.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.mianzahid.expenseentry.model.TransactionType
import com.mianzahid.expenseentry.ui.components.GlassCard
import com.mianzahid.expenseentry.ui.components.TintedIconBadge
import com.mianzahid.expenseentry.ui.screens.*
import com.mianzahid.expenseentry.ui.theme.*
import com.mianzahid.expenseentry.viewmodel.HisabooViewModel

object HisabooRoutes {
    const val HOME = "home"
    const val CATEGORIES = "categories"
    const val TOOLS = "tools"
    const val LOAN = "loan"
    const val CATEGORY_DETAIL = "category_detail/{categoryId}"
    const val REPORT = "report"
    const val BUDGET = "budget"
    const val PLANNED_PAYMENTS = "planned_payments"
    const val SEARCH = "search"
    const val REMINDERS = "reminders"
    const val UTILITY_BILLS = "utility_bills"
    const val FAMILY = "family"
    const val BACKUP = "backup"
    const val CHANGE_PIN = "change_pin"

    fun categoryDetail(categoryId: String) = "category_detail/$categoryId"
}

private data class DockTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun HisabooMainScaffold(
    viewModel: HisabooViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var quickEntryType by remember { mutableStateOf<TransactionType?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }

    // Auto-lock immediately whenever app goes to background, minimized, or screen turns off
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.isAppLocked = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Intercept Back press on Home screen to ask permission before exiting & locking
    BackHandler(enabled = !viewModel.isAppLocked && currentRoute == HisabooRoutes.HOME) {
        showExitDialog = true
    }

    val dockTabs = listOf(
        DockTab(HisabooRoutes.HOME, "Home", Icons.Rounded.Home),
        DockTab(HisabooRoutes.CATEGORIES, "Categories", Icons.Rounded.Category),
        DockTab(HisabooRoutes.TOOLS, "Tools", Icons.Rounded.Calculate),
        DockTab(HisabooRoutes.LOAN, "Loan", Icons.Rounded.Handshake)
    )

    val showFloatingDock = currentRoute in dockTabs.map { it.route }

    if (viewModel.isAppLocked) {
        AppLockScreen(
            correctPin = viewModel.currentPin,
            fingerprintEnabled = viewModel.fingerprintEnabled,
            onUnlock = { viewModel.isAppLocked = false }
        )
        return
    }

    Scaffold(
        containerColor = BgObsidian,
        bottomBar = {
            if (showFloatingDock) {
                FloatingGlassCapsuleDock(
                    tabs = dockTabs,
                    currentRoute = currentRoute,
                    onSelectTab = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HisabooRoutes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .background(BgObsidian)
                .padding(bottom = if (showFloatingDock) 0.dp else innerPadding.calculateBottomPadding())
        ) {
            composable(HisabooRoutes.HOME) {
                HomeScreen(
                    viewModel = viewModel,
                    onQuickCashIn = { quickEntryType = TransactionType.CASH_IN },
                    onQuickCashOut = { quickEntryType = TransactionType.CASH_OUT },
                    onNavigate = { route -> navController.navigate(route) }
                )
            }

            composable(HisabooRoutes.CATEGORIES) {
                CategoriesScreen(
                    viewModel = viewModel,
                    onOpenCategory = { catId ->
                        navController.navigate(HisabooRoutes.categoryDetail(catId))
                    }
                )
            }

            composable(HisabooRoutes.TOOLS) {
                ToolsScreen(viewModel = viewModel)
            }

            composable(HisabooRoutes.LOAN) {
                LoanScreen(
                    viewModel = viewModel,
                    onBack = {
                        navController.navigate(HisabooRoutes.HOME) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(
                route = HisabooRoutes.CATEGORY_DETAIL,
                arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
            ) { backStack ->
                val catId = backStack.arguments?.getString("categoryId") ?: ""
                CategoryDetailScreen(
                    categoryId = catId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(HisabooRoutes.REPORT) {
                MonthlyReportScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(HisabooRoutes.BUDGET) {
                BudgetScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(HisabooRoutes.PLANNED_PAYMENTS) {
                PlannedPaymentsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(HisabooRoutes.SEARCH) {
                SearchScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(HisabooRoutes.REMINDERS) {
                RemindersScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(HisabooRoutes.UTILITY_BILLS) {
                UtilityBillsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(HisabooRoutes.FAMILY) {
                FamilyScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(HisabooRoutes.BACKUP) {
                BackupExportScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(HisabooRoutes.CHANGE_PIN) {
                ChangePinScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        quickEntryType?.let { entryType ->
            QuickTransactionDialog(
                type = entryType,
                categories = viewModel.categories.filter { it.type == entryType },
                defaultDate = viewModel.todayFormatted(),
                onDismiss = { quickEntryType = null },
                onSave = { catId, catName, amount, note, date ->
                    viewModel.addTransaction(catId, catName, amount, note, date, entryType)
                    quickEntryType = null
                }
            )
        }

        if (showExitDialog) {
            Dialog(onDismissRequest = { showExitDialog = false }) {
                GlassCard(cornerRadius = 24.dp) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TintedIconBadge(
                            icon = Icons.Rounded.Lock,
                            tint = EmeraldPrimary,
                            size = 40.dp,
                            isCircle = true
                        )
                        Text(
                            text = "Exit & Lock Hisaboo?",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Do you want to exit the app? Hisaboo will automatically lock for your security.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showExitDialog = false },
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = SurfaceGlass,
                                contentColor = TextPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                showExitDialog = false
                                viewModel.isAppLocked = true
                                (context as? Activity)?.finish()
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = OnEmeraldPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("Exit & Lock", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingGlassCapsuleDock(
    tabs: List<DockTab>,
    currentRoute: String?,
    onSelectTab: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(SurfaceGlass.copy(alpha = 0.96f))
                .border(1.dp, BorderGlass, CircleShape)
                .padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = currentRoute == tab.route
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) EmeraldPrimary else Color.Transparent)
                        .clickable { onSelectTab(tab.route) }
                        .padding(
                            horizontal = if (isSelected) 14.dp else 12.dp,
                            vertical = 9.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (isSelected) OnEmeraldPrimary else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    AnimatedVisibility(visible = isSelected) {
                        Text(
                            text = tab.label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnEmeraldPrimary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
