package com.rotask.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
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
        prefs.edit()
            .putString(KEY_COMPLETION_SOUND_URI, sound.uriString ?: OFF_VALUE)
            .putInt(KEY_COMPLETION_SOUND_USAGE, sound.audioUsage)
            .remove(KEY_COMPLETION_SOUND)
            .apply()
        _completionSound.value = sound
    }

    companion object {
        private const val PREFS_NAME = "rotask_preferences"
        private const val KEY_COMPLETION_SOUND = "completion_sound"
        private const val KEY_COMPLETION_SOUND_URI = "completion_sound_uri"
        private const val KEY_COMPLETION_SOUND_USAGE = "completion_sound_usage"
        private const val OFF_VALUE = "__off__"

        fun readCompletionSound(context: Context): CompletionSound =
            readCompletionSound(
                context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            )

        private fun readCompletionSound(prefs: SharedPreferences): CompletionSound {
            if (prefs.contains(KEY_COMPLETION_SOUND_URI)) {
                val uriString = prefs.getString(KEY_COMPLETION_SOUND_URI, null)
                return CompletionSound(
                    uriString = uriString?.takeUnless { it == OFF_VALUE },
                    audioUsage = prefs.getInt(KEY_COMPLETION_SOUND_USAGE, AudioAttributes.USAGE_NOTIFICATION),
                )
            }

            return when (prefs.getString(KEY_COMPLETION_SOUND, null)) {
                "off" -> CompletionSound.OFF
                "alarm" -> CompletionSound.defaultAlarm()
                "ringtone" -> CompletionSound.defaultRingtone()
                else -> CompletionSound.defaultNotification()
            }
        }
    }
}
