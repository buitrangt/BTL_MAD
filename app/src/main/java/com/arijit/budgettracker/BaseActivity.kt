package com.arijit.budgettracker

import android.annotation.SuppressLint
import android.content.Intent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs

open class BaseActivity : AppCompatActivity() {

    /**
     * Wires the floating chat bubble:
     *  - Tap → open FinChatActivity
     *  - Long press + drag → free-move within parent bounds (chat-head style)
     */
    @SuppressLint("ClickableViewAccessibility")
    fun setupChatFab() {
        val fab = findViewById<View>(R.id.fabFinChat) ?: return

        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop

        var dX = 0f
        var dY = 0f
        var startRawX = 0f
        var startRawY = 0f
        var moved = false

        fab.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dX = v.x - event.rawX
                    dY = v.y - event.rawY
                    startRawX = event.rawX
                    startRawY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - startRawX
                    val dy = event.rawY - startRawY
                    if (!moved && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                        moved = true
                    }
                    if (moved) {
                        val parent = v.parent as? View ?: return@setOnTouchListener true
                        val newX = (event.rawX + dX)
                            .coerceIn(0f, (parent.width - v.width).toFloat())
                        val newY = (event.rawY + dY)
                            .coerceIn(0f, (parent.height - v.height).toFloat())
                        v.animate()
                            .x(newX)
                            .y(newY)
                            .setDuration(0)
                            .start()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        // Treated as a tap → open chat
                        v.performClick()
                        startActivity(Intent(this, FinChatActivity::class.java))
                    } else {
                        // Snap to nearest horizontal edge
                        val parent = v.parent as? View
                        if (parent != null) {
                            val centerX = v.x + v.width / 2f
                            val targetX = if (centerX < parent.width / 2f) {
                                0f
                            } else {
                                (parent.width - v.width).toFloat()
                            }
                            v.animate()
                                .x(targetX)
                                .setDuration(180)
                                .start()
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }
}
