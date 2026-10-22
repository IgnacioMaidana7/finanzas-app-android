package com.maidana.finanzasapp.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maidana.finanzasapp.ui.components.CurrencyUtils
import java.time.YearMonth
import java.time.ZoneOffset

@Composable
fun CloudSummaryCard(viewModel: CloudSummaryViewModel = viewModel()) {
    var monthText by rememberSaveable { mutableStateOf(YearMonth.now(ZoneOffset.UTC).toString()) }
    val month = YearMonth.parse(monthText)
    val state by viewModel.uiState.collectAsState()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Resumen mensual", style = MaterialTheme.typography.titleMedium)
            Text("Movimientos del mes en UTC · pesos enteros", style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(enabled = month.year > 2000 || month.monthValue > 1, onClick = {
                    monthText = month.minusMonths(1).toString()
                    viewModel.reset()
                }) { Text("Anterior") }
                Text(monthText, modifier = Modifier.padding(top = 12.dp))
                TextButton(enabled = month < YearMonth.now(ZoneOffset.UTC), onClick = {
                    monthText = month.plusMonths(1).toString()
                    viewModel.reset()
                }) { Text("Siguiente") }
            }
            when (val current = state) {
                CloudSummaryState.Idle -> Text("Consultá los ingresos y gastos de este período.")
                CloudSummaryState.Loading -> {
                    CircularProgressIndicator()
                    Text("Consultando resumen…")
                }
                is CloudSummaryState.Failed -> Text(current.message, color = MaterialTheme.colorScheme.error)
                is CloudSummaryState.Ready -> {
                    if (current.summary.month == monthText) {
                        Text("Ingresos: ${CurrencyUtils.format(current.summary.income)}")
                        Text("Gastos: ${CurrencyUtils.format(current.summary.expense)}")
                        Text("Resultado del mes: ${CurrencyUtils.format(current.summary.net)}")
                        Text("${current.summary.transactionCount} movimientos")
                        Text("No incluye saldos iniciales de las cuentas.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Button(enabled = state != CloudSummaryState.Loading, onClick = { viewModel.load(monthText) }) {
                Text(if (state is CloudSummaryState.Failed) "Reintentar" else "Consultar resumen")
            }
        }
    }
}
