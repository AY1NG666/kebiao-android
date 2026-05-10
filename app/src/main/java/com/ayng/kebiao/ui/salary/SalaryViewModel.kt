package com.ayng.kebiao.ui.salary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ayng.kebiao.data.repository.AppRepository
import com.ayng.kebiao.data.repository.SalaryBreakdown
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class SalaryViewModel(private val repo: AppRepository) : ViewModel() {

    val currentYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val currentMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))

    private val _breakdown = MutableStateFlow<SalaryBreakdown?>(null)
    val breakdown: StateFlow<SalaryBreakdown?> = _breakdown

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Track last known data version to avoid unnecessary reloads
    private var lastVersion = -1

    init {
        loadSalary()
        // Observe data changes to auto-refresh
        viewModelScope.launch {
            repo.dataVersion.collectLatest { version ->
                if (version != lastVersion) {
                    lastVersion = version
                    loadSalary(version)
                }
            }
        }
    }

    fun goToMonth(year: Int, month: Int) {
        currentYear.value = year
        currentMonth.value = month
        loadSalary()
    }

    private fun loadSalary(fromVersion: Int? = null) {
        viewModelScope.launch {
            // Only show loading indicator first time (no previous data)
            val showLoading = _breakdown.value == null
            if (showLoading) {
                _breakdown.value = SalaryBreakdown(currentYear.value, currentMonth.value, -1.0, 0, emptyList())
            }
            try {
                _error.value = null
                _breakdown.value = repo.calculateMonthlySalary(currentYear.value, currentMonth.value)
            } catch (e: Exception) {
                if (_breakdown.value?.total == -1.0) _breakdown.value = null
                _error.value = "加载失败：${e.message}"
            }
        }
    }

    class Factory(private val repo: AppRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SalaryViewModel(repo) as T
    }
}
