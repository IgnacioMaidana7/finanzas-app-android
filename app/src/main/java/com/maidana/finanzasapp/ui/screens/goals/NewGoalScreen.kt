package com.maidana.finanzasapp.ui.screens.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maidana.finanzasapp.ui.components.AppTopBar
import com.maidana.finanzasapp.ui.components.ThousandsSeparatorVisualTransformation

@Composable
fun NewGoalScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GoalsViewModel = viewModel()
) {
    val saveState by viewModel.saveState.collectAsState()

    var title by remember { mutableStateOf("") }
    var targetAmountText by remember { mutableStateOf("") }
    var initialAmountText by remember { mutableStateOf("") }
    var targetDateText by remember { mutableStateOf("Diciembre 2026") }
    var validationError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetSaveState()
        }
    }

    LaunchedEffect(saveState) {
        if (saveState is SaveGoalState.Success) {
            viewModel.resetSaveState()
            onNavigateBack()
        }
    }

    val isLoading = saveState is SaveGoalState.Loading

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Nuevo Objetivo",
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Mensaje de error de validación o del backend
            val errorMessage = validationError ?: (saveState as? SaveGoalState.Error)?.message
            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Nombre del objetivo
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    validationError = null
                },
                label = { Text("Nombre de la meta") },
                placeholder = { Text("Ej: Fondo de Emergencia, Vacaciones") },
                leadingIcon = {
                    Icon(Icons.Default.Flag, contentDescription = null)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Monto Objetivo
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Monto Objetivo a Alcanzar",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = targetAmountText,
                        onValueChange = { input ->
                            val digitsOnly = input.filter { it.isDigit() }
                            if (digitsOnly.length <= 15) {
                                targetAmountText = if (digitsOnly.length > 1) digitsOnly.trimStart('0').ifEmpty { "0" } else digitsOnly
                                validationError = null
                            }
                        },
                        placeholder = { Text("500000", style = MaterialTheme.typography.headlineMedium) },
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

            // Monto Inicial Opcional
            OutlinedTextField(
                value = initialAmountText,
                onValueChange = { input ->
                    val digitsOnly = input.filter { it.isDigit() }
                    if (digitsOnly.length <= 15) {
                        initialAmountText = if (digitsOnly.length > 1) digitsOnly.trimStart('0').ifEmpty { "0" } else digitsOnly
                        validationError = null
                    }
                },
                label = { Text("Monto inicial ya ahorrado (Opcional)") },
                prefix = { Text("$ ") },
                visualTransformation = ThousandsSeparatorVisualTransformation(),
                leadingIcon = {
                    Icon(Icons.Default.Savings, contentDescription = null)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Fecha Objetivo
            OutlinedTextField(
                value = targetDateText,
                onValueChange = { targetDateText = it },
                label = { Text("Fecha objetivo estimativa") },
                leadingIcon = {
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val trimmedName = title.trim()
                    if (trimmedName.isBlank()) {
                        validationError = "Por favor ingresa un nombre para el objetivo."
                        return@Button
                    }
                    val target = targetAmountText.filter { it.isDigit() }.toLongOrNull() ?: 0L
                    if (target <= 0L) {
                        validationError = "El monto objetivo debe ser mayor a $ 0."
                        return@Button
                    }
                    val initial = initialAmountText.filter { it.isDigit() }.toLongOrNull() ?: 0L
                    if (initial < 0L) {
                        validationError = "El monto inicial no puede ser negativo."
                        return@Button
                    }
                    if (initial > target) {
                        validationError = "El monto inicial no puede ser mayor al monto objetivo."
                        return@Button
                    }

                    validationError = null
                    viewModel.addGoal(
                        name = trimmedName,
                        targetAmount = target,
                        initialAmount = initial,
                        targetDateString = targetDateText
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
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Crear Objetivo", fontWeight = FontWeight.Bold)
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
