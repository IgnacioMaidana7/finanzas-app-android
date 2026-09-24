package com.maidana.finanzasapp.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maidana.finanzasapp.data.model.Category
import com.maidana.finanzasapp.data.model.CategoryConstants
import com.maidana.finanzasapp.data.model.TransactionType
import com.maidana.finanzasapp.data.repository.CategoryRepository
import com.maidana.finanzasapp.data.repository.FirestoreCategoryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface CategoryActionState {
    data object Idle : CategoryActionState
    data object Loading : CategoryActionState
    data class Success(val message: String, val category: Category? = null) : CategoryActionState
    data class Error(val message: String) : CategoryActionState
}

data class CategoryUiState(
    val categories: List<Category> = CategoryConstants.ALL_PREDEFINED_CATEGORIES,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val actionState: CategoryActionState = CategoryActionState.Idle
) {
    fun activeExpenseCategories(): List<Category> =
        categories.filter { it.type == TransactionType.GASTO && it.isActive }

    fun activeIncomeCategories(): List<Category> =
        categories.filter { it.type == TransactionType.INGRESO && it.isActive }

    fun customCategories(): List<Category> =
        categories.filter { !it.isPredefined }
}

class CategoryViewModel(
    private val repository: CategoryRepository = FirestoreCategoryRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadCategories()
    }

    fun loadCategories() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        loadJob = viewModelScope.launch {
            repository.getCategories()
                .catch { error ->
                    val msg = error.message?.takeIf { it.isNotBlank() }
                        ?: "No se pudieron cargar las categorías personalizadas."
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = msg,
                            categories = CategoryConstants.ALL_PREDEFINED_CATEGORIES
                        )
                    }
                }
                .collect { list ->
                    _uiState.update {
                        it.copy(
                            categories = list,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun addCategory(
        name: String,
        type: TransactionType,
        onSuccess: (Category) -> Unit = {}
    ) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            _uiState.update {
                it.copy(actionState = CategoryActionState.Error("El nombre de la categoría no puede estar vacío."))
            }
            return
        }

        _uiState.update { it.copy(actionState = CategoryActionState.Loading) }

        viewModelScope.launch {
            val result = repository.addCategory(trimmed, type)
            result.fold(
                onSuccess = { createdCategory ->
                    _uiState.update {
                        it.copy(
                            actionState = CategoryActionState.Success(
                                message = "Categoría '${createdCategory.name}' creada exitosamente.",
                                category = createdCategory
                            )
                        )
                    }
                    onSuccess(createdCategory)
                },
                onFailure = { error ->
                    val msg = error.message?.takeIf { it.isNotBlank() }
                        ?: "No se pudo crear la categoría."
                    _uiState.update { it.copy(actionState = CategoryActionState.Error(msg)) }
                }
            )
        }
    }

    fun updateCategory(
        id: String,
        newName: String,
        oldName: String = "",
        onSuccess: () -> Unit = {}
    ) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            _uiState.update {
                it.copy(actionState = CategoryActionState.Error("El nombre de la categoría no puede estar vacío."))
            }
            return
        }

        _uiState.update { it.copy(actionState = CategoryActionState.Loading) }

        viewModelScope.launch {
            val result = repository.updateCategory(id, trimmed, oldName)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(actionState = CategoryActionState.Success("Categoría actualizada a '$trimmed'."))
                    }
                    onSuccess()
                },
                onFailure = { error ->
                    val msg = error.message?.takeIf { it.isNotBlank() }
                        ?: "No se pudo actualizar la categoría."
                    _uiState.update { it.copy(actionState = CategoryActionState.Error(msg)) }
                }
            )
        }
    }

    fun archiveCategory(id: String, onSuccess: () -> Unit = {}) {
        _uiState.update { it.copy(actionState = CategoryActionState.Loading) }

        viewModelScope.launch {
            val result = repository.setCategoryActive(id, isActive = false)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(actionState = CategoryActionState.Success("Categoría archivada correctamente."))
                    }
                    onSuccess()
                },
                onFailure = { error ->
                    val msg = error.message?.takeIf { it.isNotBlank() }
                        ?: "No se pudo archivar la categoría."
                    _uiState.update { it.copy(actionState = CategoryActionState.Error(msg)) }
                }
            )
        }
    }

    fun reactivateCategory(id: String, onSuccess: () -> Unit = {}) {
        _uiState.update { it.copy(actionState = CategoryActionState.Loading) }

        viewModelScope.launch {
            val result = repository.setCategoryActive(id, isActive = true)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(actionState = CategoryActionState.Success("Categoría reactivada correctamente."))
                    }
                    onSuccess()
                },
                onFailure = { error ->
                    val msg = error.message?.takeIf { it.isNotBlank() }
                        ?: "No se pudo reactivar la categoría."
                    _uiState.update { it.copy(actionState = CategoryActionState.Error(msg)) }
                }
            )
        }
    }

    fun resetActionState() {
        _uiState.update { it.copy(actionState = CategoryActionState.Idle) }
    }
}
