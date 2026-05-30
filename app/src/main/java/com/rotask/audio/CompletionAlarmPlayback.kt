package com.rotask.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.PowerManager

object CompletionAlarmPlayback {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentPlayer: MediaPlayer? = null
    private var currentWakeLock: PowerManager.WakeLock? = null
    private var currentPendingResult: BroadcastReceiver.PendingResult? = null
    private var stopRunnable: Runnable? = null

    @Synchronized
    fun play(context: Context, pendingResult: BroadcastReceiver.PendingResult) {
        stopLocked()
        currentPendingResult = pendingResult

        val appContext = context.applicationContext
        val sound = SoundSettings.readCompletionSound(appContext)
        val uri = sound.uri
        if (uri == null) {
            finishLocked()
            return
        }

        try {
            currentWakeLock = appContext
                .getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
                .apply {
                    setReferenceCounted(false)
                    acquire(MAX_PLAYBACK_MILLIS + WAKE_LOCK_GRACE_MILLIS)
                }

            val player = MediaPlayer().apply {
                setWakeMode(appContext, PowerManager.PARTIAL_WAKE_LOCK)
                setDataSource(appContext, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(sound.audioUsage)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = false
                setOnCompletionListener { releaseIfCurrent(it) }
                setOnErrorListener { mp, _, _ ->
                    releaseIfCurrent(mp)
                    true
                }
                prepare()
            }
            currentPlayer = player
            player.start()
            scheduleFallbackStop(player)
        } catch (_: Throwable) {
            stopLocked()
        }
    }

    @Synchronized
    private fun releaseIfCurrent(player: MediaPlayer) {
        if (currentPlayer != player) return
        stopLocked()
    }

    private fun scheduleFallbackStop(player: MediaPlayer) {
        stopRunnable = Runnable { releaseIfCurrent(player) }.also {
            mainHandler.postDelayed(it, MAX_PLAYBACK_MILLIS)
        }
    }

    private fun stopLocked() {
        stopRunnable?.let { mainHandler.removeCallbacks(it) }
        stopRunnable = null
        currentPlayer?.let { player ->
            runCatching {
                if (player.isPlaying) player.stop()
            }
            player.release()
        }
        currentPlayer = null
        finishLocked()
    }

    private fun finishLocked() {
        currentWakeLock?.let { wakeLock ->
            if (wakeLock.isHeld) wakeLock.release()
        }
        currentWakeLock = null
        currentPendingResult?.finish()
        currentPendingResult = null
    }

    private const val MAX_PLAYBACK_MILLIS = 5_000L
    private const val WAKE_LOCK_GRACE_MILLIS = 1_000L
    private const val WAKE_LOCK_TAG = "Rotask:CompletionAlarm"
}
