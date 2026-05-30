package com.rotask.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class CompletionAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != CompletionAlarmScheduler.ACTION_COMPLETION_ALARM) return
        Log.i("RotaskAlarm", "alarm clock fallback received")
        CompletionAlarmPlayback.play(context.applicationContext, goAsync())
    }
}
