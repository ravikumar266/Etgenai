package com.etgenai.app.ui.scheduler

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etgenai.app.data.api.RetrofitClient
import com.etgenai.app.data.model.HealthResponse
import com.etgenai.app.data.model.SchedulerStatus
import kotlinx.coroutines.launch

class SchedulerViewModel : ViewModel() {

    private val _schedulerStatus = MutableLiveData<SchedulerStatus>()
    val schedulerStatus: LiveData<SchedulerStatus> = _schedulerStatus

    private val _healthStatus = MutableLiveData<HealthResponse>()
    val healthStatus: LiveData<HealthResponse> = _healthStatus

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    fun fetchSchedulerStatus() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getSchedulerStatus()
                if (response.isSuccessful && response.body() != null) {
                    _schedulerStatus.value = response.body()!!
                }
            } catch (e: Exception) {
                _message.value = "Error fetching scheduler status"
            }
        }
    }

    fun fetchHealth() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getHealth()
                if (response.isSuccessful && response.body() != null) {
                    _healthStatus.value = response.body()!!
                }
            } catch (e: Exception) {
                _message.value = "Error fetching health"
            }
        }
    }
}
