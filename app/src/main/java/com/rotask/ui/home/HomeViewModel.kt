package com.rotask.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rotask.data.Task
import com.rotask.domain.RotaskRepository
import com.rotask.domain.TaskStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val dailyMinutes: Int = 60,
    val statuses: List<TaskStatus> = emptyList(),
    val showAdd: Boolean = false,
    val editing: Task? = null,
    val deleting: Task? = null,
    val showConfig: Boolean = false,
) {
    val hasWorkRemaining: Boolean
        get() = statuses.any { it.task.enabled && it.remainingSecondsToday > 0 }
}

class HomeViewModel(private val repo: RotaskRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _navToWork = Channel<Long>(Channel.BUFFERED)
    val navToWork = _navToWork.receiveAsFlow()

    init {
        viewModelScope.launch {
            repo.bootstrap()
            combine(
                repo.observeTasks(),
                repo.observeSettings(),
                repo.observeWorkSessionsTick(),
            ) { _, settings, _ -> settings }.collect { settings ->
                val status = repo.status()
                _uiState.update {
                    it.copy(
                        dailyMinutes = settings?.dailyMinutes ?: 60,
                        statuses = status
                    )
                }
            }
        }
    }

    fun showAddDialog() = _uiState.update { it.copy(showAdd = true) }
    fun showConfigDialog() = _uiState.update { it.copy(showConfig = true) }
    fun startEditing(task: Task) = _uiState.update { it.copy(editing = task) }
    fun startDeleting(task: Task) = _uiState.update { it.copy(deleting = task) }
    fun dismissDialogs() = _uiState.update {
        it.copy(showAdd = false, editing = null, deleting = null, showConfig = false)
    }

    fun addTask(name: String, description: String, weight: Double, enabled: Boolean) {
        viewModelScope.launch {
            repo.addTask(
                name = name.trim(),
                description = description.trim(),
                weight = sanitizeWeight(weight),
                enabled = enabled
            )
            dismissDialogs()
        }
    }

    fun updateTask(original: Task, name: String, description: String, weight: Double, enabled: Boolean) {
        viewModelScope.launch {
            repo.updateTask(
                original.copy(
                    name = name.trim(),
                    description = description.trim(),
                    weight = sanitizeWeight(weight),
                    enabled = enabled
                )
            )
            dismissDialogs()
        }
    }

    private fun sanitizeWeight(value: Double): Double = if (value > 0.0) value else 1.0

    fun toggleEnabled(task: Task) {
        viewModelScope.launch {
            repo.setEnabled(task, !task.enabled)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repo.deleteTask(task)
            dismissDialogs()
        }
    }

    fun setDailyMinutes(minutes: Int) {
        viewModelScope.launch {
            repo.setDailyMinutes(minutes.coerceAtLeast(1))
            dismissDialogs()
        }
    }

    fun startWork() {
        viewModelScope.launch {
            val taskId = repo.pickNext()?.task?.id ?: return@launch
            _navToWork.send(taskId)
        }
    }

    companion object {
        fun factory(repo: RotaskRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(repo) }
        }
    }
}
