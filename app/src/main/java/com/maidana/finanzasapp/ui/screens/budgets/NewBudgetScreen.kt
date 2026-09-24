package com.maidana.finanzasapp.ui.screens.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maidana.finanzasapp.data.model.CategoryConstants
import com.maidana.finanzasapp.data.model.TransactionType
import com.maidana.finanzasapp.ui.components.AppTopBar
import com.maidana.finanzasapp.ui.components.CategorySelectorSection
import com.maidana.finanzasapp.ui.components.ThousandsSeparatorVisualTransformation
import com.maidana.finanzasapp.ui.screens.categories.CategoryViewModel
import java.util.Calendar

@Composable
fun NewBudgetScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BudgetsViewModel = viewModel(),
    categoryViewModel: CategoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    val defaultExpenseCategory = CategoryConstants.PREDEFINED_EXPENSE_CATEGORIES.firstOrNull()?.name ?: "Alimentación"
    val defaultExpenseCategoryId = CategoryConstants.PREDEFINED_EXPENSE_CATEGORIES.firstOrNull()?.id.orEmpty()

    var selectedCategory by remember { mutableStateOf(defaultExpenseCategory) }
    var selectedCategoryId by remember { mutableStateOf(defaultExpenseCategoryId) }
    var limitAmountText by remember { mutableStateOf("") }
    var selectedMonth by remember { mutableIntStateOf(uiState.selectedMonth) }
    var selectedYear by remember { mutableIntStateOf(uiState.selectedYear) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val isLoading = saveState is SaveBudgetState.Loading

    LaunchedEffect(saveState) {
        if (saveState is SaveBudgetState.Success) {
            viewModel.resetSaveState()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Nuevo Presupuesto",
                onBackClick = onNavigateBack
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Mensaje de error (validación local o del ViewModel)
            val activeError = validationError ?: (saveState as? SaveBudgetState.Error)?.message
            if (activeError != null) {
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
                            text = activeError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Selección de categoría (Centralizada con categorías del sistema y personalizadas)
            CategorySelectorSection(
                selectedCategoryName = selectedCategory,
                onCategorySelected = { cat ->
                    selectedCategory = cat.name
                    selectedCategoryId = cat.id
                    validationError = null
                },
                type = TransactionType.GASTO,
                categoryViewModel = categoryViewModel,
                title = "Selecciona la Categoría"
            )

            // Input Límite Mensual en pesos enteros
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Límite Máximo Mensual",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = limitAmountText,
                        onValueChange = { input ->
                            val digitsOnly = input.filter { it.isDigit() }
                            if (digitsOnly.length <= 15) {
                                limitAmountText = if (digitsOnly.length > 1) digitsOnly.trimStart('0').ifEmpty { "0" } else digitsOnly
                                validationError = null
                            }
                        },
                        placeholder = { Text("150000", style = MaterialTheme.typography.headlineMedium) },
                        prefix = {
                            Text(
                                text = "$ ",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        visualTransformation = ThousandsSeparatorVisualTransformation(),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Selector de Período (Mes y Año)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Período a Presupuestar",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (selectedMonth == 1) {
                                selectedMonth = 12
                                selectedYear -= 1
                            } else {
                                selectedMonth -= 1
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Mes anterior",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${BudgetsViewModel.getMonthName(selectedMonth)} $selectedYear",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(onClick = {
                            if (selectedMonth == 12) {
                                selectedMonth = 1
                                selectedYear += 1
                            } else {
                                selectedMonth += 1
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Mes siguiente",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botones de acción
            Button(
                onClick = {
                    val limit = limitAmountText.toLongOrNull() ?: 0L
                    if (limit <= 0L) {
                        validationError = "Por favor ingresa un límite mayor a $ 0."
                        return@Button
                    }
                    validationError = null
                    viewModel.saveBudget(
                        category = selectedCategory,
                        limitAmount = limit,
                        month = selectedMonth,
                        year = selectedYear,
                        categoryId = selectedCategoryId
                    )
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar Presupuesto", fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = onNavigateBack,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancelar")
            }
        }
    }
}
