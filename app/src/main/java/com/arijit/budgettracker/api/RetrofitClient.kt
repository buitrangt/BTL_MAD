package com.arijit.budgettracker.api

import android.content.Context
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

/**
 * Singleton Object quản lý và cung cấp Retrofit Client của ứng dụng.
 * Chịu trách nhiệm:
 * 1. Khởi tạo OkHttpClient đính kèm interceptor xác thực (AuthInterceptor) để tự động điền Token.
 * 2. Thiết lập thời gian Timeout (60 giây) cho các cuộc gọi mạng (cần thiết vì phản hồi từ Gemini AI có thể chậm).
 * 3. Tích hợp Logger Interceptor để debug HTTP request/response dễ dàng trong quá trình phát triển.
 * 4. Cấu hình Gson Converter Factory chuyển đổi JSON sang các đối tượng Kotlin DTO.
 */
object RetrofitClient {
    // Địa chỉ IP của máy chủ API. Sử dụng IP đặc biệt 10.0.2.2 để Emulator kết nối được với localhost của máy tính chạy server.
    private const val BASE_URL = "http://10.0.2.2:8080/"

    private var apiService: ApiService? = null

    /**
     * Trả về Instance duy nhất (Singleton) của ApiService được khởi tạo và cấu hình đầy đủ.
     */
    fun getApiService(context: Context): ApiService {
        if (apiService == null) {
            // Khởi tạo Logging Interceptor để ghi log toàn bộ thông tin HTTP payload
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            // Xây dựng OkHttpClient tùy biến
            val client = OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .addInterceptor(AuthInterceptor(context.applicationContext)) // Interceptor đính kèm JWT Token
                .addInterceptor(logging) // Interceptor ghi log
                .build()

            // Cấu hình Gson ở chế độ mềm dẻo (lenient) để tránh crash khi phân tích JSON không chuẩn hóa từ AI
            val gson = GsonBuilder()
                .setLenient()
                .create()

            // Khởi tạo Retrofit Builder
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(ScalarsConverterFactory.create()) // Hỗ trợ nhận dữ liệu chuỗi thô (raw String)
                .addConverterFactory(GsonConverterFactory.create(gson)) // Hỗ trợ JSON mapping
                .build()

            apiService = retrofit.create(ApiService::class.java)
        }
        return apiService!!
    }
}
