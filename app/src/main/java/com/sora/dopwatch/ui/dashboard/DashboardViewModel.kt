package com.sora.dopwatch.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sora.dopwatch.data.AppUsageEntity
import com.sora.dopwatch.data.SettingsRepository
import com.sora.dopwatch.data.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val usages: List<AppUsageEntity> = emptyList(),
    val totalMs: Long = 0L,
    val totalLimitMs: Long = 3 * 60 * 60 * 1000L,
    val isLoading: Boolean = true,
    val hasPermission: Boolean = false,
    val isBatteryOptimized: Boolean = true // true = まだ除外されていない
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val usageRepository: UsageRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null

    fun setPermissionGranted(granted: Boolean) {
        val prev = _uiState.value.hasPermission
        _uiState.value = _uiState.value.copy(hasPermission = granted)
        if (granted && !prev) refreshUsage()
    }

    fun setBatteryOptimized(optimized: Boolean) {
        _uiState.value = _uiState.value.copy(isBatteryOptimized = optimized)
    }

    fun refreshUsage() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                usageRepository.refreshAndSave()
                val settings = settingsRepository.getSettings()
                val usages = usageRepository.getTodayUsageFlow().first()
                val totalMs = usages.sumOf { it.usageTimeMs }
                _uiState.value = _uiState.value.copy(
                    usages = usages,
                    totalMs = totalMs,
                    totalLimitMs = settings.totalLimitMs,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun formatDuration(ms: Long): String {
        val hours = ms / (1000 * 60 * 60)
        val minutes = (ms % (1000 * 60 * 60)) / (1000 * 60)
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}
