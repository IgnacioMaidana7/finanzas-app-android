package com.maidana.finanzasapp.ui.screens.indicators

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.maidana.finanzasapp.domain.*
import com.maidana.finanzasapp.ui.components.AppTopBar
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class IndicatorReference(val label: String, val amount: Long, val period: YearMonth? = null)

@Composable
fun IndicatorsScreen(viewModel: IndicatorsViewModel, references: List<IndicatorReference>, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    IndicatorsContent(state, references, viewModel::loadQuotes, viewModel::loadInflation, onBack)
}

@Composable
fun IndicatorsContent(
    state: IndicatorsUiState,
    references: List<IndicatorReference> = emptyList(),
    onRetryQuotes: () -> Unit = {},
    onRetryInflation: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var amount by rememberSaveable { mutableStateOf("") }
    var base by rememberSaveable { mutableStateOf(YearMonth.now().minusMonths(2).toString()) }
    var destination by rememberSaveable { mutableStateOf(YearMonth.now().minusMonths(1).toString()) }
    var market by rememberSaveable { mutableStateOf("oficial") }
    var result by rememberSaveable { mutableStateOf<String?>(null) }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }
    var chooseReference by rememberSaveable { mutableStateOf(false) }
    fun clearResult() { result = null; validationError = null }

    if (chooseReference) AlertDialog(
        onDismissRequest = { chooseReference = false },
        title = { Text("Usar un importe de la app") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            if (references.isEmpty()) Text("Todavía no hay presupuestos o metas disponibles.")
            references.forEach { reference ->
                TextButton(onClick = {
                    amount = reference.amount.toString()
                    reference.period?.let { base = it.toString() }
                    chooseReference = false; clearResult()
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("${reference.label}\n${money(BigDecimal(reference.amount))}")
                }
            }
        } },
        confirmButton = { TextButton(onClick = { chooseReference = false }) { Text("Cerrar") } }
    )

    Scaffold(topBar = { AppTopBar(title = "Indicadores", onBackClick = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Referencias para planificar", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Consultá cotizaciones y compará el poder de compra de tus importes. Los cálculos no modifican tus cuentas.", style = MaterialTheme.typography.bodyMedium)
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0; clearResult() }, text = { Text("Cotizaciones") }, icon = { Icon(Icons.Default.CurrencyExchange, null) })
                Tab(selected = tab == 1, onClick = { tab = 1; clearResult() }, text = { Text("Inflación") }, icon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, null) })
            }
            if (tab == 0) {
                IndicatorSection(state.quotes, onRetryQuotes) { snapshot ->
                    Text("Dólar en Argentina", style = MaterialTheme.typography.titleLarge)
                    SnapshotNote(snapshot.fetchedAt, snapshot.notice)
                    snapshot.values.forEach { quote ->
                        OutlinedCard(onClick = { market = quote.market; clearResult() }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(quote.name + if (quote.market == market) " · Seleccionada" else "", fontWeight = FontWeight.Bold)
                                Text("Compra: ${quote.buy?.let(::money) ?: "No disponible"}")
                                Text("Venta: ${quote.sell?.let(::money) ?: "No disponible"}")
                                Text("Actualización del proveedor: ${date(quote.updatedAt)}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Text("Fuente: DolarAPI · DolarHoy y Ámbito Financiero. Última referencia publicada, sin garantía de cotización operable.", style = MaterialTheme.typography.bodySmall)
                    AmountField(amount) { amount = it; clearResult() }
                    TextButton(onClick = { chooseReference = true }) { Text("Usar presupuesto o meta") }
                    Button(onClick = {
                        clearResult()
                        runCatching {
                            val pesos = DecimalAmounts.parse(amount) ?: error("Ingresá un importe válido.")
                            val quote = snapshot.values.firstOrNull { it.market == market } ?: error("Elegí una cotización.")
                            val usd = FinancialCalculations.dollarsForPesos(pesos, quote)
                            "USD ${decimal(usd)} estimados · ${quote.name} venta"
                        }.onSuccess { result = it }.onFailure { validationError = it.message }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Estimar equivalencia en USD") }
                    Text("Se usa venta para estimar una compra de dólares. No incluye comisiones, impuestos ni condiciones de una operación.", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                IndicatorSection(state.inflation, onRetryInflation) { snapshot ->
                    Text("Comparación por inflación", style = MaterialTheme.typography.titleLarge)
                    SnapshotNote(snapshot.fetchedAt, snapshot.notice)
                    val latest = snapshot.values.maxByOrNull { it.period }
                    if (latest != null) Text("Último mes publicado: ${latest.period} · ${decimal(latest.percent)} %")
                    Text("Fuente: ArgentinaDatos (servicio no oficial). Origen indicado por la API: BCRA.", style = MaterialTheme.typography.bodySmall)
                    AmountField(amount) { amount = it; clearResult() }
                    TextButton(onClick = { chooseReference = true }) { Text("Usar presupuesto o meta") }
                    Text("Comprobá el mes base del importe elegido. Se incluyen las tasas desde el mes siguiente hasta el mes destino.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(base, { base = it; clearResult() }, label = { Text("Mes base · AAAA-MM") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(destination, { destination = it; clearResult() }, label = { Text("Mes destino · AAAA-MM") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        clearResult()
                        runCatching {
                            val pesos = DecimalAmounts.parse(amount) ?: error("Ingresá un importe válido.")
                            val from = runCatching { YearMonth.parse(base.trim()) }.getOrElse { error("Mes base inválido. Usá AAAA-MM.") }
                            val to = runCatching { YearMonth.parse(destination.trim()) }.getOrElse { error("Mes destino inválido. Usá AAAA-MM.") }
                            val comparison = FinancialCalculations.adjustForInflation(pesos, from, to, snapshot.values)
                            "Importe de referencia: ${money(comparison.adjusted)}\nInflación acumulada: ${decimal(comparison.accumulatedPercent)} %"
                        }.onSuccess { result = it }.onFailure { validationError = it.message }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Calcular importe de referencia") }
                    Text("Describe variaciones pasadas del nivel general de precios. No predice el precio de una compra ni cambia presupuestos o metas.", style = MaterialTheme.typography.bodySmall)
                }
            }
            validationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            result?.let { Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun AmountField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value, { if (it.length <= 24) onChange(it) }, label = { Text("Importe en pesos") },
        supportingText = { Text("Ejemplo: 100000 o 1234,56") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun <T> IndicatorSection(load: IndicatorLoad<T>, retry: () -> Unit, content: @Composable (IndicatorSnapshot<T>) -> Unit) {
    when (load) {
        IndicatorLoad.Loading -> Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(24.dp)); Text("Consultando indicadores…")
        }
        is IndicatorLoad.Failed -> { Text(load.message, color = MaterialTheme.colorScheme.error); OutlinedButton(onClick = retry) { Text("Reintentar consulta") } }
        is IndicatorLoad.Ready -> { content(load.snapshot); TextButton(onClick = retry) { Text("Comprobar actualización") } }
    }
}

@Composable
private fun SnapshotNote(fetchedAt: Long, notice: String?) {
    notice?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    Text("Consulta: ${date(Instant.ofEpochMilli(fetchedAt))}", style = MaterialTheme.typography.bodySmall)
}

private fun date(value: Instant): String = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault()).format(value)
private fun decimal(value: BigDecimal): String = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-AR")).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }.format(value)
private fun money(value: BigDecimal): String = "$ ${decimal(value)}"
