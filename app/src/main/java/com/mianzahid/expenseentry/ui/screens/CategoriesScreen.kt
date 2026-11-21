package com.mianzahid.expenseentry.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mianzahid.expenseentry.model.*
import com.mianzahid.expenseentry.ui.components.GlassCard
import com.mianzahid.expenseentry.ui.components.PillSegmentedSwitcher
import com.mianzahid.expenseentry.ui.components.TintedIconBadge
import com.mianzahid.expenseentry.ui.theme.*
import com.mianzahid.expenseentry.viewmodel.HisabooViewModel

@Composable
fun CategoriesScreen(
    viewModel: HisabooViewModel,
    onOpenCategory: (String) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }

    val activeType = if (selectedTabIndex == 0) TransactionType.CASH_OUT else TransactionType.CASH_IN
    val filteredCategories = viewModel.categories.filter { it.type == activeType }
    val typeTotal = filteredCategories.sumOf { cat ->
        viewModel.transactions.filter { it.categoryId == cat.id }.sumOf { it.amount }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgObsidian)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Permanent Record",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(EmeraldPrimary)
                        .clickable { showNewCategoryDialog = true }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "New Category",
                        tint = OnEmeraldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "+ New",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = OnEmeraldPrimary
                    )
                }
            }
        }

        item {
            PillSegmentedSwitcher(
                options = listOf("Cash Out", "Cash In"),
                selectedIndex = selectedTabIndex,
                onSelect = { selectedTabIndex = it },
                activeColor = if (selectedTabIndex == 0) CashOutRed else EmeraldPrimary,
                activeTextColor = OnEmeraldPrimary
            )
        }

        items(filteredCategories, key = { it.id }) { category ->
            val catEntries = viewModel.transactions.filter { it.categoryId == category.id }
            val catTotal = catEntries.sumOf { it.amount }
            val progress = if (typeTotal > 0.0) (catTotal / typeTotal).toFloat().coerceIn(0f, 1f) else 0f
            val (icon, tint) = getCategoryVisuals(category.name, category.type)
            val verb = if (category.type == TransactionType.CASH_IN) "Received" else "Spent"

            GlassCard(
                modifier = Modifier.clickable { onOpenCategory(category.id) },
                cornerRadius = 22.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        TintedIconBadge(icon = icon, tint = tint, size = 44.dp)
                        Column {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "${catEntries.size} entries • $verb",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = formatRs(catTotal),
                            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            color = if (category.type == TransactionType.CASH_IN) CashInGreen else TextPrimary
                        )
                        IconButton(
                            onClick = { viewModel.deleteCategory(category.id) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete Category",
                                tint = TextMuted,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(SurfaceGlass)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(tint)
                    )
                }
            }
        }
    }

    if (showNewCategoryDialog) {
        NewCategoryDialog(
            onDismiss = { showNewCategoryDialog = false },
            onAdd = { name, type ->
                viewModel.addCategory(name, type)
                showNewCategoryDialog = false
            }
        )
    }
}
