package com.etgenai.app.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.etgenai.app.R
import com.etgenai.app.data.model.*

class ChatAdapter(
    private val onApproveEmail: () -> Unit,
    private val onDenyEmail: () -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_USER = 0
        private const val TYPE_AI = 1
        private const val TYPE_EMAIL_APPROVAL = 2
        private const val TYPE_SYSTEM = 3
        private const val TYPE_LOADING = 4
    }

    private val items = mutableListOf<ChatItem>()

    fun setItems(newItems: List<ChatItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) = when (items[position]) {
        is ChatItem.UserMsg -> TYPE_USER
        is ChatItem.AiMsg -> TYPE_AI
        is ChatItem.EmailApproval -> TYPE_EMAIL_APPROVAL
        is ChatItem.SystemMsg -> TYPE_SYSTEM
        is ChatItem.Loading -> TYPE_LOADING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_USER -> UserVH(inf.inflate(R.layout.item_msg_user, parent, false))
            TYPE_AI -> AiVH(inf.inflate(R.layout.item_msg_ai, parent, false))
            TYPE_EMAIL_APPROVAL -> EmailVH(inf.inflate(R.layout.item_approval_email, parent, false))
            TYPE_SYSTEM -> SystemVH(inf.inflate(R.layout.item_msg_ai, parent, false))
            else -> LoadingVH(inf.inflate(R.layout.item_msg_ai, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ChatItem.UserMsg -> (holder as UserVH).bind(item)
            is ChatItem.AiMsg -> (holder as AiVH).bind(item)
            is ChatItem.EmailApproval -> (holder as EmailVH).bind(item)
            is ChatItem.SystemMsg -> (holder as SystemVH).bind(item)
            is ChatItem.Loading -> (holder as LoadingVH).bind(item)
        }
    }

    override fun getItemCount() = items.size

    // ─── View Holders ───────────────────────────────────────────────────────────

    class UserVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvText: TextView = view.findViewById(R.id.tvUserMsg)
        fun bind(item: ChatItem.UserMsg) { tvText.text = item.text }
    }

    class AiVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvText: TextView = view.findViewById(R.id.tvAiMsg)
        fun bind(item: ChatItem.AiMsg) {
            if (item.tools.isEmpty()) {
                tvText.text = item.text
            } else {
                val toolLine = "\n\n🔧 ${item.tools.joinToString(" · ")}"
                tvText.text = "${item.text}$toolLine"
            }
        }
    }

    inner class EmailVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTo: TextView = view.findViewById(R.id.tvEmailTo)
        private val tvSubject: TextView = view.findViewById(R.id.tvEmailSubject)
        private val tvBody: TextView = view.findViewById(R.id.tvEmailBody)
        private val btnApprove: Button = view.findViewById(R.id.btnApproveEmail)
        private val btnDeny: Button = view.findViewById(R.id.btnDenyEmail)

        fun bind(item: ChatItem.EmailApproval) {
            tvTo.text = item.email.to
            tvSubject.text = item.email.subject
            tvBody.text = item.email.body
            btnApprove.setOnClickListener { onApproveEmail() }
            btnDeny.setOnClickListener { onDenyEmail() }
        }
    }

    class SystemVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvText: TextView = view.findViewById(R.id.tvAiMsg)
        fun bind(item: ChatItem.SystemMsg) {
            tvText.text = item.text
            tvText.alpha = 0.7f
        }
    }

    class LoadingVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvText: TextView = view.findViewById(R.id.tvAiMsg)
        fun bind(item: ChatItem.Loading) {
            tvText.text = "⏳ ${item.message}"
        }
    }
}
