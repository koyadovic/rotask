package com.rotask.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CompletionAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != CompletionAlarmScheduler.ACTION_COMPLETION_ALARM) return
        CompletionAlarmPlayback.play(context.applicationContext, goAsync())
    }
}
