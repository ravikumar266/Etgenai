package com.etgenai.app.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.etgenai.app.R
import com.etgenai.app.data.model.ChatItem
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin

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
        private const val TYPE_CODE = 5   // 🔥 NEW
    }

    private val items = mutableListOf<ChatItem>()

    fun setItems(newItems: List<ChatItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is ChatItem.UserMsg -> TYPE_USER
        is ChatItem.AiMsg -> TYPE_AI
        is ChatItem.EmailApproval -> TYPE_EMAIL_APPROVAL
        is ChatItem.SystemMsg -> TYPE_SYSTEM
        is ChatItem.Loading -> TYPE_LOADING
        is ChatItem.CodeBlock -> TYPE_CODE   // ✅ NEW
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_USER -> UserVH(inf.inflate(R.layout.item_msg_user, parent, false))
            TYPE_AI -> AiVH(inf.inflate(R.layout.item_msg_ai, parent, false))
            TYPE_EMAIL_APPROVAL -> EmailVH(inf.inflate(R.layout.item_approval_email, parent, false))
            TYPE_SYSTEM -> SystemVH(inf.inflate(R.layout.item_msg_ai, parent, false))
            TYPE_LOADING -> LoadingVH(inf.inflate(R.layout.item_msg_ai, parent, false))
            TYPE_CODE -> CodeVH(inf.inflate(R.layout.item_code_block, parent, false)) // 🔥 NEW
            else -> throw IllegalArgumentException("Invalid type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ChatItem.UserMsg -> (holder as UserVH).bind(item)
            is ChatItem.AiMsg -> (holder as AiVH).bind(item)
            is ChatItem.EmailApproval -> (holder as EmailVH).bind(item)
            is ChatItem.SystemMsg -> (holder as SystemVH).bind(item)
            is ChatItem.Loading -> (holder as LoadingVH).bind(item)
            is ChatItem.CodeBlock -> (holder as CodeVH).bind(item) //new
        }
    }

    override fun getItemCount() = items.size
    class UserVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvText: TextView = view.findViewById(R.id.tvUserMsg)

        fun bind(item: ChatItem.UserMsg) {
            tvText.text = item.text
        }
    }

    class AiVH(view: View) : RecyclerView.ViewHolder(view) {

        private val tvText: TextView = view.findViewById(R.id.tvAiMsg)

        private val markwon = Markwon.builder(view.context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TaskListPlugin.create(view.context))
            .build()

        fun bind(item: ChatItem.AiMsg) {

            val text = if (item.tools.isEmpty()) {
                item.text
            } else {
                item.text + "\n\n🔧 " + item.tools.joinToString(" · ")
            }
            markwon.setMarkdown(tvText, text)
        }
    }

    class CodeVH(view: View) : RecyclerView.ViewHolder(view) {

        private val tvLang: TextView = view.findViewById(R.id.tvLang)
        private val tvCode: TextView = view.findViewById(R.id.tvCode)
        private val btnCopy: ImageView = view.findViewById(R.id.btnCopy)

        fun bind(item: ChatItem.CodeBlock) {
            val raw = item.code.trim()

            var language = "code"
            var actualCode = raw

            val lines = raw.lines()

            if (lines.isNotEmpty() && lines[0].trim().startsWith("```")) {
                val firstLine = lines[0].trim()
                language = firstLine.removePrefix("```").trim().ifEmpty { "code" }

                actualCode = lines
                    .drop(1)
                    .dropLast(1)
                    .joinToString("\n")
                    .trim()
            }

            tvLang.text = language
            tvCode.text = actualCode

            btnCopy.setOnClickListener {
                val clipboard = itemView.context.getSystemService(Context.CLIPBOARD_SERVICE)
                        as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("code", actualCode))
                Toast.makeText(itemView.context, "Code Copied", Toast.LENGTH_SHORT).show()
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