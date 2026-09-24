package com.maidana.finanzasapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maidana.finanzasapp.data.model.Transaction
import com.maidana.finanzasapp.data.model.TransactionType
import com.maidana.finanzasapp.ui.theme.ExpenseRed
import com.maidana.finanzasapp.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionRow(
    transaction: Transaction,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null
) {
    val isIncome = transaction.type == TransactionType.INGRESO
    val iconInfo = getCategoryIcon(transaction.category)
    val formattedDate = SimpleDateFormat("dd MMM, yyyy", Locale.forLanguageTag("es-AR"))
        .format(Date(transaction.date))

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        onClick = { onClick?.invoke() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de categoría
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconInfo.color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconInfo.icon,
                    contentDescription = transaction.category,
                    tint = iconInfo.color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Información de movimiento
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val subtitleText = if (transaction.accountName.isNotBlank()) {
                    "${transaction.category} • ${transaction.accountName} • $formattedDate"
                } else {
                    "${transaction.category} • $formattedDate"
                }
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Monto
            Text(
                text = CurrencyUtils.formatWithSign(transaction.amount, isIncome),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isIncome) IncomeGreen else ExpenseRed
            )

            if (onDeleteClick != null) {
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Eliminar movimiento",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

data class CategoryIconInfo(val icon: ImageVector, val color: Color)

fun getCategoryIcon(category: String): CategoryIconInfo {
    val clean = category.lowercase().trim()
    return when {
        clean == "salario" -> CategoryIconInfo(Icons.Default.Payments, Color(0xFF10B981))
        clean.contains("alimen") || clean.contains("super") || clean.contains("comida") -> CategoryIconInfo(Icons.Default.Restaurant, Color(0xFFF59E0B))
        clean.contains("vivien") || clean.contains("alquiler") || clean.contains("casa") -> CategoryIconInfo(Icons.Default.Home, Color(0xFF3B82F6))
        clean.contains("ocio") || clean.contains("cafe") || clean.contains("café") || clean.contains("salida") -> CategoryIconInfo(Icons.Default.LocalCafe, Color(0xFFEC4899))
        clean.contains("transpor") || clean.contains("combust") || clean.contains("auto") -> CategoryIconInfo(Icons.Default.DirectionsCar, Color(0xFF8B5CF6))
        clean.contains("servici") || clean.contains("internet") || clean.contains("luz") -> CategoryIconInfo(Icons.Default.Bolt, Color(0xFF14B8A6))
        clean.contains("salud") || clean.contains("farmacia") || clean.contains("medic") -> CategoryIconInfo(Icons.Default.MedicalServices, Color(0xFFEF4444))
        clean.contains("educa") || clean.contains("univer") || clean.contains("curso") -> CategoryIconInfo(Icons.Default.School, Color(0xFF6366F1))
        clean.contains("tecno") || clean.contains("compu") || clean.contains("celular") -> CategoryIconInfo(Icons.Default.Devices, Color(0xFF0EA5E9))
        clean.contains("inver") || clean.contains("extra") -> CategoryIconInfo(Icons.AutoMirrored.Filled.TrendingUp, Color(0xFF059669))
        else -> CategoryIconInfo(Icons.Default.Receipt, Color(0xFF64748B))
    }
}
