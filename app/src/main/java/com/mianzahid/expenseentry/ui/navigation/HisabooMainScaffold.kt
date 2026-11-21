package com.mianzahid.expenseentry.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.mianzahid.expenseentry.model.TransactionType
import com.mianzahid.expenseentry.ui.screens.*
import com.mianzahid.expenseentry.ui.theme.*
import com.mianzahid.expenseentry.viewmodel.HisabooViewModel

object HisabooRoutes {
    const val HOME = "home"
    const val CATEGORIES = "categories"
    const val TOOLS = "tools"
    const val CATEGORY_DETAIL = "category_detail/{categoryId}"
    const val REPORT = "report"
    const val BUDGET = "budget"
    const val PLANNED_PAYMENTS = "planned_payments"
    const val SEARCH = "search"
    const val REMINDERS = "reminders"
    const val LOAN = "loan"
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
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var quickEntryType by remember { mutableStateOf<TransactionType?>(null) }

    val dockTabs = listOf(
        DockTab(HisabooRoutes.HOME, "Home", Icons.Rounded.Home),
        DockTab(HisabooRoutes.CATEGORIES, "Categories", Icons.Rounded.Category),
        DockTab(HisabooRoutes.TOOLS, "Tools", Icons.Rounded.Calculate)
    )

    val showFloatingDock = currentRoute in dockTabs.map { it.route }

    if (viewModel.isAppLocked) {
        AppLockScreen(
            correctPin = viewModel.currentPin,
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

            composable(HisabooRoutes.LOAN) {
                LoanScreen(
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
            .padding(horizontal = 28.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(SurfaceGlass.copy(alpha = 0.96f))
                .border(1.dp, BorderGlass, CircleShape)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                            horizontal = if (isSelected) 18.dp else 14.dp,
                            vertical = 10.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (isSelected) OnEmeraldPrimary else TextSecondary,
                        modifier = Modifier.size(21.dp)
                    )
                    AnimatedVisibility(visible = isSelected) {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = OnEmeraldPrimary
                        )
                    }
                }
            }
        }
    }
}
