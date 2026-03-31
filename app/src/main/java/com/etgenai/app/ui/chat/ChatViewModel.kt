package com.etgenai.app.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etgenai.app.data.api.RetrofitClient
import com.etgenai.app.data.model.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Job

class ChatViewModel : ViewModel() {

    private val _chatItems = MutableLiveData<List<ChatItem>>(emptyList())
    val chatItems: LiveData<List<ChatItem>> = _chatItems

    private val _threadId = MutableLiveData<String?>()
    val threadId: LiveData<String?> = _threadId

    private val _recentThreads = MutableLiveData<List<ThreadSummary>>(emptyList())
    val recentThreads: LiveData<List<ThreadSummary>> = _recentThreads

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    var currentThreadId: String? = null

    private var currentJob: Job? = null

    // update sendMessage() start + launch handling
    fun sendMessage(text: String) {
        if (_isLoading.value == true) return

        _isLoading.value = true
        addItem(ChatItem.UserMsg(text))
        addItem(ChatItem.Loading("Agent is thinking..."))

        currentJob = viewModelScope.launch {
            try {
                val req = ChatRequest(message = text, threadId = currentThreadId ?: "")
                val response = RetrofitClient.apiService.chat(req)

                removeLastCard<ChatItem.Loading>()

                if (response.isSuccessful && response.body() != null) {
                    val res = response.body()!!
                    currentThreadId = res.threadId
                    _threadId.value = res.threadId

                    val parts = splitMessage(res.reply)
                    parts.forEach { addItem(it) }

                    if (res.emailApprovalRequired && res.pendingEmail != null) {
                        addItem(ChatItem.EmailApproval(res.pendingEmail))
                    }
                } else {
                    _error.value = "Error ${response.code()}: ${response.message()}"
                }
            } catch (e: Exception) {
                removeLastCard<ChatItem.Loading>()
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun stopResponse() {
        currentJob?.cancel()
        removeLastCard<ChatItem.Loading>()
        _isLoading.value = false
    }

    fun sendMessageWithPdf(text: String, file: File?) {
        if (_isLoading.value == true) return
        if (file != null) {
            addItem(ChatItem.Loading("Uploading PDF..."))
            viewModelScope.launch {
                try {
                    val requestFile = file.asRequestBody("application/pdf".toMediaTypeOrNull())
                    val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

                    if (currentThreadId == null) {
                        currentThreadId = UUID.randomUUID().toString()
                    }

                    val currentId = currentThreadId ?: "default"
                    val collectionName = currentId.toRequestBody("text/plain".toMediaTypeOrNull())

                    val response = RetrofitClient.apiService.uploadPdf(body, collectionName)
                    removeLastCard<ChatItem.Loading>()

                    if (response.isSuccessful && response.body()?.success == true) {
                        addItem(ChatItem.SystemMsg("✅ ${response.body()!!.message}"))
                        if (text.isNotBlank()) {
                            sendMessage(text)
                        }
                    } else {
                        _error.value = "Upload failed. Skipping prompt."
                    }
                } catch (e: Exception) {
                    removeLastCard<ChatItem.Loading>()
                    _error.value = "Upload error: ${e.message}"
                }
            }
        } else if (text.isNotBlank()) {
            sendMessage(text)
        }
    }

    fun approveEmail(decision: String) {
        removeLastCard<ChatItem.EmailApproval>()
        val threadId = currentThreadId ?: return

        viewModelScope.launch {
            try {
                val req = EmailApprovalRequest(threadId = threadId, decision = decision)
                val response = RetrofitClient.apiService.approveEmail(req)

                if (response.isSuccessful && response.body() != null) {
                    val res = response.body()!!
                    val icon = if (res.decision == "approved") "✅" else "❌"
                    addItem(ChatItem.SystemMsg("$icon ${res.message}"))
                } else {
                    _error.value = "Approval error ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            }
        }
    }

    fun loadThread(threadId: String) {
        currentThreadId = threadId
        _threadId.value = threadId
        _chatItems.value = listOf(ChatItem.Loading("Loading conversation..."))

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getHistory(threadId)

                if (response.isSuccessful && response.body() != null) {
                    val history = response.body()!!

                    val items = history.messages.flatMap { msg ->
                        when (msg.role.lowercase()) {
                            "human", "user" -> listOf(ChatItem.UserMsg(msg.content))
                            else -> splitMessage(msg.content)
                        }
                    }

                    _chatItems.value = if (items.isEmpty()) {
                        listOf(ChatItem.AiMsg("This thread has no messages yet."))
                    } else {
                        items
                    }
                } else {
                    _chatItems.value = emptyList()
                    _error.value = "Could not load thread: ${response.code()}"
                }
            } catch (e: Exception) {
                _chatItems.value = emptyList()
                _error.value = "Load error: ${e.message}"
            }
        }
    }

    fun fetchRecentThreads() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getThreads()
                if (response.isSuccessful && response.body() != null) {
                    _recentThreads.value = response.body()!!.threads
                }
            } catch (_: Exception) {
            }
        }
    }

    fun resetThread() {
        viewModelScope.launch {
            try {
                val res = RetrofitClient.apiService.createThread()
                if (res.isSuccessful && res.body() != null) {
                    currentThreadId = res.body()!!.threadId
                    _threadId.value = currentThreadId
                    _chatItems.value = listOf(
                        ChatItem.AiMsg("New thread started. How can I help?")
                    )
                }
            } catch (e: Exception) {
                _error.value = "Reset error: ${e.message}"
            }
        }
    }

    fun uploadPdf(file: File, collection: String = "default") {
        addItem(ChatItem.Loading("Ingesting PDF..."))
        viewModelScope.launch {
            try {
                val requestFile = file.asRequestBody("application/pdf".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
                val collectionName = collection.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = RetrofitClient.apiService.uploadPdf(body, collectionName)
                removeLastCard<ChatItem.Loading>()

                if (response.isSuccessful && response.body() != null) {
                    val res = response.body()!!
                    if (res.success) {
                        addItem(ChatItem.SystemMsg("✅ ${res.message}"))
                    } else {
                        addItem(ChatItem.SystemMsg("⚠️ ${res.message}"))
                    }
                } else {
                    _error.value = "Upload failed: ${response.code()}"
                }
            } catch (e: Exception) {
                removeLastCard<ChatItem.Loading>()
                _error.value = "Upload error: ${e.message}"
            }
        }
    }

    fun ingestUrl(url: String, collection: String = "default") {
        addItem(ChatItem.Loading("Ingesting URL..."))
        viewModelScope.launch {
            try {
                val req = RagIngestUrlRequest(url, collection)
                val response = RetrofitClient.apiService.ingestUrl(req)

                removeLastCard<ChatItem.Loading>()

                if (response.isSuccessful && response.body() != null) {
                    val res = response.body()!!
                    if (res.success) {
                        addItem(ChatItem.SystemMsg("✅ ${res.message}"))
                    } else {
                        addItem(ChatItem.SystemMsg("⚠️ ${res.message}"))
                    }
                } else {
                    _error.value = "Ingestion failed: ${response.code()}"
                }
            } catch (e: Exception) {
                removeLastCard<ChatItem.Loading>()
                _error.value = "Ingestion error: ${e.message}"
            }
        }
    }

    private fun splitMessage(text: String): List<ChatItem> {
        val list = mutableListOf<ChatItem>()

        // Matches full fenced blocks, including ```lang and closing ```
        val regex = Regex("```[\\w-]*\\n[\\s\\S]*?```", RegexOption.MULTILINE)
        var last = 0

        for (match in regex.findAll(text)) {
            if (match.range.first > last) {
                val before = text.substring(last, match.range.first)
                if (before.isNotBlank()) {
                    list.add(ChatItem.AiMsg(before))
                }
            }

            // Keep the full block so the adapter can extract language + code
            list.add(ChatItem.CodeBlock(match.value.trim()))
            last = match.range.last + 1
        }

        if (last < text.length) {
            val after = text.substring(last)
            if (after.isNotBlank()) {
                list.add(ChatItem.AiMsg(after))
            }
        }

        return list
    }

    private fun addItem(item: ChatItem) {
        val list = _chatItems.value?.toMutableList() ?: mutableListOf()
        list.add(item)
        _chatItems.value = list
    }

    private inline fun <reified T : ChatItem> removeLastCard() {
        val list = _chatItems.value?.toMutableList() ?: return
        val idx = list.indexOfLast { it is T }
        if (idx >= 0) {
            list.removeAt(idx)
            _chatItems.value = list
        }
    }
}