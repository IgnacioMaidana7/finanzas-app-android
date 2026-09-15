package com.maidana.finanzasapp.data.mock

import androidx.compose.ui.graphics.Color
import com.maidana.finanzasapp.data.model.Budget
import com.maidana.finanzasapp.data.model.GoalContribution
import com.maidana.finanzasapp.data.model.SavingsGoal
import com.maidana.finanzasapp.data.model.Transaction
import com.maidana.finanzasapp.data.model.TransactionType
import com.maidana.finanzasapp.data.model.User

data class CategoryExpenseStat(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val color: Color
)

data class MonthlyEvolutionPoint(
    val month: String,
    val balance: Double
)

data class PeriodFinancialSummary(
    val income: Double,
    val expense: Double,
    val balance: Double
)

/**
 * Proveedor centralizado de datos simulados (Mock).
 * Toda la UI consume estos datos mediante los ViewModels.
 * Cuando se implementen Firebase o repositorios reales, solo se reemplaza esta fuente.
 */
object MockDataProvider {

    val user: User = User(
        id = "usr_001",
        email = "ignacio@finanzasapp.com",
        displayName = "Ignacio Maidana"
    )

    val transactions: List<Transaction> = listOf(
        Transaction(
            id = "tx_01",
            title = "Sueldo Septiembre",
            amount = 950000L,
            type = TransactionType.INGRESO,
            category = "Salario",
            date = 1726000000000L
        ),
        Transaction(
            id = "tx_02",
            title = "Supermercado Coto",
            amount = 78500L,
            type = TransactionType.GASTO,
            category = "Alimentación",
            date = 1726100000000L
        ),
        Transaction(
            id = "tx_03",
            title = "Alquiler Departamento",
            amount = 220000L,
            type = TransactionType.GASTO,
            category = "Vivienda",
            date = 1726150000000L
        ),
        Transaction(
            id = "tx_04",
            title = "Rendimientos CEDEARs",
            amount = 65000L,
            type = TransactionType.INGRESO,
            category = "Inversión",
            date = 1726200000000L
        ),
        Transaction(
            id = "tx_05",
            title = "Cena La Cabrera",
            amount = 28400L,
            type = TransactionType.GASTO,
            category = "Ocio",
            date = 1726250000000L
        ),
        Transaction(
            id = "tx_06",
            title = "Combustible YPF",
            amount = 32000L,
            type = TransactionType.GASTO,
            category = "Transporte",
            date = 1726300000000L
        ),
        Transaction(
            id = "tx_07",
            title = "Internet Fibertel & Celular",
            amount = 18500L,
            type = TransactionType.GASTO,
            category = "Servicios",
            date = 1726350000000L
        ),
        Transaction(
            id = "tx_08",
            title = "Proyecto Freelance UI",
            amount = 140000L,
            type = TransactionType.INGRESO,
            category = "Ingresos Extra",
            date = 1726400000000L
        ),
        Transaction(
            id = "tx_09",
            title = "Membresía Gimnasio",
            amount = 16000L,
            type = TransactionType.GASTO,
            category = "Salud",
            date = 1726450000000L
        ),
        Transaction(
            id = "tx_10",
            title = "Farmacia Farmacity",
            amount = 9400L,
            type = TransactionType.GASTO,
            category = "Salud",
            date = 1726500000000L
        ),
        Transaction(
            id = "tx_11",
            title = "Café de Especialidad",
            amount = 4800L,
            type = TransactionType.GASTO,
            category = "Ocio",
            date = 1726550000000L
        )
    )

    val budgets: List<Budget> = listOf(
        Budget(
            id = "bg_01",
            category = "Alimentación",
            limitAmount = 150000L,
            spentAmount = 98500L,
            month = 9,
            year = 2026,
            createdAt = 1726000000000L
        ),
        Budget(
            id = "bg_02",
            category = "Vivienda y Servicios",
            limitAmount = 250000L,
            spentAmount = 238500L, // 95.4% -> advertencia
            month = 9,
            year = 2026,
            createdAt = 1726000000000L
        ),
        Budget(
            id = "bg_03",
            category = "Salidas y Ocio",
            limitAmount = 50000L,
            spentAmount = 58200L, // 116.4% -> excedido
            month = 9,
            year = 2026,
            createdAt = 1726000000000L
        ),
        Budget(
            id = "bg_04",
            category = "Transporte",
            limitAmount = 60000L,
            spentAmount = 32000L, // 53.3% -> normal
            month = 9,
            year = 2026,
            createdAt = 1726000000000L
        ),
        Budget(
            id = "bg_05",
            category = "Salud y Cuidado",
            limitAmount = 40000L,
            spentAmount = 25400L, // 63.5% -> normal
            month = 9,
            year = 2026,
            createdAt = 1726000000000L
        )
    )

    val goals: List<SavingsGoal> = listOf(
        SavingsGoal(
            id = "meta-emergencia",
            title = "Fondo de Emergencia",
            targetAmount = 1000000.0,
            currentAmount = 650000.0,
            targetDateString = "Diciembre 2026",
            contributions = listOf(
                GoalContribution("c1", "meta-emergencia", 250000.0, "01/07/2026", "Aporte inicial"),
                GoalContribution("c2", "meta-emergencia", 200000.0, "05/08/2026", "Sobrante julio"),
                GoalContribution("c3", "meta-emergencia", 200000.0, "04/09/2026", "Aporte mensual")
            )
        ),
        SavingsGoal(
            id = "meta-vacaciones",
            title = "Vacaciones en Brasil",
            targetAmount = 800000.0,
            currentAmount = 480000.0,
            targetDateString = "Febrero 2027",
            contributions = listOf(
                GoalContribution("c4", "meta-vacaciones", 180000.0, "15/07/2026", "Reserva de vuelo"),
                GoalContribution("c5", "meta-vacaciones", 150000.0, "12/08/2026", "Aporte aguinaldo"),
                GoalContribution("c6", "meta-vacaciones", 150000.0, "10/09/2026", "Ahorro septiembre")
            )
        ),
        SavingsGoal(
            id = "meta-auto",
            title = "Cambio de Vehículo",
            targetAmount = 3500000.0,
            currentAmount = 1400000.0,
            targetDateString = "Noviembre 2027",
            contributions = listOf(
                GoalContribution("c7", "meta-auto", 800000.0, "10/06/2026", "Entrega base"),
                GoalContribution("c8", "meta-auto", 300000.0, "15/07/2026", "Aporte extraordinario"),
                GoalContribution("c9", "meta-auto", 300000.0, "20/08/2026", "Aporte mensual")
            )
        )
    )

    // Desglose de gastos por categoría para la pantalla de Estadísticas
    val categoryExpenses: List<CategoryExpenseStat> = listOf(
        CategoryExpenseStat("Vivienda", 238500.0, 0.44f, Color(0xFF3B82F6)),
        CategoryExpenseStat("Alimentación", 98500.0, 0.22f, Color(0xFF10B981)),
        CategoryExpenseStat("Ocio", 58200.0, 0.13f, Color(0xFFF59E0B)),
        CategoryExpenseStat("Transporte", 32000.0, 0.08f, Color(0xFF8B5CF6)),
        CategoryExpenseStat("Salud", 25400.0, 0.07f, Color(0xFFEC4899)),
        CategoryExpenseStat("Servicios", 18500.0, 0.06f, Color(0xFF14B8A6))
    )

    // Evolución de saldo mensual
    val balanceEvolution: List<MonthlyEvolutionPoint> = listOf(
        MonthlyEvolutionPoint("Abr", 720000.0),
        MonthlyEvolutionPoint("May", 890000.0),
        MonthlyEvolutionPoint("Jun", 1050000.0),
        MonthlyEvolutionPoint("Jul", 1210000.0),
        MonthlyEvolutionPoint("Ago", 1360000.0),
        MonthlyEvolutionPoint("Sep", 1540000.0)
    )

    fun getSummaryForPeriod(period: String): PeriodFinancialSummary {
        return when (period) {
            "1M" -> PeriodFinancialSummary(income = 1155000.0, expense = 471100.0, balance = 683900.0)
            "3M" -> PeriodFinancialSummary(income = 3280000.0, expense = 1450000.0, balance = 1830000.0)
            "6M" -> PeriodFinancialSummary(income = 6120000.0, expense = 2890000.0, balance = 3230000.0)
            else -> PeriodFinancialSummary(income = 12400000.0, expense = 5900000.0, balance = 6500000.0)
        }
    }
}
