package com.sora.dopwatch.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sora.dopwatch.data.Settings
import com.sora.dopwatch.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { s ->
                _settings.value = s
            }
        }
    }

    fun saveLineConfig(token: String, groupId: String) {
        viewModelScope.launch {
            settingsRepository.updateLineConfig(token, groupId)
            _saveMessage.value = "LINE設定を保存しました"
        }
    }

    fun saveBeeminderConfig(user: String, token: String, goal: String) {
        viewModelScope.launch {
            settingsRepository.updateBeeminderConfig(user, token, goal)
            _saveMessage.value = "Beeminder設定を保存しました"
        }
    }

    fun saveThresholds(totalHours: Float, snsHours: Float, videoHours: Float) {
        viewModelScope.launch {
            settingsRepository.updateThresholds(
                totalMs = (totalHours * 60 * 60 * 1000).toLong(),
                snsMs = (snsHours * 60 * 60 * 1000).toLong(),
                videoMs = (videoHours * 60 * 60 * 1000).toLong()
            )
            _saveMessage.value = "制限時間を保存しました"
        }
    }

    fun saveDriveFileId(fileId: String) {
        viewModelScope.launch {
            settingsRepository.updateDriveFileId(fileId.trim())
            _saveMessage.value = "Drive設定を保存しました"
        }
    }

    fun clearMessage() {
        _saveMessage.value = null
    }
}
