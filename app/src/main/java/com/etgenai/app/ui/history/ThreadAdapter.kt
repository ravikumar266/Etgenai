package com.etgenai.app.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.etgenai.app.R
import com.etgenai.app.data.model.ThreadSummary

class ThreadAdapter(
    private val onThreadClick: (String) -> Unit
) : RecyclerView.Adapter<ThreadAdapter.ThreadVH>() {

    private val items = mutableListOf<ThreadSummary>()

    fun setItems(newItems: List<ThreadSummary>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThreadVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_thread, parent, false)
        return ThreadVH(view)
    }

    override fun onBindViewHolder(holder: ThreadVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ThreadVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvThreadId: TextView = view.findViewById(R.id.tvThreadId)
        private val tvMessageCount: TextView = view.findViewById(R.id.tvMessageCount)
        private val tvLastMessage: TextView = view.findViewById(R.id.tvLastMessage)

        fun bind(thread: ThreadSummary) {
            val title = if (!thread.lastActive.isNullOrEmpty()) {
                "Active: ${thread.lastActive.substringBefore(" ")}"
            } else {
                "Thread ${thread.threadId.take(4)}"
            }
            
            tvThreadId.text = title
            tvMessageCount.text = "${thread.messageCount} messages"
            
            itemView.setOnClickListener {
                onThreadClick(thread.threadId)
            }
        }
    }
}
