package com.rotask.audio

import android.content.Context
import android.media.RingtoneManager

class SoundPlayer(private val context: Context) {

    fun playTaskCompleted() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) ?: return
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri) ?: return
            ringtone.play()
        } catch (_: Throwable) {
            // best effort: a missing or silenced default sound is not an error worth surfacing.
        }
    }
}
