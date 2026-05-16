package com.rotask.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rotask.data.Group
import com.rotask.data.Task
import com.rotask.domain.GroupStatus
import com.rotask.domain.RotaskRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val groups: List<GroupStatus> = emptyList(),
    val addingTaskFor: Group? = null,
    val editingTask: Task? = null,
    val deletingTask: Task? = null,
    val showAddGroup: Boolean = false,
    val editingGroup: Group? = null,
    val deletingGroup: Group? = null,
)

class HomeViewModel(private val repo: RotaskRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _navToWork = Channel<Long>(Channel.BUFFERED)
    val navToWork = _navToWork.receiveAsFlow()

    init {
        viewModelScope.launch {
            repo.bootstrap()
            combine(
                repo.observeGroups(),
                repo.observeTasks(),
                repo.observeWorkSessionsTick(),
            ) { _, _, _ -> Unit }.collect {
                val groups = repo.groupStatuses()
                _uiState.update { it.copy(groups = groups) }
            }
        }
    }

    // ---- Dialog state ----

    fun showAddGroupDialog() = _uiState.update { it.copy(showAddGroup = true) }
    fun startEditingGroup(group: Group) = _uiState.update { it.copy(editingGroup = group) }
    fun startDeletingGroup(group: Group) = _uiState.update { it.copy(deletingGroup = group) }

    fun showAddTaskFor(group: Group) = _uiState.update { it.copy(addingTaskFor = group) }
    fun startEditingTask(task: Task) = _uiState.update { it.copy(editingTask = task) }
    fun startDeletingTask(task: Task) = _uiState.update { it.copy(deletingTask = task) }

    fun dismissDialogs() = _uiState.update {
        it.copy(
            showAddGroup = false,
            editingGroup = null,
            deletingGroup = null,
            addingTaskFor = null,
            editingTask = null,
            deletingTask = null,
        )
    }

    // ---- Group actions ----

    fun addGroup(name: String, dailyMinutes: Int) {
        viewModelScope.launch {
            repo.addGroup(name, dailyMinutes)
            dismissDialogs()
        }
    }

    fun updateGroup(original: Group, name: String, dailyMinutes: Int) {
        viewModelScope.launch {
            repo.updateGroup(
                original.copy(
                    name = name.trim(),
                    dailyMinutes = dailyMinutes.coerceAtLeast(1),
                )
            )
            dismissDialogs()
        }
    }

    fun deleteGroup(group: Group) {
        viewModelScope.launch {
            repo.deleteGroup(group)
            dismissDialogs()
        }
    }

    // ---- Task actions ----

    fun addTask(groupId: Long, name: String, description: String, weight: Double, enabled: Boolean) {
        viewModelScope.launch {
            repo.addTask(
                groupId = groupId,
                name = name.trim(),
                description = description.trim(),
                weight = sanitizeWeight(weight),
                enabled = enabled,
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
                    enabled = enabled,
                )
            )
            dismissDialogs()
        }
    }

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

    // ---- Work ----

    fun startWorkInGroup(groupId: Long) {
        viewModelScope.launch {
            val taskId = repo.pickNextInGroup(groupId)?.task?.id ?: return@launch
            _navToWork.send(taskId)
        }
    }

    private fun sanitizeWeight(value: Double): Double = if (value > 0.0) value else 1.0

    companion object {
        fun factory(repo: RotaskRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(repo) }
        }
    }
}
