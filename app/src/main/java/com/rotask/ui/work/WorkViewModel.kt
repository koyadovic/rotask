package com.rotask.ui.work

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rotask.domain.RotaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

data class WorkUiState(
    val loading: Boolean = true,
    val taskName: String = "",
    val taskDescription: String = "",
    val sessionTargetSeconds: Long = 0L,
    val sessionElapsedSeconds: Long = 0L,
    val finished: Boolean = false,
    val noWorkNeeded: Boolean = false,
)

class WorkViewModel(
    private val repo: RotaskRepository,
    private val appScope: CoroutineScope,
    private val taskId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(WorkUiState())
    val state: StateFlow<WorkUiState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private val persisted = AtomicBoolean(false)

    init {
        viewModelScope.launch {
            val status = repo.statusForTask(taskId)
            if (status == null || status.remainingSecondsToday <= 0) {
                _state.value = WorkUiState(loading = false, noWorkNeeded = true, finished = true)
                return@launch
            }
            _state.value = WorkUiState(
                loading = false,
                taskName = status.task.name,
                taskDescription = status.task.description,
                sessionTargetSeconds = status.remainingSecondsToday,
                sessionElapsedSeconds = 0L,
                finished = false,
            )
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _state.value
                if (current.finished) break
                val nextElapsed = current.sessionElapsedSeconds + 1
                _state.value = current.copy(sessionElapsedSeconds = nextElapsed)
                if (nextElapsed >= current.sessionTargetSeconds) {
                    stop()
                    break
                }
            }
        }
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
        appScope.launch {
            repo.recordWork(taskId, elapsed)
        }
    }

    companion object {
        fun factory(
            repo: RotaskRepository,
            appScope: CoroutineScope,
            taskId: Long
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { WorkViewModel(repo, appScope, taskId) }
        }
    }
}
