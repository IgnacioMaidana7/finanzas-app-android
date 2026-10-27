package com.maidana.finanzasapp.ui.navigation

sealed class Screen(val route: String) {
    // Autenticación
    data object Login : Screen("login")
    data object Register : Screen("register")

    // Pantallas principales (disponibles en la barra de navegación)
    data object Dashboard : Screen("dashboard")
    data object Transactions : Screen("transactions")
    data object Budgets : Screen("budgets")
    data object Goals : Screen("goals")
    data object Statistics : Screen("statistics")

    // Pantallas secundarias / formularios
    data object NewTransaction : Screen("new_transaction")
    data object Indicators : Screen("indicators")
    data object NewBudget : Screen("new_budget")
    data object NewGoal : Screen("new_goal")
    data object GoalDetail : Screen("goal_detail/{goalId}") {
        const val ARG_GOAL_ID = "goalId"
        fun createRoute(goalId: String): String = "goal_detail/$goalId"
    }
}
