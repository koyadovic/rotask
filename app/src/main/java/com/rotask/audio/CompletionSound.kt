package com.rotask.audio

import android.media.RingtoneManager

enum class CompletionSound(
    val preferenceValue: String,
    val ringtoneType: Int?,
) {
    OFF("off", null),
    NOTIFICATION("notification", RingtoneManager.TYPE_NOTIFICATION),
    ALARM("alarm", RingtoneManager.TYPE_ALARM),
    RINGTONE("ringtone", RingtoneManager.TYPE_RINGTONE);

    companion object {
        val DEFAULT = NOTIFICATION

        fun fromPreferenceValue(value: String?): CompletionSound =
            entries.firstOrNull { it.preferenceValue == value } ?: DEFAULT
    }
}
