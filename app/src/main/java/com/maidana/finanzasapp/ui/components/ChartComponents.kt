package com.maidana.finanzasapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maidana.finanzasapp.data.model.CategoryExpenseStat
import com.maidana.finanzasapp.data.model.MonthlyEvolutionPoint
import com.maidana.finanzasapp.ui.theme.ExpenseRed
import com.maidana.finanzasapp.ui.theme.IncomeGreen
import kotlin.math.roundToInt

@Composable
fun ComparativeIncomeExpenseChart(
    income: Long,
    expense: Long,
    modifier: Modifier = Modifier
) {
    val total = income + expense
    val hasData = total > 0L
    val incomeFraction = if (hasData) (income.toFloat() / total.toFloat()) else 0f
    val expenseFraction = if (hasData) (expense.toFloat() / total.toFloat()) else 0f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Ingresos vs Gastos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Barra proporcional dividida
            if (!hasData) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sin movimientos en este período",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    if (incomeFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(incomeFraction)
                                .fillMaxHeight()
                                .background(IncomeGreen)
                        )
                    }
                    if (incomeFraction > 0f && expenseFraction > 0f) {
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    if (expenseFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(expenseFraction)
                                .fillMaxHeight()
                                .background(ExpenseRed)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Leyenda Ingresos
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(IncomeGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Ingresos (${(incomeFraction * 100).roundToInt()}%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyUtils.format(income),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }
                }

                // Leyenda Gastos
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(ExpenseRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Gastos (${(expenseFraction * 100).roundToInt()}%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyUtils.format(expense),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EvolutionBarChart(
    points: List<MonthlyEvolutionPoint>,
    modifier: Modifier = Modifier
) {
    val maxBalance = points.maxOfOrNull { it.balance.coerceAtLeast(0L) } ?: 0L

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Evolución del Saldo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(20.dp))

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
                        val heightFraction = if (maxBalance > 0L && point.balance > 0L) {
                            (point.balance.toFloat() / maxBalance.toFloat()).coerceIn(0.12f, 1f)
                        } else {
                            0.08f
                        }

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

@Composable
fun CategoryDistributionCard(
    categories: List<CategoryExpenseStat>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Gastos por Categoría",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (categories.isEmpty()) {
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
                // Barra segmentada multicolor
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                ) {
                    categories.forEach { cat ->
                        Box(
                            modifier = Modifier
                                .weight(cat.percentage.coerceAtLeast(0.02f))
                                .fillMaxHeight()
                                .background(cat.color)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Lista detallada de categorías ordenada descendentemente
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    categories.forEach { cat ->
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
                                    text = cat.category,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
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

