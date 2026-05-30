package com.rotask.audio

import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri

data class CompletionSound(
    val uriString: String?,
    val audioUsage: Int = AudioAttributes.USAGE_NOTIFICATION,
) {
    val uri: Uri?
        get() = uriString?.let(Uri::parse)

    val isSilent: Boolean
        get() = uriString == null

    companion object {
        val OFF = CompletionSound(uriString = null)

        fun defaultNotification(): CompletionSound =
            defaultForType(RingtoneManager.TYPE_NOTIFICATION, AudioAttributes.USAGE_NOTIFICATION)

        fun defaultAlarm(): CompletionSound =
            defaultForType(RingtoneManager.TYPE_ALARM, AudioAttributes.USAGE_ALARM)

        fun defaultRingtone(): CompletionSound =
            defaultForType(RingtoneManager.TYPE_RINGTONE, AudioAttributes.USAGE_NOTIFICATION_RINGTONE)

        private fun defaultForType(ringtoneType: Int, audioUsage: Int): CompletionSound =
            CompletionSound(
                uriString = RingtoneManager.getDefaultUri(ringtoneType)?.toString(),
                audioUsage = audioUsage,
            )
    }
}
