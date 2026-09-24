package com.maidana.finanzasapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maidana.finanzasapp.data.model.Category
import com.maidana.finanzasapp.data.model.TransactionType
import com.maidana.finanzasapp.ui.screens.categories.CategoryActionState
import com.maidana.finanzasapp.ui.screens.categories.CategoryViewModel

/**
 * Componente unificado para selección y gestión rápida de categorías
 * en formularios de Movimientos y Presupuestos.
 */
@Composable
fun CategorySelectorSection(
    selectedCategoryName: String,
    onCategorySelected: (Category) -> Unit,
    type: TransactionType,
    categoryViewModel: CategoryViewModel,
    modifier: Modifier = Modifier,
    title: String = "Categoría"
) {
    val uiState by categoryViewModel.uiState.collectAsState()

    // Filtrar categorías activas para el tipo actual (GASTO o INGRESO)
    val activeCategories = uiState.categories.filter { it.type == type && it.isActive }
    val predefinedList = activeCategories.filter { it.isPredefined }
    val customList = activeCategories.filter { !it.isPredefined }

    var showCreateDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    var categoryToArchive by remember { mutableStateOf<Category?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Título de la sección
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        // 1. Categorías del Sistema (Predefinidas)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(predefinedList, key = { it.id }) { cat ->
                CategoryChip(
                    category = cat,
                    isSelected = cat.name.equals(selectedCategoryName, ignoreCase = true),
                    onClick = { onCategorySelected(cat) }
                )
            }
        }

        // 2. Mis Categorías (Personalizadas creadas por el usuario)
        if (customList.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Mis Categorías",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(customList, key = { it.id }) { cat ->
                    CustomCategoryChip(
                        category = cat,
                        isSelected = cat.name.equals(selectedCategoryName, ignoreCase = true),
                        onClick = { onCategorySelected(cat) },
                        onEditClick = { categoryToEdit = cat }
                    )
                }
            }
        }

        // 3. Botón para crear nueva categoría
        OutlinedCard(
            onClick = {
                categoryViewModel.resetActionState()
                showCreateDialog = true
            },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Crear nueva categoría",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    // Diálogo de Crear Categoría
    if (showCreateDialog) {
        CreateCategoryDialog(
            type = type,
            actionState = uiState.actionState,
            onDismiss = {
                showCreateDialog = false
                categoryViewModel.resetActionState()
            },
            onCreate = { newName ->
                categoryViewModel.addCategory(
                    name = newName,
                    type = type,
                    onSuccess = { created ->
                        onCategorySelected(created)
                        showCreateDialog = false
                        categoryViewModel.resetActionState()
                    }
                )
            }
        )
    }

    // Diálogo de Editar Nombre de Categoría
    categoryToEdit?.let { cat ->
        EditCategoryDialog(
            category = cat,
            actionState = uiState.actionState,
            onDismiss = {
                categoryToEdit = null
                categoryViewModel.resetActionState()
            },
            onSave = { newName ->
                categoryViewModel.updateCategory(
                    id = cat.id,
                    newName = newName,
                    oldName = cat.name,
                    onSuccess = {
                        categoryToEdit = null
                        categoryViewModel.resetActionState()
                    }
                )
            },
            onArchiveClick = {
                categoryToArchive = cat
                categoryToEdit = null
            }
        )
    }

    // Diálogo de Confirmación para Archivar Categoría
    categoryToArchive?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToArchive = null },
            title = { Text("¿Archivar categoría '${cat.name}'?") },
            text = {
                Text(
                    "Esta categoría ya no estará disponible para nuevos movimientos ni presupuestos, " +
                    "pero se conservará intacta en tus movimientos históricos y estadísticas."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        categoryViewModel.archiveCategory(cat.id) {
                            categoryToArchive = null
                            categoryViewModel.resetActionState()
                        }
                    }
                ) {
                    Text("Archivar")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToArchive = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun CategoryChip(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconInfo = getCategoryIcon(category.name)

    Card(
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.5.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(iconInfo.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconInfo.icon,
                    contentDescription = null,
                    tint = iconInfo.color,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CustomCategoryChip(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconInfo = getCategoryIcon(category.name)

    Card(
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.5.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(iconInfo.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconInfo.icon,
                    contentDescription = null,
                    tint = iconInfo.color,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = onEditClick,
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Opciones de categoría",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CreateCategoryDialog(
    type: TransactionType,
    actionState: CategoryActionState,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    val isLoading = actionState is CategoryActionState.Loading
    val activeError = localError ?: (actionState as? CategoryActionState.Error)?.message

    val typeLabel = if (type == TransactionType.INGRESO) "Ingreso" else "Gasto"

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = "Nueva Categoría de $typeLabel",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Ingresa un nombre para tu categoría personalizada:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        localError = null
                    },
                    label = { Text("Nombre de la categoría") },
                    placeholder = { Text("Ej: Mascotas, Universidad") },
                    singleLine = true,
                    isError = activeError != null,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (activeError != null) {
                    Text(
                        text = activeError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = name.trim()
                    if (clean.isBlank()) {
                        localError = "Por favor ingresa un nombre."
                        return@Button
                    }
                    onCreate(clean)
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Crear")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun EditCategoryDialog(
    category: Category,
    actionState: CategoryActionState,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onArchiveClick: () -> Unit
) {
    var name by remember { mutableStateOf(category.name) }
    var localError by remember { mutableStateOf<String?>(null) }
    val isLoading = actionState is CategoryActionState.Loading
    val activeError = localError ?: (actionState as? CategoryActionState.Error)?.message

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = "Gestionar Categoría",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Edita el nombre o archiva esta categoría personalizada:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        localError = null
                    },
                    label = { Text("Nombre") },
                    singleLine = true,
                    isError = activeError != null,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (activeError != null) {
                    Text(
                        text = activeError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick = onArchiveClick,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Archivar categoría",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = name.trim()
                    if (clean.isBlank()) {
                        localError = "Por favor ingresa un nombre."
                        return@Button
                    }
                    onSave(clean)
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Guardar Cambios")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancelar")
            }
        }
    )
}
