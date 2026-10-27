package com.maidana.finanzasapp.ui.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveable
import com.google.firebase.auth.FirebaseAuth
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maidana.finanzasapp.data.model.Account
import com.maidana.finanzasapp.data.model.CategoryConstants
import com.maidana.finanzasapp.data.model.TransactionType
import com.maidana.finanzasapp.ui.components.AccountSelectorSection
import com.maidana.finanzasapp.ui.components.AppTopBar
import com.maidana.finanzasapp.ui.components.CategorySelectorSection
import com.maidana.finanzasapp.ui.components.ThousandsSeparatorVisualTransformation
import com.maidana.finanzasapp.ui.screens.accounts.AccountViewModel
import com.maidana.finanzasapp.ui.screens.categories.CategoryViewModel
import com.maidana.finanzasapp.ui.theme.ExpenseRed
import com.maidana.finanzasapp.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NewTransactionScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionsViewModel = viewModel(),
    categoryViewModel: CategoryViewModel = viewModel(),
    accountViewModel: AccountViewModel = viewModel(),
    receiptViewModel: ReceiptViewModel = viewModel()
) {
    val saveState by viewModel.saveState.collectAsState()
    val accountUiState by accountViewModel.uiState.collectAsState()

    var type by rememberSaveable { mutableStateOf(TransactionType.GASTO) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var receiptOwner by rememberSaveable { mutableStateOf<String?>(null) }
    var accountReviewed by rememberSaveable { mutableStateOf(false) }
    var categoryReviewed by rememberSaveable { mutableStateOf(false) }
    val auth = remember { FirebaseAuth.getInstance() }
    var activeUid by remember { mutableStateOf(auth.currentUser?.uid) }
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { activeUid = it.currentUser?.uid }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }
    LaunchedEffect(activeUid, receiptOwner) {
        if (receiptOwner != null && receiptOwner != activeUid) {
            amountText = ""; title = ""; receiptOwner = null
            accountReviewed = false; categoryReviewed = false
            viewModel.resetSaveState()
        }
    }
    val defaultExpenseCategory = CategoryConstants.PREDEFINED_EXPENSE_CATEGORIES.firstOrNull()?.name ?: "Alimentación"
    val defaultExpenseCategoryId = CategoryConstants.PREDEFINED_EXPENSE_CATEGORIES.firstOrNull()?.id.orEmpty()
    val defaultIncomeCategory = CategoryConstants.PREDEFINED_INCOME_CATEGORIES.firstOrNull()?.name ?: "Salario"
    val defaultIncomeCategoryId = CategoryConstants.PREDEFINED_INCOME_CATEGORIES.firstOrNull()?.id.orEmpty()

    var selectedCategory by rememberSaveable { mutableStateOf(defaultExpenseCategory) }
    var selectedCategoryId by rememberSaveable { mutableStateOf(defaultExpenseCategoryId) }

    var selectedAccountId by rememberSaveable { mutableStateOf(Account.DEFAULT_CASH_ID) }
    var selectedAccountName by rememberSaveable { mutableStateOf(Account.DEFAULT_CASH_ACCOUNT.name) }

    var dateString by rememberSaveable {
        mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()))
    }

    LaunchedEffect(accountUiState.accounts) {
        if (selectedAccountId.isBlank() && accountUiState.accounts.isNotEmpty()) {
            val defaultAcc = accountUiState.activeAccounts().firstOrNull() ?: Account.DEFAULT_CASH_ACCOUNT
            selectedAccountId = defaultAcc.id
            selectedAccountName = defaultAcc.name
        }
    }

    LaunchedEffect(saveState) {
        if (saveState is SaveTransactionState.Success) {
            viewModel.resetSaveState()
            onNavigateBack()
        }
    }

    val categories = if (type == TransactionType.GASTO) {
        CategoryConstants.EXPENSE_CATEGORIES
    } else {
        CategoryConstants.INCOME_CATEGORIES
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Nuevo Movimiento",
                onBackClick = {
                    viewModel.resetSaveState()
                    onNavigateBack()
                }
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
            if (type == TransactionType.GASTO) {
                ReceiptScannerSection(receiptViewModel, saveState !is SaveTransactionState.Loading) { draft, owner ->
                    amountText = draft.amount?.longValueExact()?.toString().orEmpty()
                    title = draft.merchant.orEmpty()
                    dateString = draft.date.orEmpty()
                    receiptOwner = owner
                    accountReviewed = false; categoryReviewed = false
                    selectedAccountId = ""; selectedAccountName = ""
                    selectedCategory = ""; selectedCategoryId = ""
                    viewModel.resetSaveState()
                }
            }
            if (receiptOwner != null && (!accountReviewed || !categoryReviewed)) {
                Text("Ticket revisado. Elegí la cuenta de origen y la categoría antes de guardar.", color = MaterialTheme.colorScheme.primary)
            }
            if (saveState is SaveTransactionState.Error) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = (saveState as SaveTransactionState.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            // Selector Ingreso / Gasto
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    val isGasto = type == TransactionType.GASTO
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isGasto) ExpenseRed else Color.Transparent)
                            .clickable {
                                type = TransactionType.GASTO
                                selectedCategory = defaultExpenseCategory
                                selectedCategoryId = defaultExpenseCategoryId
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Gasto (-)",
                            fontWeight = FontWeight.Bold,
                            color = if (isGasto) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val isIngreso = type == TransactionType.INGRESO
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isIngreso) IncomeGreen else Color.Transparent)
                            .clickable {
                                type = TransactionType.INGRESO
                                selectedCategory = defaultIncomeCategory
                                selectedCategoryId = defaultIncomeCategoryId
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ingreso (+)",
                            fontWeight = FontWeight.Bold,
                            color = if (isIngreso) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Input de Monto destacado
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Monto",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { input ->
                            val digitsOnly = input.filter { it.isDigit() }
                            if (digitsOnly.length <= 15) {
                                amountText = if (digitsOnly.length > 1) digitsOnly.trimStart('0').ifEmpty { "0" } else digitsOnly
                            }
                        },
                        placeholder = { Text("0", style = MaterialTheme.typography.headlineMedium) },
                        prefix = {
                            Text(
                                text = "$ ",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (type == TransactionType.INGRESO) IncomeGreen else ExpenseRed
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

            // Selector de Cuenta / Origen del dinero
            AccountSelectorSection(
                accounts = accountUiState.accounts,
                selectedAccountId = selectedAccountId,
                onAccountSelected = { acc ->
                    selectedAccountId = acc.id
                    selectedAccountName = acc.name
                    accountReviewed = true
                }
            )

            // Selector de Categoría (Centralizado con soporte de personalizadas)
            CategorySelectorSection(
                selectedCategoryName = selectedCategory,
                onCategorySelected = { cat ->
                    selectedCategory = cat.name
                    selectedCategoryId = cat.id
                    categoryReviewed = true
                },
                type = type,
                categoryViewModel = categoryViewModel
            )

            // Descripción
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Descripción o Nota") },
                placeholder = { Text("Ej: Supermercado Coto, Alquiler") },
                leadingIcon = {
                    Icon(Icons.Default.Description, contentDescription = null)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Fecha
            OutlinedTextField(
                value = dateString,
                onValueChange = { dateString = it },
                label = { Text("Fecha") },
                leadingIcon = {
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            val isLoading = saveState is SaveTransactionState.Loading

            // Botón Guardar
            Button(
                onClick = {
                    viewModel.saveTransaction(
                        title = title,
                        amountText = amountText,
                        type = type,
                        category = selectedCategory,
                        dateString = dateString,
                        categoryId = selectedCategoryId,
                        accountId = selectedAccountId,
                        accountName = selectedAccountName,
                        expectedOwnerUid = receiptOwner
                    )
                },
                enabled = !isLoading && (receiptOwner == null || (accountReviewed && categoryReviewed)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == TransactionType.INGRESO) IncomeGreen else MaterialTheme.colorScheme.primary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Guardando movimiento...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar Movimiento", fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = {
                    viewModel.resetSaveState()
                    onNavigateBack()
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancelar")
            }
        }
    }
}
