package com.maidana.finanzasapp.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maidana.finanzasapp.data.model.AccountDistributionStat
import com.maidana.finanzasapp.data.model.AccountType
import com.maidana.finanzasapp.data.model.BudgetHealthStatus
import com.maidana.finanzasapp.data.model.BudgetPerformanceStat
import com.maidana.finanzasapp.data.model.CategoryExpenseStat
import com.maidana.finanzasapp.data.model.MonthlyEvolutionPoint
import com.maidana.finanzasapp.ui.theme.ExpenseRed
import com.maidana.finanzasapp.ui.theme.IncomeGreen
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

// =============================================================================
// GRÁFICO 1: COMPARATIVA INGRESOS VS GASTOS (TEMPORAL)
// =============================================================================

@Composable
fun MonthlyIncomeVsExpenseBarChart(
    points: List<MonthlyEvolutionPoint>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ingresos vs Gastos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Leyenda compacta
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(IncomeGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ingresos",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ExpenseRed)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Gastos",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (points.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sin datos en el período seleccionado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (points.size == 1) {
                // Período de 1 mes: Vista comparativa ampliada
                val point = points.first()
                val total = point.income + point.expense
                val incFraction = if (total > 0L) (point.income.toFloat() / total.toFloat()) else 0f
                val expFraction = if (total > 0L) (point.expense.toFloat() / total.toFloat()) else 0f

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Barra dividida
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        if (incFraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(incFraction)
                                    .fillMaxHeight()
                                    .background(IncomeGreen)
                            )
                        }
                        if (incFraction > 0f && expFraction > 0f) {
                            Spacer(modifier = Modifier.width(2.dp))
                        }
                        if (expFraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(expFraction)
                                    .fillMaxHeight()
                                    .background(ExpenseRed)
                            )
                        }
                    }

                    // Valores
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Ingresos (${(incFraction * 100).roundToInt()}%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyUtils.format(point.income),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Gastos (${(expFraction * 100).roundToInt()}%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyUtils.format(point.expense),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                        }
                    }

                    // Indicador de Déficit si los gastos superan los ingresos
                    if (point.isDeficit) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(ExpenseRed.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "⚠️ En este mes los gastos superaron a los ingresos por ${CurrencyUtils.format(point.expense - point.income)}.",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = ExpenseRed
                            )
                        }
                    }
                }
            } else {
                // Período de varios meses: Barras agrupadas lado a lado
                val maxAmount = points.maxOfOrNull { max(it.income, it.expense) }?.coerceAtLeast(1L) ?: 1L
                val hasAnyDeficit = points.any { it.isDeficit }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    points.forEach { point ->
                        val incomeHeight = (point.income.toFloat() / maxAmount.toFloat()).coerceIn(0.04f, 1f)
                        val expenseHeight = (point.expense.toFloat() / maxAmount.toFloat()).coerceIn(0.04f, 1f)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Alerta de déficit encima de la columna si ocurrió
                            if (point.isDeficit) {
                                Text(
                                    text = "!",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            } else {
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            // Barras agrupadas
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // Barra Ingreso
                                Box(
                                    modifier = Modifier
                                        .weight(0.4f)
                                        .fillMaxHeight(incomeHeight)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(IncomeGreen)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                // Barra Gasto
                                Box(
                                    modifier = Modifier
                                        .weight(0.4f)
                                        .fillMaxHeight(expenseHeight)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(ExpenseRed)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = point.month,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                color = if (point.isDeficit) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (point.isDeficit) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                if (hasAnyDeficit) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "(!) Indica meses donde el gasto superó a los ingresos.",
                        style = MaterialTheme.typography.labelSmall,
                        color = ExpenseRed.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

// Compatibilidad retroactiva para llamadas simples
@Composable
fun ComparativeIncomeExpenseChart(
    income: Long,
    expense: Long,
    modifier: Modifier = Modifier
) {
    MonthlyIncomeVsExpenseBarChart(
        points = listOf(
            MonthlyEvolutionPoint(
                month = "Período",
                income = income,
                expense = expense,
                isDeficit = expense > income
            )
        ),
        modifier = modifier
    )
}

// =============================================================================
// GRÁFICO 2: DISTRIBUCIÓN DE GASTOS POR CATEGORÍA (TOP 5 + OTROS)
// =============================================================================

@Composable
fun CategoryDistributionCard(
    categories: List<CategoryExpenseStat>,
    allCategories: List<CategoryExpenseStat> = categories,
    modifier: Modifier = Modifier
) {
    var showAll by remember { mutableStateOf(false) }
    val displayCategories = if (showAll) allCategories else categories
    val hasOthers = allCategories.size > 5

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gastos por Categoría",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (hasOthers) {
                    TextButton(onClick = { showAll = !showAll }) {
                        Text(
                            text = if (showAll) "Ver principales" else "Ver todas (${allCategories.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (displayCategories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se registraron gastos en el período seleccionado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Barra segmentada multicolor superior
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                ) {
                    displayCategories.forEach { cat ->
                        Box(
                            modifier = Modifier
                                .weight(cat.percentage.coerceAtLeast(0.02f))
                                .fillMaxHeight()
                                .background(cat.color)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Lista detallada
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    displayCategories.forEach { cat ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(cat.color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (cat.isOthers) "${cat.category} (${cat.subcategoriesCount})" else cat.category,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (cat.isOthers) FontWeight.Medium else FontWeight.Normal
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = CurrencyUtils.format(cat.amount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${(cat.percentage * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// GRÁFICO 3: EVOLUCIÓN DEL SALDO (PATRIMONIO LÍQUIDO ACUMULADO)
// =============================================================================

@Composable
fun EvolutionBarChart(
    points: List<MonthlyEvolutionPoint>,
    modifier: Modifier = Modifier
) {
    val maxBalance = points.maxOfOrNull { abs(it.balance) }?.coerceAtLeast(1L) ?: 1L

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Evolución del Saldo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Patrimonio líquido acumulado mes a mes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val lastBalance = points.lastOrNull()?.balance ?: 0L
                Text(
                    text = CurrencyUtils.format(lastBalance),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (lastBalance >= 0L) MaterialTheme.colorScheme.primary else ExpenseRed
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (points.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sin datos en el período seleccionado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    points.forEach { point ->
                        val heightFraction = (abs(point.balance).toFloat() / maxBalance.toFloat()).coerceIn(0.12f, 1f)

                        val animatedHeight by animateFloatAsState(
                            targetValue = heightFraction,
                            animationSpec = tween(durationMillis = 500),
                            label = "barHeight"
                        )

                        val barColor = when {
                            point.balance > 0L -> MaterialTheme.colorScheme.primary
                            point.balance < 0L -> ExpenseRed
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.55f)
                                    .fillMaxHeight(animatedHeight)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(barColor)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = point.month,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// GRÁFICO 4: DISTRIBUCIÓN POR CUENTA (EFECTIVO VS DIGITAL)
// =============================================================================

@Composable
fun AccountDistributionCard(
    accounts: List<AccountDistributionStat>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Distribución por Cuenta",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Dónde se encuentra tu dinero actualmente",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (accounts.isEmpty()) {
                Text(
                    text = "No hay cuentas configuradas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Barra de distribución proporcional
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                ) {
                    accounts.forEach { acc ->
                        val weight = acc.percentage.coerceAtLeast(0.03f)
                        Box(
                            modifier = Modifier
                                .weight(weight)
                                .fillMaxHeight()
                                .background(acc.color)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Detalle por cuenta
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    accounts.forEach { acc ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(acc.color.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (acc.type == AccountType.CASH) {
                                            Icons.Default.Payments
                                        } else {
                                            Icons.Default.AccountBalanceWallet
                                        },
                                        contentDescription = null,
                                        tint = acc.color,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = acc.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = CurrencyUtils.format(acc.balance),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (acc.balance >= 0L) MaterialTheme.colorScheme.onSurface else ExpenseRed
                                )
                                Text(
                                    text = "${(acc.percentage * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// GRÁFICO 5: PRESUPUESTO VS GASTO REAL
// =============================================================================

@Composable
fun BudgetPerformanceCard(
    budgets: List<BudgetPerformanceStat>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Presupuesto vs Gasto Real",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Seguimiento de consumo de límites mensuales",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (budgets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay presupuestos activos en este período.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    budgets.forEach { item ->
                        val statusColor = when (item.status) {
                            BudgetHealthStatus.NORMAL -> IncomeGreen
                            BudgetHealthStatus.WARNING -> Color(0xFFF59E0B) // Amber
                            BudgetHealthStatus.EXCEEDED -> ExpenseRed
                        }

                        val statusLabel = when (item.status) {
                            BudgetHealthStatus.NORMAL -> "En orden"
                            BudgetHealthStatus.WARNING -> "Cerca del límite"
                            BudgetHealthStatus.EXCEEDED -> "Excedido"
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Cabecera: Categoría y Badge de estado
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.category,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(statusColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "$statusLabel (${(item.percentage * 100).roundToInt()}%)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                }
                            }

                            // Barra de progreso
                            LinearProgressIndicator(
                                progress = { item.percentage.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = statusColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            // Montos: Gastado vs Límite
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Gastado: ${CurrencyUtils.format(item.spentAmount)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Límite: ${CurrencyUtils.format(item.limitAmount)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
