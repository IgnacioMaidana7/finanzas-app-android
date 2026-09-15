package com.maidana.finanzasapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maidana.finanzasapp.ui.screens.auth.LoginScreen
import com.maidana.finanzasapp.ui.screens.auth.RegisterScreen
import com.maidana.finanzasapp.ui.screens.budgets.BudgetsScreen
import com.maidana.finanzasapp.ui.screens.budgets.BudgetsViewModel
import com.maidana.finanzasapp.ui.screens.budgets.NewBudgetScreen
import com.maidana.finanzasapp.ui.screens.dashboard.DashboardScreen
import com.maidana.finanzasapp.ui.screens.goals.GoalDetailScreen
import com.maidana.finanzasapp.ui.screens.goals.GoalsScreen
import com.maidana.finanzasapp.ui.screens.goals.GoalsViewModel
import com.maidana.finanzasapp.ui.screens.goals.NewGoalScreen
import com.maidana.finanzasapp.ui.screens.statistics.StatisticsScreen
import com.maidana.finanzasapp.ui.screens.transactions.NewTransactionScreen
import com.maidana.finanzasapp.ui.screens.transactions.TransactionsScreen
import com.maidana.finanzasapp.ui.screens.transactions.TransactionsViewModel


import com.maidana.finanzasapp.ui.screens.auth.AuthViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = viewModel()
) {
    val transactionsViewModel: TransactionsViewModel = viewModel()
    val budgetsViewModel: BudgetsViewModel = viewModel()
    val goalsViewModel: GoalsViewModel = viewModel()

    val startDestination = if (authViewModel.isUserAuthenticated()) {
        Screen.Dashboard.route
    } else {
        Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // --- Módulo Autenticación ---
        composable(route = Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(route = Screen.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // --- Pantallas Principales ---
        composable(route = Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToNewTransaction = {
                    navController.navigate(Screen.NewTransaction.route)
                },
                onNavigateToTransactions = {
                    navController.navigate(Screen.Transactions.route)
                },
                onNavigateToGoals = {
                    navController.navigate(Screen.Goals.route)
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.Transactions.route) {
            TransactionsScreen(
                viewModel = transactionsViewModel,
                onNavigateToNewTransaction = {
                    navController.navigate(Screen.NewTransaction.route)
                }
            )
        }

        composable(route = Screen.Budgets.route) {
            BudgetsScreen(
                viewModel = budgetsViewModel,
                onNavigateToNewBudget = {
                    navController.navigate(Screen.NewBudget.route)
                }
            )
        }

        composable(route = Screen.Goals.route) {
            GoalsScreen(
                viewModel = goalsViewModel,
                onNavigateToNewGoal = {
                    navController.navigate(Screen.NewGoal.route)
                },
                onNavigateToGoalDetail = { goalId ->
                    navController.navigate(Screen.GoalDetail.createRoute(goalId))
                }
            )
        }

        composable(route = Screen.Statistics.route) {
            StatisticsScreen()
        }

        // --- Pantallas Secundarias / Formularios ---
        composable(route = Screen.NewTransaction.route) {
            NewTransactionScreen(
                viewModel = transactionsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.NewBudget.route) {
            NewBudgetScreen(
                viewModel = budgetsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.NewGoal.route) {
            NewGoalScreen(
                viewModel = goalsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.GoalDetail.route,
            arguments = listOf(
                navArgument(Screen.GoalDetail.ARG_GOAL_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val goalId = backStackEntry.arguments?.getString(Screen.GoalDetail.ARG_GOAL_ID).orEmpty()
            GoalDetailScreen(
                goalId = goalId,
                viewModel = goalsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
