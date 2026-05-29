package com.rotask

import android.app.Application
import com.rotask.audio.CompletionAlarmScheduler
import com.rotask.audio.SoundPlayer
import com.rotask.audio.SoundSettings
import com.rotask.data.AppDatabase
import com.rotask.domain.RotaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class RotaskApplication : Application() {

    lateinit var repository: RotaskRepository
        private set

    lateinit var soundPlayer: SoundPlayer
        private set

    lateinit var soundSettings: SoundSettings
        private set

    lateinit var completionAlarmScheduler: CompletionAlarmScheduler
        private set

    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.get(this)
        soundSettings = SoundSettings(this)
        completionAlarmScheduler = CompletionAlarmScheduler(this)
        repository = RotaskRepository(db)
        soundPlayer = SoundPlayer(this, soundSettings)
    }
}
