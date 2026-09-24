package com.maidana.finanzasapp.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maidana.finanzasapp.data.model.AccountType
import com.maidana.finanzasapp.ui.components.BalanceCard
import com.maidana.finanzasapp.ui.components.BudgetCard
import com.maidana.finanzasapp.ui.components.CurrencyUtils
import com.maidana.finanzasapp.ui.components.SavingsGoalCard
import com.maidana.finanzasapp.ui.components.StatCard
import com.maidana.finanzasapp.ui.components.ThousandsSeparatorVisualTransformation
import com.maidana.finanzasapp.ui.components.TransactionRow
import com.maidana.finanzasapp.ui.screens.accounts.AccountViewModel
import com.maidana.finanzasapp.ui.theme.ExpenseRed
import com.maidana.finanzasapp.ui.theme.IncomeGreen

@Composable
fun DashboardScreen(
    onNavigateToNewTransaction: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel(),
    accountViewModel: AccountViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showInitialBalanceDialog by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    if (showInitialBalanceDialog) {
        val currentCashInitial = uiState.accounts.firstOrNull { it.type == AccountType.CASH }?.initialBalance ?: 0L
        val currentDigitalInitial = uiState.accounts.firstOrNull { it.type == AccountType.DIGITAL }?.initialBalance ?: 0L

        EditInitialBalancesDialog(
            cashInitial = currentCashInitial,
            digitalInitial = currentDigitalInitial,
            onDismiss = { showInitialBalanceDialog = false },
            onSave = { cashVal, digitalVal ->
                accountViewModel.updateInitialBalances(cashVal, digitalVal) {
                    showInitialBalanceDialog = false
                    viewModel.loadData()
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Cabecera con Saludo y Salida
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.userName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column {
                    Text(
                        text = "¡Hola, ${uiState.userName}! 👋",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Resumen de tus finanzas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onLogout) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Cerrar sesión",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Banner de Error si falló la carga de datos
        if (uiState.isError) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.errorMessage ?: "Error al sincronizar datos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Button(
                        onClick = { viewModel.loadData() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Reintentar")
                    }
                }
            }
        }

        // Tarjeta Principal de Balance
        BalanceCard(
            totalBalance = uiState.totalBalance,
            cashBalance = uiState.cashBalance,
            digitalBalance = uiState.digitalBalance,
            monthlyIncome = uiState.monthlyIncome,
            monthlyExpense = uiState.monthlyExpense,
            onEditInitialBalances = { showInitialBalanceDialog = true }
        )

        // Tarjetas de Métricas Rápidas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Ahorro del Mes",
                value = CurrencyUtils.format(uiState.monthlySavings),
                icon = Icons.Default.Savings,
                iconColor = MaterialTheme.colorScheme.primary,
                subtitle = uiState.savingsRateText,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Gastos del Mes",
                value = CurrencyUtils.format(uiState.monthlyExpense),
                icon = Icons.Default.ArrowDownward,
                iconColor = ExpenseRed,
                subtitle = uiState.expenseRateText,
                modifier = Modifier.weight(1f)
            )
        }

        // Botón de Acción Rápida: Registrar Movimiento
        Button(
            onClick = onNavigateToNewTransaction,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Registrar Nuevo Movimiento", fontWeight = FontWeight.SemiBold)
        }

        // Resumen de Presupuestos (Mock)
        if (uiState.budgets.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Control de Presupuestos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                val topBudget = uiState.budgets.firstOrNull()
                if (topBudget != null) {
                    BudgetCard(budget = topBudget)
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Sin presupuestos este mes",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Configura tus límites mensuales en la sección de Presupuestos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Resumen de Objetivos de Ahorro (Reales desde Firestore)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Meta Destacada",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToGoals) {
                    Text("Ver todas")
                }
            }

            if (uiState.featuredGoal != null) {
                SavingsGoalCard(
                    goal = uiState.featuredGoal!!,
                    onClick = onNavigateToGoals
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    onClick = onNavigateToGoals
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Sin metas de ahorro creadas",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Toca aquí para definir tus objetivos y comenzar a ahorrar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Últimos Movimientos (Reales desde Firestore)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Últimos Movimientos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToTransactions) {
                    Text("Ver todos")
                }
            }

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.5.dp
                        )
                    }
                }

                uiState.isError -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = uiState.errorMessage ?: "No se pudieron cargar los movimientos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.loadData() }) {
                                Text("Reintentar")
                            }
                        }
                    }
                }

                uiState.recentTransactions.isEmpty() -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Sin movimientos registrados aún",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tus transacciones aparecerán aquí a medida que las registres.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    uiState.recentTransactions.forEach { tx ->
                        TransactionRow(
                            transaction = tx,
                            onClick = onNavigateToTransactions
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun EditInitialBalancesDialog(
    cashInitial: Long,
    digitalInitial: Long,
    onDismiss: () -> Unit,
    onSave: (cashInitial: Long, digitalInitial: Long) -> Unit
) {
    var cashText by remember { mutableStateOf(if (cashInitial > 0L) cashInitial.toString() else "") }
    var digitalText by remember { mutableStateOf(if (digitalInitial > 0L) digitalInitial.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Ajustar Saldos Iniciales",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Define cuánto dinero tenías en cada cuenta antes de comenzar a registrar tus movimientos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = cashText,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        cashText = digits
                    },
                    label = { Text("Saldo inicial en Efectivo") },
                    prefix = { Text("$ ") },
                    visualTransformation = ThousandsSeparatorVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = digitalText,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        digitalText = digits
                    },
                    label = { Text("Saldo inicial Billetera / Transf.") },
                    prefix = { Text("$ ") },
                    visualTransformation = ThousandsSeparatorVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cashVal = cashText.toLongOrNull() ?: 0L
                    val digitalVal = digitalText.toLongOrNull() ?: 0L
                    onSave(cashVal, digitalVal)
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

