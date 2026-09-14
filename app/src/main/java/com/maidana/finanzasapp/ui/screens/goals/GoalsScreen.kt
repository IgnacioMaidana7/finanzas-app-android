package com.maidana.finanzasapp.ui.screens.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun GoalsScreen(
    onNavigateToNewGoal: () -> Unit,
    onNavigateToGoalDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GoalsViewModel = viewModel()
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Objetivos de Ahorro",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Metas Financieras y Aportes",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Gestiona tus metas de ahorro, registra aportes periódicos y sigue el avance hacia cada objetivo.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Button(
            onClick = onNavigateToNewGoal,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Crear Nuevo Objetivo")
        }

        OutlinedButton(
            onClick = { onNavigateToGoalDetail("meta-vacaciones") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ver Detalle de Objetivo (Ej: Meta Vacaciones)")
        }
    }
}
