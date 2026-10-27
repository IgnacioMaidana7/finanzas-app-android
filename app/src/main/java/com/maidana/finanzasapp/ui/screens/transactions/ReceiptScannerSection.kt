package com.maidana.finanzasapp.ui.screens.transactions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.maidana.finanzasapp.domain.DecimalAmounts
import com.maidana.finanzasapp.domain.ReceiptDraft
import java.math.RoundingMode

@Composable
fun ReceiptScannerSection(viewModel: ReceiptViewModel, enabled: Boolean, onApply: (ReceiptDraft, String) -> Unit) {
    val state by viewModel.state.collectAsState()
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture(), viewModel::cameraResult)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { viewModel.read(it) } }
    val reading = state is ReceiptState.Reading

    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Cargar desde un ticket", style = MaterialTheme.typography.titleMedium)
            Text("El texto se reconoce en este dispositivo. Revisarás los datos antes de crear el gasto.", style = MaterialTheme.typography.bodySmall)
            // Stacked controls also fit large system fonts and small phones.
            OutlinedButton(onClick = {
                try { camera.launch(viewModel.prepareCamera()) }
                catch (_: Exception) { viewModel.showError("No se pudo abrir la cámara. Podés elegir una imagen.") }
            }, enabled = enabled && !reading, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CameraAlt, null); Spacer(Modifier.width(8.dp)); Text("Tomar foto")
            }
            OutlinedButton(onClick = {
                try { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                catch (_: Exception) { viewModel.showError("No se pudo abrir el selector de imágenes.") }
            }, enabled = enabled && !reading, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.PhotoLibrary, null); Spacer(Modifier.width(8.dp)); Text("Elegir imagen")
            }
            if (reading) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(24.dp)); Text("Leyendo ticket…")
            }
            (state as? ReceiptState.Error)?.let { Text(it.message, color = MaterialTheme.colorScheme.error) }
            Text("ML Kit procesa la imagen localmente y puede enviar métricas técnicas a Google. El ticket no se sube a Firestore.", style = MaterialTheme.typography.bodySmall)
        }
    }

    val review = state as? ReceiptState.Review
    if (review != null) ReceiptReviewDialog(review.draft, viewModel::reset) { corrected ->
        onApply(corrected, review.ownerUid); viewModel.reset()
    }
}

@Composable
fun ReceiptReviewDialog(draft: ReceiptDraft, onDismiss: () -> Unit, onApply: (ReceiptDraft) -> Unit) {
    var merchant by rememberSaveable(draft) { mutableStateOf(draft.merchant.orEmpty()) }
    var date by rememberSaveable(draft) { mutableStateOf(draft.date.orEmpty()) }
    var amount by rememberSaveable(draft) { mutableStateOf(draft.amount?.toPlainString().orEmpty()) }
    var reviewed by rememberSaveable(draft) { mutableStateOf(false) }
    var acceptRounding by rememberSaveable(draft) { mutableStateOf(false) }
    val parsed = DecimalAmounts.parse(amount)
    val needsRounding = parsed != null && parsed.stripTrailingZeros().scale() > 0
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Revisá el ticket") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            draft.warnings.forEach { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            OutlinedTextField(merchant, { merchant = it; reviewed = false }, label = { Text("Comercio o descripción") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(date, { date = it; reviewed = false }, label = { Text("Fecha · dd/mm/aaaa") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(amount, { amount = it; reviewed = false; acceptRounding = false }, label = { Text("Total en pesos argentinos") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            if (needsRounding) {
                Text("La app registra pesos enteros. El total $ ${parsed?.toPlainString()} se redondeará a $ ${parsed?.setScale(0, RoundingMode.HALF_UP)}.")
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(acceptRounding, role = Role.Checkbox, onValueChange = { acceptRounding = it }), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(acceptRounding, null); Text("Acepto este redondeo", Modifier.weight(1f))
                }
            }
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(reviewed, role = Role.Checkbox, onValueChange = { reviewed = it }), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(reviewed, null); Text("Revisé la fecha, el comercio y el importe", Modifier.weight(1f))
            }
            Text("Después deberás elegir la cuenta y la categoría. Todavía no se guardará ningún gasto.", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = {
        TextButton(onClick = {
            onApply(draft.copy(merchant = merchant.trim(), date = date.trim(), amount = parsed?.setScale(0, RoundingMode.HALF_UP)))
        }, enabled = reviewed && (!needsRounding || acceptRounding) && parsed != null && parsed.signum() > 0 && merchant.isNotBlank() && date.isNotBlank()) { Text("Usar en formulario") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Descartar") } })
}
