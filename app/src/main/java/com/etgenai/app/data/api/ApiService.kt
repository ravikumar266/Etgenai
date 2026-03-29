package com.etgenai.app.data.api

import com.etgenai.app.data.model.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.MultipartBody
import okhttp3.RequestBody

interface ApiService {

    // ── Chat ─────────────────────────────────────────────────────────────────
    @POST("/chat")
    suspend fun chat(@Body request: ChatRequest): Response<ChatResponse>

    // ── Email Approval ───────────────────────────────────────────────────────
    @POST("/email/approve")
    suspend fun approveEmail(@Body request: EmailApprovalRequest): Response<EmailApprovalResponse>

    @GET("/email/pending/{thread_id}")
    suspend fun getPendingEmail(@Path("thread_id") threadId: String): Response<Map<String, Any>>

    // ── Threads ──────────────────────────────────────────────────────────────
    @POST("/threads/new")
    suspend fun createThread(): Response<NewThreadResponse>

    @GET("/threads")
    suspend fun getThreads(): Response<ThreadsListResponse>

    @GET("/history/{thread_id}")
    suspend fun getHistory(@Path("thread_id") threadId: String): Response<HistoryResponse>

    // ── PDF Upload ───────────────────────────────────────────────────────────
    @Multipart
    @POST("/upload-pdf")
    suspend fun uploadPdf(
        @Part file: MultipartBody.Part,
        @Part("collection") collection: RequestBody
    ): Response<UploadPdfResponse>

    // ── RAG ──────────────────────────────────────────────────────────────────
    @POST("/rag/ingest-url")
    suspend fun ingestUrl(@Body request: RagIngestUrlRequest): Response<RagIngestResponse>

    // ── Health & Scheduler ───────────────────────────────────────────────────
    @GET("/health")
    suspend fun getHealth(): Response<HealthResponse>

    @GET("/scheduler/status")
    suspend fun getSchedulerStatus(): Response<SchedulerStatus>
}
