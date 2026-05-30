package com.rotask.ui.settings

import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rotask.audio.CompletionSound
import com.rotask.audio.SoundPlayer
import com.rotask.audio.SoundSettings
import com.rotask.domain.RotaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val completionSound: CompletionSound = CompletionSound.defaultNotification(),
    val completionSoundOptions: List<CompletionSoundOption> = emptyList(),
    val busy: Boolean = false,
    val status: SettingsStatus? = null,
    val statusId: Int = 0,
)

data class CompletionSoundOption(
    val title: String,
    val sound: CompletionSound,
)

enum class SettingsStatus {
    EXPORT_SUCCESS,
    EXPORT_ERROR,
    IMPORT_SUCCESS,
    IMPORT_ERROR,
}

class SettingsViewModel(
    private val repo: RotaskRepository,
    private val soundSettings: SoundSettings,
    private val soundPlayer: SoundPlayer,
    context: Context,
) : ViewModel() {

    private val appContext = context.applicationContext
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val options = withContext(Dispatchers.IO) { loadCompletionSoundOptions() }
            _uiState.update { it.copy(completionSoundOptions = options) }
        }
        viewModelScope.launch {
            soundSettings.completionSound.collect { completionSound ->
                _uiState.update { it.copy(completionSound = completionSound) }
            }
        }
    }

    fun setCompletionSound(sound: CompletionSound) {
        soundSettings.setCompletionSound(sound)
    }

    fun previewCompletionSound() {
        soundPlayer.playCompletionSound(_uiState.value.completionSound)
    }

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            val status = runCatching {
                withContext(Dispatchers.IO) {
                    val json = repo.exportBackupJson()
                    appContext.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)
                        ?.use { it.write(json) }
                        ?: error("Could not open export destination")
                }
            }.fold(
                onSuccess = { SettingsStatus.EXPORT_SUCCESS },
                onFailure = { SettingsStatus.EXPORT_ERROR },
            )
            publishStatus(status)
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            val status = runCatching {
                withContext(Dispatchers.IO) {
                    val json = appContext.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                        ?: error("Could not open import source")
                    repo.importBackupJson(json)
                }
            }.fold(
                onSuccess = { SettingsStatus.IMPORT_SUCCESS },
                onFailure = { SettingsStatus.IMPORT_ERROR },
            )
            publishStatus(status)
        }
    }

    private fun publishStatus(status: SettingsStatus) {
        _uiState.update {
            it.copy(
                busy = false,
                status = status,
                statusId = it.statusId + 1,
            )
        }
    }

    private fun loadCompletionSoundOptions(): List<CompletionSoundOption> {
        val options = mutableListOf<CompletionSoundOption>()
        options += CompletionSoundOption(
            title = appContext.getString(com.rotask.R.string.sound_off),
            sound = CompletionSound.OFF,
        )
        options += CompletionSoundOption(
            title = appContext.getString(com.rotask.R.string.sound_default_notification),
            sound = CompletionSound.defaultNotification(),
        )
        options += CompletionSoundOption(
            title = appContext.getString(com.rotask.R.string.sound_default_alarm),
            sound = CompletionSound.defaultAlarm(),
        )
        options += CompletionSoundOption(
            title = appContext.getString(com.rotask.R.string.sound_default_ringtone),
            sound = CompletionSound.defaultRingtone(),
        )
        options += loadRingtones(
            type = RingtoneManager.TYPE_NOTIFICATION,
            audioUsage = AudioAttributes.USAGE_NOTIFICATION,
            category = appContext.getString(com.rotask.R.string.sound_category_notification),
        )
        options += loadRingtones(
            type = RingtoneManager.TYPE_ALARM,
            audioUsage = AudioAttributes.USAGE_ALARM,
            category = appContext.getString(com.rotask.R.string.sound_category_alarm),
        )
        options += loadRingtones(
            type = RingtoneManager.TYPE_RINGTONE,
            audioUsage = AudioAttributes.USAGE_NOTIFICATION_RINGTONE,
            category = appContext.getString(com.rotask.R.string.sound_category_ringtone),
        )
        return options.distinctBy { "${it.sound.audioUsage}:${it.sound.uriString}" }
    }

    private fun loadRingtones(
        type: Int,
        audioUsage: Int,
        category: String,
    ): List<CompletionSoundOption> {
        val manager = RingtoneManager(appContext).apply {
            setType(type)
        }
        return runCatching {
            val cursor = manager.cursor ?: return emptyList()
            buildList {
                cursor.use {
                    for (position in 0 until it.count) {
                        val uri = manager.getRingtoneUri(position) ?: continue
                        val ringtone = manager.getRingtone(position)
                        val title = ringtone?.getTitle(appContext)?.takeIf { value -> value.isNotBlank() }
                            ?: uri.lastPathSegment
                            ?: uri.toString()
                        add(
                            CompletionSoundOption(
                                title = "$category: $title",
                                sound = CompletionSound(
                                    uriString = uri.toString(),
                                    audioUsage = audioUsage,
                                ),
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        fun factory(
            repo: RotaskRepository,
            soundSettings: SoundSettings,
            soundPlayer: SoundPlayer,
            context: Context,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(repo, soundSettings, soundPlayer, context) }
        }
    }
}
