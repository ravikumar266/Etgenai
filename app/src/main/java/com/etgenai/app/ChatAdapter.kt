package com.etgenai.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Legacy adapter — retained for backward compatibility with item_chat_message layout.
 * The main chat now uses com.etgenai.app.ui.chat.ChatAdapter.
 */
data class LegacyChatMessage(val role: String, val text: String)

class ChatAdapter(private val messages: MutableList<LegacyChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val roleText: TextView = view.findViewById(R.id.roleText)
        val messageText: TextView = view.findViewById(R.id.messageText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        holder.roleText.text = if (message.role == "user") "You" else "Axiom Agent"
        holder.messageText.text = message.text

        if (message.role == "user") {
            holder.messageText.setTextColor(android.graphics.Color.DKGRAY)
        } else {
            holder.messageText.setTextColor(android.graphics.Color.BLACK)
        }
    }

    override fun getItemCount() = messages.size

    fun addMessage(message: LegacyChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }
}
