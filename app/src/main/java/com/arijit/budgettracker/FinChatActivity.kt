package com.arijit.budgettracker

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arijit.budgettracker.api.ChatRequest
import com.arijit.budgettracker.api.RetrofitClient
import com.arijit.budgettracker.models.ChatMessage
import com.arijit.budgettracker.utils.ChatAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity màn hình giao diện trò chuyện với Trợ lý tài chính ảo FinChat
 * Hiển thị hội thoại dạng bong bóng chat và kết nối API Gemini thông qua server Spring Boot.
 */
class FinChatActivity : AppCompatActivity() {
    private lateinit var adapter: ChatAdapter
    private val chatList = mutableListOf<ChatMessage>()
    private lateinit var rvChat: RecyclerView
    private lateinit var btnSend: ImageButton

    /** 
     * Lưu trữ mã phiên chat (Session ID). 
     * Server sẽ dùng mã này để liên kết và truy vấn lịch sử cuộc trò chuyện.
     */
    private var sessionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fin_chat)

        // Ánh xạ các View từ Layout XML
        rvChat = findViewById(R.id.rvChat)
        val etMessage = findViewById<EditText>(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        // Xử lý sự kiện nhấn nút quay lại
        val goBack = View.OnClickListener {
            Toast.makeText(this, "Quay lại", Toast.LENGTH_SHORT).show()
            onBackPressedDispatcher.onBackPressed()
        }
        btnBack.setOnClickListener(goBack)
        btnBack.bringToFront()

        // Thiết lập Adapter và LayoutManager cho RecyclerView hiển thị tin nhắn
        adapter = ChatAdapter(chatList)
        rvChat.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true // Hiển thị tin nhắn từ dưới lên
        }
        rvChat.adapter = adapter

        // Gửi lời chào mở đầu từ FinBot
        chatList.add(
            ChatMessage(
                "Chào bạn! Tôi là FinBot 🤖\nHãy hỏi tôi bất cứ điều gì về tài chính của bạn — chi tiêu, dự đoán, ngân sách, mẹo tiết kiệm…",
                true
            )
        )
        adapter.notifyItemInserted(0)

        // Xử lý sự kiện khi người dùng click nút Gửi tin nhắn
        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                addUserMessage(text) // Thêm tin nhắn của User vào giao diện
                etMessage.text.clear() // Xóa ô nhập liệu
                askAi(text) // Gửi câu hỏi lên AI
            }
        }
    }

    /**
     * Thêm tin nhắn của người dùng (User) vào RecyclerView
     */
    private fun addUserMessage(text: String) {
        chatList.add(ChatMessage(text, false))
        adapter.notifyItemInserted(chatList.size - 1)
        rvChat.scrollToPosition(chatList.size - 1) // Cuộn tới tin nhắn mới nhất
    }

    /**
     * Thêm tin nhắn của Robot (AI) vào RecyclerView
     */
    private fun addBotMessage(text: String) {
        chatList.add(ChatMessage(text, true))
        adapter.notifyItemInserted(chatList.size - 1)
        rvChat.smoothScrollToPosition(chatList.size - 1) // Cuộn mượt tới câu trả lời
    }

    /**
     * Gọi API gửi câu hỏi của người dùng lên Server để chuyển tới Gemini AI
     */
    private fun askAi(query: String) {
        btnSend.isEnabled = false
        // Thêm bong bóng chat hiển thị dấu "..." đại diện cho trạng thái đang tải
        addBotMessage("…")
        val loadingIndex = chatList.size - 1

        lifecycleScope.launch {
            try {
                // Gọi API bất đồng bộ trên luồng Background Thread (IO)
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.getApiService(this@FinChatActivity)
                        .sendChatMessage(ChatRequest(message = query, sessionId = sessionId))
                }

                // Xóa bong bóng loading "..."
                chatList.removeAt(loadingIndex)
                adapter.notifyItemRemoved(loadingIndex)

                // Cập nhật phản hồi từ AI lên giao diện
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    sessionId = body.sessionId // Cập nhật Session ID từ server
                    addBotMessage(body.reply) // Hiển thị câu trả lời của AI
                } else {
                    addBotMessage("Xin lỗi, không nhận được phản hồi (${response.code()}).")
                }
            } catch (e: Exception) {
                // Xóa trạng thái loading nếu bị lỗi kết nối
                if (loadingIndex < chatList.size) {
                    chatList.removeAt(loadingIndex)
                    adapter.notifyItemRemoved(loadingIndex)
                }
                Toast.makeText(this@FinChatActivity, "Lỗi kết nối AI: ${e.message}", Toast.LENGTH_SHORT).show()
                addBotMessage("Hiện không thể kết nối tới AI. Vui lòng thử lại sau.")
            } finally {
                btnSend.isEnabled = true // Kích hoạt lại nút gửi
            }
        }
    }
}
