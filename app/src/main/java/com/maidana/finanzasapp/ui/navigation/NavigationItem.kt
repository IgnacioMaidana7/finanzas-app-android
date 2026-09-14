package com.maidana.finanzasapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavigationItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Dashboard : NavigationItem(
        route = Screen.Dashboard.route,
        title = "Dashboard",
        icon = Icons.Default.Dashboard
    )

    data object Transactions : NavigationItem(
        route = Screen.Transactions.route,
        title = "Movimientos",
        icon = Icons.AutoMirrored.Filled.ReceiptLong
    )

    data object Budgets : NavigationItem(
        route = Screen.Budgets.route,
        title = "Presupuestos",
        icon = Icons.Default.AccountBalanceWallet
    )

    data object Goals : NavigationItem(
        route = Screen.Goals.route,
        title = "Objetivos",
        icon = Icons.Default.TrackChanges
    )

    data object Statistics : NavigationItem(
        route = Screen.Statistics.route,
        title = "Estadísticas",
        icon = Icons.Default.BarChart
    )

    companion object {
        val bottomNavItems = listOf(
            Dashboard,
            Transactions,
            Budgets,
            Goals,
            Statistics
        )
    }
}
