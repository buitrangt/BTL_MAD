package com.arijit.budgettracker

import android.os.Bundle
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

class FinChatActivity : AppCompatActivity() {
    private lateinit var adapter: ChatAdapter
    private val chatList = mutableListOf<ChatMessage>()
    private lateinit var rvChat: RecyclerView
    private lateinit var btnSend: ImageButton

    /** Persistent across the same chat — server keeps history under this id. */
    private var sessionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fin_chat)

        rvChat = findViewById(R.id.rvChat)
        val etMessage = findViewById<EditText>(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        adapter = ChatAdapter(chatList)
        rvChat.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvChat.adapter = adapter

        // Greeting
        chatList.add(
            ChatMessage(
                "Chào bạn! Tôi là FinBot 🤖\nHãy hỏi tôi bất cứ điều gì về tài chính của bạn — chi tiêu, dự đoán, ngân sách, mẹo tiết kiệm…",
                true
            )
        )
        adapter.notifyItemInserted(0)

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

    private fun addBotMessage(text: String) {
        chatList.add(ChatMessage(text, true))
        adapter.notifyItemInserted(chatList.size - 1)
        rvChat.smoothScrollToPosition(chatList.size - 1)
    }

    private fun askAi(query: String) {
        btnSend.isEnabled = false
        // Loading placeholder
        addBotMessage("…")
        val loadingIndex = chatList.size - 1

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.getApiService(this@FinChatActivity)
                        .sendChatMessage(ChatRequest(message = query, sessionId = sessionId))
                }

                // Remove loading indicator
                chatList.removeAt(loadingIndex)
                adapter.notifyItemRemoved(loadingIndex)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    sessionId = body.sessionId
                    addBotMessage(body.reply)
                } else {
                    addBotMessage("Xin lỗi, không nhận được phản hồi (${response.code()}).")
                }
            } catch (e: Exception) {
                if (loadingIndex < chatList.size) {
                    chatList.removeAt(loadingIndex)
                    adapter.notifyItemRemoved(loadingIndex)
                }
                Toast.makeText(this@FinChatActivity, "Lỗi kết nối AI: ${e.message}", Toast.LENGTH_SHORT).show()
                addBotMessage("Hiện không thể kết nối tới AI. Vui lòng thử lại sau.")
            } finally {
                btnSend.isEnabled = true
            }
        }
    }
}
