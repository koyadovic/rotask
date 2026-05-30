package com.rotask.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper

class SoundPlayer(
    private val context: Context,
    private val settings: SoundSettings? = null,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentPlayer: MediaPlayer? = null
    private var stopRunnable: Runnable? = null

    fun playTaskCompleted() {
        playCompletionSound(settings?.completionSound?.value ?: SoundSettings.readCompletionSound(context))
    }

    @Synchronized
    fun playCompletionSound(sound: CompletionSound) {
        val uri = sound.uri ?: return stopCurrentLocked()
        stopCurrentLocked()
        try {
            playUriOnce(uri, sound.audioUsage)
        } catch (_: Throwable) {
            stopCurrentLocked()
            // best effort: a missing or silenced default sound is not an error worth surfacing.
        }
    }

    private fun playUriOnce(uri: Uri, audioUsage: Int) {
        stopCurrentLocked()
        var player: MediaPlayer? = null
        try {
            player = MediaPlayer().apply {
                setDataSource(context.applicationContext, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(audioUsage)
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
            player?.release()
            if (currentPlayer == player) currentPlayer = null
        }
    }

    private fun scheduleFallbackStop(player: MediaPlayer) {
        stopRunnable = Runnable { releaseIfCurrent(player) }.also {
            mainHandler.postDelayed(it, MAX_PLAYBACK_MILLIS)
        }
    }

    @Synchronized
    private fun releaseIfCurrent(player: MediaPlayer) {
        if (currentPlayer != player) return
        stopCurrentLocked()
    }

    private fun stopCurrentLocked() {
        stopRunnable?.let { mainHandler.removeCallbacks(it) }
        stopRunnable = null
        currentPlayer?.let { player ->
            runCatching {
                if (player.isPlaying) player.stop()
            }
            player.release()
        }
        currentPlayer = null
    }

    companion object {
        private const val MAX_PLAYBACK_MILLIS = 5_000L
    }
}
