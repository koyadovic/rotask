package com.rotask.ui.work

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rotask.audio.SoundPlayer
import com.rotask.domain.RotaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

data class WorkUiState(
    val loading: Boolean = true,
    val taskName: String = "",
    val taskDescription: String = "",
    val sessionTargetSeconds: Long = 0L,
    val sessionElapsedSeconds: Long = 0L,
    val paused: Boolean = false,
    val finished: Boolean = false,
    val noWorkNeeded: Boolean = false,
)

class WorkViewModel(
    private val repo: RotaskRepository,
    private val appScope: CoroutineScope,
    private val soundPlayer: SoundPlayer,
    initialTaskId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(WorkUiState())
    val state: StateFlow<WorkUiState> = _state.asStateFlow()

    private var currentTaskId: Long = initialTaskId
    private var currentGroupId: Long = 0L
    private var timerJob: Job? = null
    private val persisted = AtomicBoolean(false)

    init {
        viewModelScope.launch {
            loadTask(initialTaskId, resetPersistFlag = false)
        }
    }

    private suspend fun loadTask(taskId: Long, resetPersistFlag: Boolean) {
        val status = repo.statusForTask(taskId)
        if (status == null || !status.task.enabled || status.remainingSecondsToday <= 0) {
            _state.value = WorkUiState(loading = false, noWorkNeeded = true, finished = true)
            return
        }
        currentTaskId = taskId
        currentGroupId = status.task.groupId
        if (resetPersistFlag) persisted.set(false)
        _state.value = WorkUiState(
            loading = false,
            taskName = status.task.name,
            taskDescription = status.task.description,
            sessionTargetSeconds = status.remainingSecondsToday,
            sessionElapsedSeconds = 0L,
            paused = true,
            finished = false,
        )
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _state.value
                if (current.finished || current.paused) break
                val nextElapsed = current.sessionElapsedSeconds + 1
                _state.value = current.copy(sessionElapsedSeconds = nextElapsed)
                if (nextElapsed >= current.sessionTargetSeconds) {
                    advanceToNext(reason = AdvanceReason.AUTO_COMPLETED)
                    break
                }
            }
        }
    }

    private enum class AdvanceReason { AUTO_COMPLETED, SKIPPED }

    private fun advanceToNext(reason: AdvanceReason) {
        timerJob?.cancel()
        if (reason == AdvanceReason.AUTO_COMPLETED) {
            soundPlayer.playTaskCompleted()
        }
        val finishedTaskId = currentTaskId
        val groupId = currentGroupId
        val elapsed = _state.value.sessionElapsedSeconds
        viewModelScope.launch {
            if (elapsed > 0) {
                repo.recordWork(finishedTaskId, elapsed)
            }
            persisted.set(true)
            val next = repo.pickNextInGroupExcluding(groupId, finishedTaskId)
            if (next == null) {
                _state.update { it.copy(finished = true) }
                return@launch
            }
            loadTask(next.task.id, resetPersistFlag = true)
        }
    }

    fun togglePause() {
        val current = _state.value
        if (current.finished || current.loading) return
        if (current.paused) {
            _state.value = current.copy(paused = false)
            startTimer()
        } else {
            timerJob?.cancel()
            _state.value = current.copy(paused = true)
        }
    }

    fun skip() {
        val current = _state.value
        if (current.finished || current.loading) return
        advanceToNext(reason = AdvanceReason.SKIPPED)
    }

    fun stop() {
        timerJob?.cancel()
        val elapsed = _state.value.sessionElapsedSeconds
        persistOnce(elapsed)
        _state.value = _state.value.copy(finished = true)
    }

    override fun onCleared() {
        super.onCleared()
        val elapsed = _state.value.sessionElapsedSeconds
        persistOnce(elapsed)
    }

    private fun persistOnce(elapsed: Long) {
        if (elapsed <= 0) return
        if (!persisted.compareAndSet(false, true)) return
        val taskId = currentTaskId
        appScope.launch {
            repo.recordWork(taskId, elapsed)
        }
    }

    companion object {
        fun factory(
            repo: RotaskRepository,
            appScope: CoroutineScope,
            soundPlayer: SoundPlayer,
            taskId: Long,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { WorkViewModel(repo, appScope, soundPlayer, taskId) }
        }
    }
}
