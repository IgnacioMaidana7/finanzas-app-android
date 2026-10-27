package com.maidana.finanzasapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
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


import com.maidana.finanzasapp.ui.screens.accounts.AccountViewModel
import com.maidana.finanzasapp.ui.screens.auth.AuthViewModel
import com.maidana.finanzasapp.ui.screens.categories.CategoryViewModel
import com.maidana.finanzasapp.ui.screens.indicators.IndicatorReference
import com.maidana.finanzasapp.ui.screens.indicators.IndicatorsScreen
import com.maidana.finanzasapp.ui.screens.indicators.IndicatorsViewModel
import java.time.YearMonth

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = viewModel()
) {
    val transactionsViewModel: TransactionsViewModel = viewModel()
    val budgetsViewModel: BudgetsViewModel = viewModel()
    val goalsViewModel: GoalsViewModel = viewModel()
    val categoryViewModel: CategoryViewModel = viewModel()
    val accountViewModel: AccountViewModel = viewModel()

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
                accountViewModel = accountViewModel,
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
                },
                onNavigateToIndicators = {
                    navController.navigate(Screen.Indicators.route)
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
            StatisticsScreen(
                onNavigateToNewTransaction = {
                    navController.navigate(Screen.NewTransaction.route)
                }
            )
        }

        // --- Pantallas Secundarias / Formularios ---
        composable(route = Screen.Indicators.route) {
            val context = LocalContext.current
            val indicatorsViewModel: IndicatorsViewModel = viewModel(factory = IndicatorsViewModel.factory(context))
            // These private references belong to this destination and its current session.
            // Do not reuse financial lists created before login at the root navigation level.
            val indicatorBudgets: BudgetsViewModel = viewModel()
            val indicatorGoals: GoalsViewModel = viewModel()
            val budgets by indicatorBudgets.uiState.collectAsState()
            val goals by indicatorGoals.uiState.collectAsState()
            val references = budgets.budgets.map { budget ->
                IndicatorReference("Presupuesto · ${budget.category}", budget.limitAmount,
                    runCatching { YearMonth.of(budget.year, budget.month) }.getOrNull())
            } + goals.goals.map { goal -> IndicatorReference("Meta · ${goal.title}", goal.targetAmount) }
            IndicatorsScreen(indicatorsViewModel, references) { navController.popBackStack() }
        }

        composable(route = Screen.NewTransaction.route) {
            NewTransactionScreen(
                viewModel = transactionsViewModel,
                categoryViewModel = categoryViewModel,
                accountViewModel = accountViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.NewBudget.route) {
            NewBudgetScreen(
                viewModel = budgetsViewModel,
                categoryViewModel = categoryViewModel,
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
