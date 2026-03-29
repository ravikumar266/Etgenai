package com.etgenai.app.data.model

import com.google.gson.annotations.SerializedName

// ── Chat ─────────────────────────────────────────────────────────────────────

data class ChatRequest(
    @SerializedName("message") val message: String,
    @SerializedName("thread_id") val threadId: String = ""
)

data class ChatResponse(
    @SerializedName("reply") val reply: String,
    @SerializedName("thread_id") val threadId: String,
    @SerializedName("tools_used") val toolsUsed: List<String>?,
    @SerializedName("pending_email") val pendingEmail: PendingEmail? = null,
    @SerializedName("email_approval_required") val emailApprovalRequired: Boolean = false
)

data class PendingEmail(
    @SerializedName("to") val to: String,
    @SerializedName("subject") val subject: String,
    @SerializedName("body") val body: String
)

// ── Email Approval ───────────────────────────────────────────────────────────

data class EmailApprovalRequest(
    @SerializedName("thread_id") val threadId: String,
    @SerializedName("decision") val decision: String
)

data class EmailApprovalResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("decision") val decision: String,
    @SerializedName("message") val message: String,
    @SerializedName("email") val email: Map<String, String>? = null
)

// ── Threads ──────────────────────────────────────────────────────────────────

data class NewThreadResponse(
    @SerializedName("thread_id") val threadId: String
)

data class HistoryResponse(
    @SerializedName("thread_id") val threadId: String,
    @SerializedName("summary") val summary: String?,
    @SerializedName("messages") val messages: List<HistoryMessage>,
    @SerializedName("count") val count: Int
)

data class HistoryMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String,
    @SerializedName("created_at") val createdAt: String? = null
)

data class ThreadsListResponse(
    @SerializedName("threads") val threads: List<ThreadSummary>,
    @SerializedName("total") val total: Int
)

data class ThreadSummary(
    @SerializedName("thread_id") val threadId: String,
    @SerializedName("message_count") val messageCount: Int = 0,
    @SerializedName("last_active") val lastActive: String? = null
)

// ── RAG ──────────────────────────────────────────────────────────────────────

data class RagIngestUrlRequest(
    @SerializedName("url") val url: String,
    @SerializedName("collection") val collection: String = "default"
)

data class RagIngestResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("chunks") val chunks: Int = 0,
    @SerializedName("collection") val collection: String? = null
)

data class UploadPdfResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("chunks") val chunks: Int = 0,
    @SerializedName("collection") val collection: String? = null,
    @SerializedName("filename") val filename: String? = null,
    @SerializedName("size_kb") val sizeKb: Double = 0.0
)

// ── Health & Scheduler ───────────────────────────────────────────────────────

data class HealthResponse(
    @SerializedName("status") val status: String,
    @SerializedName("version") val version: String,
    @SerializedName("scheduler") val scheduler: Boolean = false,
    @SerializedName("pending_emails") val pendingEmails: Int = 0
)

data class SchedulerStatus(
    @SerializedName("running") val running: Boolean,
    @SerializedName("jobs") val jobs: List<SchedulerJob> = emptyList()
)

data class SchedulerJob(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("next_run") val nextRun: String? = null
)

// ── UI Models ────────────────────────────────────────────────────────────────

sealed class ChatItem {
    data class UserMsg(val text: String) : ChatItem()
    data class AiMsg(val text: String, val tools: List<String> = emptyList()) : ChatItem()
    data class EmailApproval(val email: PendingEmail) : ChatItem()
    data class SystemMsg(val text: String) : ChatItem()
    data class Loading(val message: String = "Uploading...") : ChatItem()
}
