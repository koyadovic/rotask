package com.rotask.audio

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.rotask.MainActivity

class CompletionAlarmScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun schedule(delaySeconds: Long): Boolean {
        if (delaySeconds <= 0) return false
        alarmManager.cancel(alarmIntent())
        return try {
            CompletionAlarmService.schedule(appContext, delaySeconds)
            Log.i(TAG, "foreground service alarm scheduled delaySeconds=$delaySeconds")
            true
        } catch (error: Throwable) {
            Log.w(TAG, "foreground service schedule failed; using alarm clock fallback", error)
            scheduleAlarmClock(delaySeconds)
        }
    }

    fun cancel() {
        runCatching { CompletionAlarmService.cancel(appContext) }
        alarmManager.cancel(alarmIntent())
    }

    private fun scheduleAlarmClock(delaySeconds: Long): Boolean {
        return try {
            val triggerAtMillis = System.currentTimeMillis() + delaySeconds * 1000L
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent()),
                alarmIntent(),
            )
            true
        } catch (error: Throwable) {
            Log.w(TAG, "alarm clock fallback failed", error)
            false
        }
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
        private const val TAG = "RotaskAlarm"
        private const val REQUEST_CODE = 2037
    }
}
