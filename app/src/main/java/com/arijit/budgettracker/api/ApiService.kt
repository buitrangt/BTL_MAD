package com.arijit.budgettracker.api

import com.arijit.budgettracker.models.User
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

// DTOs
data class TransactionRequest(
    val name: String,
    val amount: Double,
    val categoryName: String,
    val timeStamp: Long,
    val note: String? = null,
    val type: String? = null
)
data class TransactionResponse(
    val id: Long,
    val name: String,
    val amount: Double,
    val categoryId: Long?,
    @com.google.gson.annotations.SerializedName("categoryName") val category: String?,
    val type: String,
    val note: String?,
    val timeStamp: Long
)
data class StatsResponse(val totalAmount: Double, val categoryBreakdown: Map<String, Double>?)
data class WeeklyOverviewResponse(
    val totalAmount: Double,
    val percentChange: Double,
    val dailyBreakdown: Map<String, Double>,
    val categoryBreakdown: List<CategoryStat>
)
data class HomeOverviewResponse(
    val todayAmount: Double,
    val weekAmount: Double,
    val monthAmount: Double,
    val monthIncome: Double,
    val monthExpense: Double,
    val monthSavings: Double,
    val recentTransactions: List<TransactionResponse>
)
data class CategoryStat(
    val category: String,
    val amount: Double,
    val percent: Double
)
// Yêu cầu đặt lại mật khẩu mới cho chức năng Quên mật khẩu
data class ResetPasswordRequest(val email: String, val newPassword: String)
// Yêu cầu đổi mật khẩu dành cho người dùng đã đăng nhập thành công
data class ChangePasswordRequest(val oldPassword: String, val newPassword: String)
data class CategoryRequest(val name: String, val note: String?)
data class CategoryResponse(val id: Long, val name: String, val note: String?, val isDefault: Boolean)
data class SmsTemplateDto(val id: Long, val senderPattern: String, val amountRegex: String, val type: String, val bankName: String, val version: Int)

// ======== INSIGHTS DTOs ========
data class PredictionDto(
    val month: Int?,
    val year: Int?,
    val currentAmount: Double,
    val predictedAmount: Double,
    val status: String?
)
data class ClassificationDto(
    val month: Int?,
    val year: Int?,
    val level: String,
    val label: String,
    val note: String?
)
data class AnomalyDto(
    val categoryId: Long,
    val categoryName: String,
    val amountDiff: Double,
    val percentIncrease: Double
)
data class BudgetSuggestionDto(
    val categoryId: Long,
    val categoryName: String,
    val suggestedAmount: Double
)
data class InsightsSummaryDto(
    val prediction: PredictionDto?,
    val classification: ClassificationDto?,
    val anomalies: List<AnomalyDto>,
    val budgetSuggestions: List<BudgetSuggestionDto>,
    val aiNarrative: String? = null
)

// ======== CHAT DTOs (CÁC ĐỐI TƯỢNG TRUYỀN TẢI DỮ LIỆU CHATBOT AI) ========
// Yêu cầu gửi tin nhắn đến FinChat (Gemini AI)
data class ChatRequest(val message: String, val sessionId: String? = null)
// Phản hồi từ FinChat chứa câu trả lời và sessionId phiên hội thoại
data class ChatReply(val sessionId: String, val reply: String)
// Chi tiết tin nhắn trò chuyện dùng để đồng bộ lịch sử chat
data class ChatMessageDto(
    val id: Long,
    val sessionId: String,
    val role: String,
    val content: String,
    val createdAt: String?
)

// ======== ADMIN DTOs ========
data class AdminUserDto(
    val id: Long,
    val name: String,
    val email: String,
    val phone: String?,
    val role: String?,
    val locked: Boolean?,
    val createdAt: String?
)
data class AdminOverviewResponse(
    val totalUsers: Long,
    val percentChangeVsLastMonth: Double,
    val newUsersToday: Long,
    val newUsersChangePercent: Double,
    val activeUsersToday: Long,
    val activeUsersChangePercent: Double,
    val weeklyRegistrations: Map<String, Long>,
    val recentUsers: List<AdminUserDto>
)
data class AdminUsersPageResponse(
    val items: List<AdminUserDto>,
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val totalPages: Int
)
data class AdminLockRequest(val locked: Boolean)
data class AdminCategoryDto(
    val id: Long,
    val name: String,
    val note: String?,
    val isDefault: Boolean?,
    val createdAt: String?
)
data class AdminCategoryCreateRequest(val name: String, val note: String?)

interface ApiService {
    // ======== CÁC API XÁC THỰC NGƯỜI DÙNG (AUTHENTICATION) ========
    // API Đăng ký tài khoản mới
    @POST("api/auth/register")
    suspend fun register(@Body request: AuthRequest): Response<AuthResponse>

    // API Đăng nhập hệ thống nhận JWT Token
    @POST("api/auth/login")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    // Transactions (includes type field)
    @GET("api/transactions")
    suspend fun getAllTransactions(): Response<List<TransactionResponse>>

    @GET("api/transactions/search")
    suspend fun searchTransactions(@Query("keyword") keyword: String): Response<List<TransactionResponse>>

    @POST("api/transactions")
    suspend fun createTransaction(@Body request: TransactionRequest): Response<TransactionResponse>

    @PUT("api/transactions/{id}")
    suspend fun updateTransaction(@Path("id") id: Long, @Body request: TransactionRequest): Response<TransactionResponse>

    @DELETE("api/transactions/{id}")
    suspend fun deleteTransaction(@Path("id") id: Long): Response<Void>

    @POST("api/transactions/sync")
    suspend fun syncTransactions(@Body requests: List<TransactionRequest>): Response<List<TransactionResponse>>

    // Categories
    @GET("api/categories")
    suspend fun getAllCategories(): Response<List<CategoryResponse>>

    @POST("api/categories")
    suspend fun createCategory(@Body request: CategoryRequest): Response<CategoryResponse>

    @PUT("api/categories/{id}")
    suspend fun updateCategory(@Path("id") id: Long, @Body request: CategoryRequest): Response<CategoryResponse>

    @DELETE("api/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Long): Response<Void>

    // Stats
    @GET("api/stats/home-overview")
    suspend fun getHomeOverview(): Response<HomeOverviewResponse>

    @GET("api/stats/weekly-overview")
    suspend fun getWeeklyOverview(): Response<WeeklyOverviewResponse>

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

    // API Quên mật khẩu: Yêu cầu gửi mã OTP xác nhận qua email
    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Query("email") email: String): Response<ResponseBody>

    // API Xác thực mã OTP người dùng nhập vào
    @POST("api/auth/verify-otp")
    suspend fun verifyOtp(@Query("email") email: String, @Query("otp") otp: String): Response<ResponseBody>

    // API Đặt lại mật khẩu mới sau khi xác thực OTP thành công
    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<ResponseBody>

    // API Thay đổi mật khẩu khi người dùng đã đăng nhập
    @POST("api/auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<ResponseBody>

    // SMS Templates
    @GET("api/sms/templates")
    suspend fun getSmsTemplates(): Response<List<SmsTemplateDto>>

    // ===== ADMIN =====
    @GET("api/admin/overview")
    suspend fun getAdminOverview(): Response<AdminOverviewResponse>

    @GET("api/admin/users")
    suspend fun getAdminUsers(
        @Query("search") search: String = "",
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<AdminUsersPageResponse>

    @PATCH("api/admin/users/{id}/lock")
    suspend fun setAdminUserLock(
        @Path("id") id: Long,
        @Body body: AdminLockRequest
    ): Response<AdminUserDto>

    @GET("api/admin/categories")
    suspend fun getAdminCategories(): Response<List<AdminCategoryDto>>

    @POST("api/admin/categories")
    suspend fun createAdminCategory(
        @Body body: AdminCategoryCreateRequest
    ): Response<AdminCategoryDto>

    @DELETE("api/admin/categories/{id}")
    suspend fun deleteAdminCategory(@Path("id") id: Long): Response<Void>

    // ===== INSIGHTS =====
    @GET("api/insights/summary")
    suspend fun getInsightsSummary(): Response<InsightsSummaryDto>

    @POST("api/insights/refresh")
    suspend fun refreshInsights(): Response<InsightsSummaryDto>

    // ===== CÁC API GIAO TIẾP VỚI TRỢ LÝ TÀI CHÍNH ẢO FINCHAT AI =====
    // API gửi tin nhắn hỏi FinChat và nhận phản hồi sinh ra bởi Gemini AI
    @POST("api/finchat/message")
    suspend fun sendChatMessage(@Body request: ChatRequest): Response<ChatReply>

    // API truy vấn toàn bộ lịch sử trò chuyện của một phiên chat cụ thể
    @GET("api/finchat/history/{sessionId}")
    suspend fun getChatHistory(@Path("sessionId") sessionId: String): Response<List<ChatMessageDto>>

    // API lấy danh sách các tin nhắn hội thoại gần đây của người dùng
    @GET("api/finchat/recent")
    suspend fun getRecentChatMessages(@Query("limit") limit: Int = 20): Response<List<ChatMessageDto>>

    // API FinChat cũ (Legacy) phục vụ tương thích ngược
    @POST("api/finchat/ask")
    suspend fun askFinChat(
        @Query("message") message: String,
        @Query("userId") userId: Long
    ): Response<String>
}
