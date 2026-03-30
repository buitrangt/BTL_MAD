package com.arijit.budgettracker.api

import retrofit2.Response
import retrofit2.http.*

// DTOs
data class ExpenseRequest(val amount: Double, val category: String, val timeStamp: Long)
data class ExpenseResponse(val id: Long, val amount: Double, val category: String, val timeStamp: Long)
data class StatsResponse(val totalAmount: Double, val categoryBreakdown: Map<String, Double>?)

interface ApiService {
    // Auth
    @POST("api/auth/register")
    suspend fun register(@Body request: AuthRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    // Expenses
    @GET("api/expenses")
    suspend fun getAllExpenses(): Response<List<ExpenseResponse>>

    @POST("api/expenses")
    suspend fun createExpense(@Body request: ExpenseRequest): Response<ExpenseResponse>

    @DELETE("api/expenses/{id}")
    suspend fun deleteExpense(@Path("id") id: Long): Response<Void>

    @POST("api/expenses/sync")
    suspend fun syncExpenses(@Body requests: List<ExpenseRequest>): Response<List<ExpenseResponse>>

    // Stats
    @GET("api/stats/daily")
    suspend fun getDailyStats(): Response<StatsResponse>

    @GET("api/stats/weekly")
    suspend fun getWeeklyStats(): Response<StatsResponse>

    @GET("api/stats/monthly")
    suspend fun getMonthlyStats(): Response<StatsResponse>

    @GET("api/stats/by-category")
    suspend fun getByCategoryStats(): Response<StatsResponse>
}
