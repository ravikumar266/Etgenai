package com.etgenai.app.ui.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etgenai.app.data.api.RetrofitClient
import com.etgenai.app.data.model.ThreadSummary
import kotlinx.coroutines.launch

class HistoryViewModel : ViewModel() {

    private val _threads = MutableLiveData<List<ThreadSummary>>(emptyList())
    val threads: LiveData<List<ThreadSummary>> = _threads

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun fetchThreads() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getThreads()
                if (response.isSuccessful && response.body() != null) {
                    _threads.value = response.body()!!.threads
                } else {
                    _error.value = "Error ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Error loading threads: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
