package com.rotask.audio

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.rotask.MainActivity

class CompletionAlarmScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun schedule(delaySeconds: Long): Boolean {
        if (delaySeconds <= 0) return false
        cancel()
        return try {
            val triggerAtMillis = System.currentTimeMillis() + delaySeconds * 1000L
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent()),
                alarmIntent(),
            )
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun cancel() {
        alarmManager.cancel(alarmIntent())
    }

    private fun alarmIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE,
            Intent(appContext, CompletionAlarmReceiver::class.java).setAction(ACTION_COMPLETION_ALARM),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun showIntent(): PendingIntent =
        PendingIntent.getActivity(
            appContext,
            REQUEST_CODE,
            Intent(appContext, MainActivity::class.java)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val ACTION_COMPLETION_ALARM = "com.rotask.action.COMPLETION_ALARM"
        private const val REQUEST_CODE = 2037
    }
}
