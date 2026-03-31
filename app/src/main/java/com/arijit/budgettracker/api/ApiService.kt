package com.arijit.budgettracker.api

import com.arijit.budgettracker.models.User
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

// DTOs
data class ExpenseRequest(val amount: Double, val category: String, val timeStamp: Long)
data class ExpenseResponse(val id: Long, val amount: Double, val category: String, val timeStamp: Long)
data class StatsResponse(val totalAmount: Double, val categoryBreakdown: Map<String, Double>?)
data class ResetPasswordRequest(val email: String, val newPassword: String)

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

    @GET("api/users/profile")
    suspend fun getUserProfile(): Response<User>

    @PUT("api/users/profile")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): Response<Void>

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Query("email") email: String): Response<ResponseBody>

    @POST("api/auth/verify-otp")
    suspend fun verifyOtp(@Query("email") email: String, @Query("otp") otp: String): Response<ResponseBody>

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<ResponseBody>

    @POST("api/finchat/ask")
    suspend fun askFinChat(
        @Query("message") message: String,
        @Query("userId") userId: Long
    ): Response<String>
}
