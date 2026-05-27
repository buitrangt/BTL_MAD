package com.arijit.budgettracker.api

import android.content.Context
import com.arijit.budgettracker.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor xác thực mạng (OkHttp Interceptor) của ứng dụng.
 * Chịu trách nhiệm:
 * 1. Chặn toàn bộ các yêu cầu HTTP gửi từ Client đi trước khi truyền qua Internet.
 * 2. Đọc token JWT hiện lưu cục bộ trong máy qua TokenManager.
 * 3. Nếu token tồn tại, tự động đính kèm thêm Header `Authorization: Bearer <jwt_token>`.
 * 4. Chuyển tiếp Request đã nâng cấp đi đến server. Giúp bảo mật thông tin các API cần quyền hạn truy cập.
 */
class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
        
        // Truy vấn xem người dùng đã đăng nhập và lưu JWT Token chưa
        TokenManager.getToken(context)?.let { token ->
            // Đính kèm Token vào Header của request gửi lên Server Spring Boot
            request.addHeader("Authorization", "Bearer $token")
        }
        
        return chain.proceed(request.build())
    }
}
