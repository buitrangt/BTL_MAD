package com.arijit.budgettracker

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arijit.budgettracker.utils.ChatAdapter
import com.arijit.budgettracker.api.RetrofitClient
import com.arijit.budgettracker.models.ChatMessage
import kotlinx.coroutines.launch

class FinChatActivity : AppCompatActivity() {
    private lateinit var adapter: ChatAdapter
    private val chatList = mutableListOf<ChatMessage>()
    private lateinit var rvChat: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fin_chat)

        rvChat = findViewById(R.id.rvChat)
        val etMessage = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)

        // 1. Khởi tạo RecyclerView
        adapter = ChatAdapter(chatList)
        rvChat.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true // Tin nhắn mới xuất hiện từ dưới lên
        }
        rvChat.adapter = adapter

        // Tin nhắn chào mừng
        if (chatList.isEmpty()) {
            chatList.add(ChatMessage("Chào bạn! Tôi là FinBot, tôi có thể giúp gì cho tài chính của bạn?", true))
            adapter.notifyItemInserted(0)
        }

        // 2. Sự kiện nút Send
        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                addUserMessage(text)
                etMessage.text.clear()
                askAi(text)
            }
        }
    }

    private fun addUserMessage(text: String) {
        chatList.add(ChatMessage(text, false))
        adapter.notifyItemInserted(chatList.size - 1)
        rvChat.scrollToPosition(chatList.size - 1)
    }

    private fun askAi(query: String) {
        lifecycleScope.launch {
            try {
                // Thay '1L' bằng ID user thật nếu Huy đã lưu trong SharedPreferences
                val response = RetrofitClient.getApiService(this@FinChatActivity).askFinChat(query, 1L)
                if (response.isSuccessful) {
                    val botResponse = response.body() ?: "Xin lỗi, tôi không nhận được phản hồi."
                    chatList.add(ChatMessage(botResponse, true))
                    adapter.notifyItemInserted(chatList.size - 1)
                    rvChat.smoothScrollToPosition(chatList.size - 1)
                }
            } catch (e: Exception) {
                Toast.makeText(this@FinChatActivity, "Lỗi kết nối AI: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}