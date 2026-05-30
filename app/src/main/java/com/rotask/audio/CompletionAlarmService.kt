package com.rotask.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import com.rotask.MainActivity
import com.rotask.R

class CompletionAlarmService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var fireRunnable: Runnable? = null
    private var timerWakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_SCHEDULE -> {
                val delaySeconds = intent.getLongExtra(EXTRA_DELAY_SECONDS, 0L)
                if (delaySeconds <= 0L) {
                    stopAlarm()
                    START_NOT_STICKY
                } else {
                    val targetElapsedMillis = SystemClock.elapsedRealtime() + delaySeconds * 1000L
                    storeTarget(targetElapsedMillis)
                    startTimerForeground()
                    armTimer(targetElapsedMillis)
                    Log.i(TAG, "service scheduled delaySeconds=$delaySeconds targetElapsed=$targetElapsedMillis")
                    START_STICKY
                }
            }

            ACTION_CANCEL -> {
                Log.i(TAG, "service cancelled")
                stopAlarm()
                START_NOT_STICKY
            }

            else -> {
                val targetElapsedMillis = readStoredTarget()
                if (targetElapsedMillis == null) {
                    stopAlarm()
                    START_NOT_STICKY
                } else {
                    startTimerForeground()
                    armTimer(targetElapsedMillis)
                    Log.i(TAG, "service restored targetElapsed=$targetElapsedMillis")
                    START_STICKY
                }
            }
        }
    }

    override fun onDestroy() {
        stopTimer()
        super.onDestroy()
    }

    private fun armTimer(targetElapsedMillis: Long) {
        stopTimer()
        val delayMillis = (targetElapsedMillis - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        acquireTimerWakeLock(delayMillis + TIMER_WAKE_LOCK_GRACE_MILLIS)
        fireRunnable = Runnable { fireAlarm() }.also { handler.postDelayed(it, delayMillis) }
    }

    private fun fireAlarm() {
        Log.i(TAG, "service firing")
        clearStoredTarget()
        stopTimer()
        CompletionAlarmPlayback.play(applicationContext) {
            Log.i(TAG, "service playback finished")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun stopAlarm() {
        clearStoredTarget()
        stopTimer()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopTimer() {
        fireRunnable?.let { handler.removeCallbacks(it) }
        fireRunnable = null
        timerWakeLock?.let { wakeLock ->
            if (wakeLock.isHeld) wakeLock.release()
        }
        timerWakeLock = null
    }

    private fun acquireTimerWakeLock(timeoutMillis: Long) {
        timerWakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TIMER_WAKE_LOCK_TAG)
            .apply {
                setReferenceCounted(false)
                acquire(timeoutMillis.coerceAtLeast(TIMER_WAKE_LOCK_GRACE_MILLIS))
            }
    }

    private fun startTimerForeground() {
        createNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            REQUEST_CODE_OPEN_APP,
            Intent(this, MainActivity::class.java)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.active_timer_notification_title))
            .setContentText(getString(R.string.active_timer_notification_text))
            .setContentIntent(pendingIntent)
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .setShowWhen(false)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.active_timer_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun storeTarget(targetElapsedMillis: Long) {
        prefs()
            .edit()
            .putLong(KEY_TARGET_ELAPSED_MILLIS, targetElapsedMillis)
            .apply()
    }

    private fun readStoredTarget(): Long? {
        val target = prefs().getLong(KEY_TARGET_ELAPSED_MILLIS, 0L)
        return target.takeIf { it > 0L }
    }

    private fun clearStoredTarget() {
        prefs().edit().remove(KEY_TARGET_ELAPSED_MILLIS).apply()
    }

    private fun prefs() = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "RotaskAlarm"
        private const val CHANNEL_ID = "rotask_active_timer"
        private const val NOTIFICATION_ID = 2401
        private const val REQUEST_CODE_OPEN_APP = 2402
        private const val PREFS_NAME = "completion_alarm_service"
        private const val KEY_TARGET_ELAPSED_MILLIS = "target_elapsed_millis"
        private const val TIMER_WAKE_LOCK_GRACE_MILLIS = 10_000L
        private const val TIMER_WAKE_LOCK_TAG = "Rotask:ActiveTimer"

        private const val ACTION_SCHEDULE = "com.rotask.action.SCHEDULE_COMPLETION_SERVICE"
        private const val ACTION_CANCEL = "com.rotask.action.CANCEL_COMPLETION_SERVICE"
        private const val EXTRA_DELAY_SECONDS = "delay_seconds"

        fun schedule(context: Context, delaySeconds: Long) {
            val appContext = context.applicationContext
            val intent = Intent(appContext, CompletionAlarmService::class.java)
                .setAction(ACTION_SCHEDULE)
                .putExtra(EXTRA_DELAY_SECONDS, delaySeconds)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(intent)
            } else {
                appContext.startService(intent)
            }
        }

        fun cancel(context: Context) {
            val appContext = context.applicationContext
            appContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_TARGET_ELAPSED_MILLIS)
                .apply()
            appContext.stopService(Intent(appContext, CompletionAlarmService::class.java))
        }
    }
}
