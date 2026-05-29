package com.rotask.audio

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SoundSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _completionSound = MutableStateFlow(readCompletionSound(prefs))
    val completionSound: StateFlow<CompletionSound> = _completionSound.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_COMPLETION_SOUND) {
            _completionSound.value = readCompletionSound(prefs)
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun setCompletionSound(sound: CompletionSound) {
        prefs.edit().putString(KEY_COMPLETION_SOUND, sound.preferenceValue).apply()
        _completionSound.value = sound
    }

    companion object {
        private const val PREFS_NAME = "rotask_preferences"
        private const val KEY_COMPLETION_SOUND = "completion_sound"

        fun readCompletionSound(context: Context): CompletionSound =
            readCompletionSound(
                context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            )

        private fun readCompletionSound(prefs: SharedPreferences): CompletionSound =
            CompletionSound.fromPreferenceValue(prefs.getString(KEY_COMPLETION_SOUND, null))
    }
}
