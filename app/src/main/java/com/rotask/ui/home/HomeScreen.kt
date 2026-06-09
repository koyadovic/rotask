package com.rotask.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotask.R
import com.rotask.data.Group
import com.rotask.data.Task
import com.rotask.domain.GroupStatus
import com.rotask.domain.TaskStatus
import com.rotask.ui.format.formatClock
import com.rotask.ui.format.formatWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onStartWork: (WorkStart) -> Unit,
    onOpenGroup: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by vm.uiState.collectAsState()

    LaunchedEffect(Unit) {
        vm.navToWork.collect { onStartWork(it) }
    }

    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.groups.isEmpty()) {
                EmptyState(onAddGroup = { vm.showAddGroupDialog() })
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    state.groups.forEach { groupStatus ->
                        item(key = "header-${groupStatus.group.id}") {
                            GroupHeader(
                                status = groupStatus,
                                onOpen = { onOpenGroup(groupStatus.group.id) },
                                onEdit = { vm.startEditingGroup(groupStatus.group) },
                                onDelete = { vm.startDeletingGroup(groupStatus.group) },
                            )
                        }
                        item(key = "actions-${groupStatus.group.id}") {
                            Spacer(Modifier.height(10.dp))
                            GroupActions(
                                canStartWork = groupStatus.hasWorkRemaining,
                                onStartWork = { vm.startWorkInGroup(groupStatus.group.id) },
                            )
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                    item(key = "add-group") {
                        AddGroupButton(onClick = { vm.showAddGroupDialog() })
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // Dialogs

    if (state.showAddGroup) {
        GroupEditDialog(
            title = stringResource(R.string.add_group),
            initialName = "",
            initialDailyMinutes = 60,
            initialTimed = true,
            onSave = { name, minutes, timed -> vm.addGroup(name, minutes, timed) },
            onCancel = { vm.dismissDialogs() },
        )
    }

    state.editingGroup?.let { group ->
        GroupEditDialog(
            title = stringResource(R.string.edit_group),
            initialName = group.name,
            initialDailyMinutes = group.dailyMinutes,
            initialTimed = group.timed,
            onSave = { name, minutes, timed -> vm.updateGroup(group, name, minutes, timed) },
            onCancel = { vm.dismissDialogs() },
        )
    }

    state.deletingGroup?.let { group ->
        val statuses = state.groups.firstOrNull { it.group.id == group.id }?.statuses.orEmpty()
        AlertDialog(
            onDismissRequest = { vm.dismissDialogs() },
            title = { Text(stringResource(R.string.delete_group)) },
            text = {
                Text(
                    if (statuses.isEmpty()) {
                        stringResource(R.string.delete_group_confirm_empty, group.name)
                    } else {
                        stringResource(R.string.delete_group_confirm_with_tasks, group.name, statuses.size)
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.deleteGroup(group) }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.dismissDialogs() }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupTasksScreen(
    vm: HomeViewModel,
    groupId: Long,
    onStartWork: (WorkStart) -> Unit,
    onBack: () -> Unit,
) {
    val state by vm.uiState.collectAsState()
    val groupStatus = state.groups.firstOrNull { it.group.id == groupId }

    LaunchedEffect(Unit) {
        vm.navToWork.collect { onStartWork(it) }
    }

    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(groupStatus?.group?.name ?: stringResource(R.string.home_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            if (groupStatus == null) {
                item {
                    Text(
                        text = stringResource(R.string.group_empty),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontStyle = FontStyle.Italic,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    )
                }
            } else {
                val enabledStatuses = groupStatus.statuses
                    .filter { it.task.enabled }
                    .sortedWith(
                        compareByDescending<TaskStatus> { it.scheduledToday }
                            .thenBy {
                                if (!groupStatus.group.timed && it.scheduledToday && it.completedToday) 1 else 0
                            }
                            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.task.name }
                    )
                val disabledStatuses = groupStatus.statuses
                    .filter { !it.task.enabled }
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.task.name })
                val showDisabled = groupStatus.group.id in state.expandedDisabledGroupIds
                val visibleStatuses = if (showDisabled) {
                    enabledStatuses + disabledStatuses
                } else {
                    enabledStatuses
                }
                items(
                    visibleStatuses,
                    key = { "task-${it.task.id}" },
                ) { taskStatus ->
                    Spacer(Modifier.height(8.dp))
                    TaskRow(
                        status = taskStatus,
                        timedGroup = groupStatus.group.timed,
                        onToggleEnabled = { vm.toggleEnabled(taskStatus.task) },
                        onStartTaskAlone = { vm.startTaskAlone(taskStatus.task) },
                        onMarkDone = { vm.markTaskDone(taskStatus.task) },
                        onEdit = { vm.startEditingTask(taskStatus.task) },
                        onDelete = { vm.startDeletingTask(taskStatus.task) },
                    )
                }
                if (groupStatus.statuses.isEmpty()) {
                    item(key = "empty-${groupStatus.group.id}") {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.group_empty),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            fontStyle = FontStyle.Italic,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                        )
                    }
                }
                if (disabledStatuses.isNotEmpty()) {
                    item(key = "disabled-toggle-${groupStatus.group.id}") {
                        Spacer(Modifier.height(8.dp))
                        DisabledTasksToggle(
                            count = disabledStatuses.size,
                            expanded = showDisabled,
                            onClick = { vm.toggleDisabledTasksVisible(groupStatus.group.id) },
                        )
                    }
                }
                item(key = "actions-${groupStatus.group.id}") {
                    Spacer(Modifier.height(10.dp))
                    GroupActions(
                        canStartWork = groupStatus.hasWorkRemaining,
                        onAddTask = { vm.showAddTaskFor(groupStatus.group) },
                        onStartWork = { vm.startWorkInGroup(groupStatus.group.id) },
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    state.addingTaskFor?.let { group ->
        TaskEditDialog(
            title = stringResource(R.string.add_task_in, group.name),
            initialName = "",
            initialDescription = "",
            initialWeight = 1.0,
            initialEnabled = true,
            initialScheduledDays = Task.ALL_DAYS_MASK,
            timedGroup = group.timed,
            onSave = { name, description, weight, enabled, scheduledDays ->
                vm.addTask(group.id, name, description, weight, enabled, scheduledDays)
            },
            onCancel = { vm.dismissDialogs() },
        )
    }

    state.editingTask?.let { task ->
        val timedGroup = state.groups.firstOrNull { it.group.id == task.groupId }?.group?.timed ?: true
        TaskEditDialog(
            title = stringResource(R.string.edit_task),
            initialName = task.name,
            initialDescription = task.description,
            initialWeight = task.weight,
            initialEnabled = task.enabled,
            initialScheduledDays = task.scheduledDays,
            timedGroup = timedGroup,
            onSave = { name, description, weight, enabled, scheduledDays ->
                vm.updateTask(task, name, description, weight, enabled, scheduledDays)
            },
            onCancel = { vm.dismissDialogs() },
        )
    }

    state.deletingTask?.let { task ->
        AlertDialog(
            onDismissRequest = { vm.dismissDialogs() },
            title = { Text(stringResource(R.string.delete_task)) },
            text = { Text(task.name) },
            confirmButton = {
                TextButton(onClick = { vm.deleteTask(task) }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.dismissDialogs() }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun EmptyState(onAddGroup: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.empty_groups),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAddGroup) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.add_group))
        }
    }
}

@Composable
private fun GroupHeader(
    status: GroupStatus,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val targetSeconds = status.totalTargetSeconds
    val workedSeconds = status.totalWorkedSeconds
    val remainingSeconds = (targetSeconds - workedSeconds).coerceAtLeast(0L)
    val timeProgress = if (targetSeconds > 0L) {
        (workedSeconds.toFloat() / targetSeconds).coerceIn(0f, 1f)
    } else {
        0f
    }
    val scheduledTasks = status.scheduledTaskCountToday
    val completedTasks = status.completedTaskCountToday.coerceAtMost(scheduledTasks)
    val remainingTasks = (scheduledTasks - completedTasks).coerceAtLeast(0)
    val taskProgress = if (scheduledTasks > 0) {
        (completedTasks.toFloat() / scheduledTasks).coerceIn(0f, 1f)
    } else {
        0f
    }
    val taskProgressPercent = (taskProgress * 100f + 0.5f).toInt()

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onOpen),
        ) {
            Text(
                text = status.group.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = if (status.group.timed) {
                    stringResource(R.string.daily_minutes_value, status.group.dailyMinutes)
                } else {
                    stringResource(R.string.group_without_time)
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Filled.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.size(4.dp))
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Column(modifier = Modifier.clickable(onClick = onOpen)) {
        if (status.group.timed && targetSeconds > 0L) {
            LinearProgressIndicator(
                progress = { timeProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp),
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        R.string.group_progress_value,
                        formatClock(workedSeconds),
                        formatClock(targetSeconds),
                    ),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.group_progress_remaining, formatClock(remainingSeconds)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        } else if (!status.group.timed && scheduledTasks > 0) {
            LinearProgressIndicator(
                progress = { taskProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp),
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        R.string.group_checklist_progress_value,
                        completedTasks,
                        scheduledTasks,
                        taskProgressPercent,
                    ),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.group_checklist_progress_remaining, remainingTasks),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        } else {
            Text(
                text = stringResource(R.string.group_no_work_today),
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f),
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.surface)
}

@Composable
private fun TaskRow(
    status: TaskStatus,
    timedGroup: Boolean,
    onToggleEnabled: () -> Unit,
    onStartTaskAlone: () -> Unit,
    onMarkDone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val enabled = status.task.enabled
    val scheduledToday = status.scheduledToday
    val appliesToday = enabled && scheduledToday
    val visuallyPending = appliesToday && !status.completedToday
    val hasRemainingWork = appliesToday && if (timedGroup) {
        status.remainingSecondsToday > 0
    } else {
        !status.completedToday
    }
    val rowAlpha = if (visuallyPending) 1f else 0.62f
    val nameColor = if (visuallyPending) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val secondaryColor = if (visuallyPending) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    val markDoneTint = if (hasRemainingWork) {
        MaterialTheme.colorScheme.primary
    } else {
        secondaryColor
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(rowAlpha),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = status.task.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = nameColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (status.task.description.isNotBlank()) {
                        Text(
                            text = status.task.description,
                            fontSize = 12.sp,
                            color = secondaryColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = scheduleSummary(status.task.scheduledDays),
                        fontSize = 12.sp,
                        color = secondaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (timedGroup) {
                    Text(
                        text = formatWeight(status.task.weight),
                        color = if (visuallyPending) MaterialTheme.colorScheme.primary else secondaryColor,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.size(8.dp))
                }
                Switch(checked = enabled, onCheckedChange = { onToggleEnabled() })
            }

            Spacer(Modifier.height(6.dp))

            if (timedGroup && appliesToday) {
                val target = status.targetSecondsToday
                val worked = status.workedSecondsToday
                val progress = if (target > 0) (worked.toFloat() / target).coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                )
                Spacer(Modifier.height(4.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when {
                        !enabled -> stringResource(R.string.task_paused)
                        !scheduledToday -> stringResource(R.string.task_not_scheduled_today)
                        !timedGroup && status.completedToday -> stringResource(R.string.task_done_today)
                        !timedGroup -> stringResource(R.string.task_pending_today)
                        else -> "${formatClock(status.workedSecondsToday)} / ${formatClock(status.targetSecondsToday)}"
                    },
                    fontSize = 12.sp,
                    fontStyle = if (visuallyPending) FontStyle.Normal else FontStyle.Italic,
                    color = if (visuallyPending) MaterialTheme.colorScheme.onSurface else secondaryColor,
                    modifier = Modifier.weight(1f),
                )
                if (timedGroup) {
                    IconButton(
                        onClick = onStartTaskAlone,
                        enabled = hasRemainingWork,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.start_single_task),
                            tint = if (hasRemainingWork) MaterialTheme.colorScheme.primary else secondaryColor,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.size(4.dp))
                }
                IconButton(
                    onClick = onMarkDone,
                    enabled = hasRemainingWork,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = stringResource(R.string.mark_task_done),
                        tint = markDoneTint,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.size(4.dp))
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = null,
                        tint = secondaryColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.size(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = secondaryColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DisabledTasksToggle(
    count: Int,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) {
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = if (expanded) {
                stringResource(R.string.hide_disabled_tasks, count)
            } else {
                stringResource(R.string.show_disabled_tasks, count)
            },
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun GroupActions(
    canStartWork: Boolean,
    onAddTask: (() -> Unit)? = null,
    onStartWork: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onAddTask != null) {
            OutlinedButton(
                onClick = onAddTask,
                modifier = if (onStartWork == null) {
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                } else {
                    Modifier.height(52.dp)
                },
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.add_task_short))
            }
            if (onStartWork != null) {
                Spacer(Modifier.weight(1f))
            }
        }
        if (onStartWork != null) {
            Button(
                onClick = onStartWork,
                enabled = canStartWork,
                modifier = if (onAddTask == null) {
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                } else {
                    Modifier.height(52.dp)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text(
                    text = if (!canStartWork) {
                        stringResource(R.string.all_done_today)
                    } else {
                        stringResource(R.string.start_work)
                    },
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun AddGroupButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Spacer(Modifier.size(6.dp))
        Text(stringResource(R.string.add_group))
    }
}

@Composable
private fun GroupEditDialog(
    title: String,
    initialName: String,
    initialDailyMinutes: Int,
    initialTimed: Boolean,
    onSave: (name: String, dailyMinutes: Int, timed: Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var minutesText by remember { mutableStateOf(initialDailyMinutes.toString()) }
    var timed by remember { mutableStateOf(initialTimed) }

    val parsedMinutes = minutesText.toIntOrNull()
    val canSave = name.isNotBlank() && (!timed || (parsedMinutes != null && parsedMinutes > 0))

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.group_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.group_timed),
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = timed, onCheckedChange = { timed = it })
                }
                if (timed) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { v -> minutesText = v.filter { it.isDigit() } },
                        label = { Text(stringResource(R.string.minutes_per_day)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = { onSave(name, parsedMinutes ?: initialDailyMinutes.coerceAtLeast(1), timed) },
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun TaskEditDialog(
    title: String,
    initialName: String,
    initialDescription: String,
    initialWeight: Double,
    initialEnabled: Boolean,
    initialScheduledDays: Int,
    timedGroup: Boolean,
    onSave: (name: String, description: String, weight: Double, enabled: Boolean, scheduledDays: Int) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    var weightText by remember { mutableStateOf(weightToText(initialWeight)) }
    var enabled by remember { mutableStateOf(initialEnabled) }
    var scheduledDays by remember { mutableStateOf(Task.sanitizedScheduledDays(initialScheduledDays)) }

    val parsedWeight = parseWeight(weightText)
    val hasScheduledDays = (scheduledDays and Task.ALL_DAYS_MASK) != 0
    val canSave = name.isNotBlank() &&
        (!timedGroup || (parsedWeight != null && parsedWeight > 0.0)) &&
        hasScheduledDays

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.task_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.task_description)) },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier.heightIn(min = 88.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                )
                Spacer(Modifier.height(12.dp))
                if (timedGroup) {
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { v -> weightText = sanitizeWeightInput(v) },
                        label = { Text(stringResource(R.string.task_weight)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.task_enabled),
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                Spacer(Modifier.height(12.dp))
                ScheduledDaysSelector(
                    scheduledDays = scheduledDays,
                    onChange = { scheduledDays = it },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(
                        name,
                        description,
                        if (timedGroup) parsedWeight ?: 1.0 else 1.0,
                        enabled,
                        scheduledDays,
                    )
                },
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun ScheduledDaysSelector(
    scheduledDays: Int,
    onChange: (Int) -> Unit,
) {
    Text(
        text = stringResource(R.string.task_schedule),
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(6.dp))
    ScheduledDayCheckbox(
        mask = Task.MONDAY_MASK,
        label = stringResource(R.string.weekday_monday),
        scheduledDays = scheduledDays,
        onChange = onChange,
    )
    ScheduledDayCheckbox(
        mask = Task.TUESDAY_MASK,
        label = stringResource(R.string.weekday_tuesday),
        scheduledDays = scheduledDays,
        onChange = onChange,
    )
    ScheduledDayCheckbox(
        mask = Task.WEDNESDAY_MASK,
        label = stringResource(R.string.weekday_wednesday),
        scheduledDays = scheduledDays,
        onChange = onChange,
    )
    ScheduledDayCheckbox(
        mask = Task.THURSDAY_MASK,
        label = stringResource(R.string.weekday_thursday),
        scheduledDays = scheduledDays,
        onChange = onChange,
    )
    ScheduledDayCheckbox(
        mask = Task.FRIDAY_MASK,
        label = stringResource(R.string.weekday_friday),
        scheduledDays = scheduledDays,
        onChange = onChange,
    )
    ScheduledDayCheckbox(
        mask = Task.SATURDAY_MASK,
        label = stringResource(R.string.weekday_saturday),
        scheduledDays = scheduledDays,
        onChange = onChange,
    )
    ScheduledDayCheckbox(
        mask = Task.SUNDAY_MASK,
        label = stringResource(R.string.weekday_sunday),
        scheduledDays = scheduledDays,
        onChange = onChange,
    )
    if ((scheduledDays and Task.ALL_DAYS_MASK) == 0) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.task_schedule_required),
            color = MaterialTheme.colorScheme.error,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun ScheduledDayCheckbox(
    mask: Int,
    label: String,
    scheduledDays: Int,
    onChange: (Int) -> Unit,
) {
    val checked = (scheduledDays and mask) != 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clickable { onChange(toggleScheduledDay(scheduledDays, mask)) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onChange(toggleScheduledDay(scheduledDays, mask)) },
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun scheduleSummary(scheduledDays: Int): String {
    val normalized = scheduledDays and Task.ALL_DAYS_MASK
    if (normalized == Task.ALL_DAYS_MASK) return stringResource(R.string.task_schedule_every_day)
    if (normalized == 0) return stringResource(R.string.task_schedule_no_days)
    return listOfNotNull(
        stringResource(R.string.weekday_short_monday).takeIf { (normalized and Task.MONDAY_MASK) != 0 },
        stringResource(R.string.weekday_short_tuesday).takeIf { (normalized and Task.TUESDAY_MASK) != 0 },
        stringResource(R.string.weekday_short_wednesday).takeIf { (normalized and Task.WEDNESDAY_MASK) != 0 },
        stringResource(R.string.weekday_short_thursday).takeIf { (normalized and Task.THURSDAY_MASK) != 0 },
        stringResource(R.string.weekday_short_friday).takeIf { (normalized and Task.FRIDAY_MASK) != 0 },
        stringResource(R.string.weekday_short_saturday).takeIf { (normalized and Task.SATURDAY_MASK) != 0 },
        stringResource(R.string.weekday_short_sunday).takeIf { (normalized and Task.SUNDAY_MASK) != 0 },
    ).joinToString(", ")
}

private fun toggleScheduledDay(scheduledDays: Int, mask: Int): Int {
    val normalized = scheduledDays and Task.ALL_DAYS_MASK
    return if ((normalized and mask) != 0) normalized and mask.inv()
    else normalized or mask
}

private fun sanitizeWeightInput(raw: String): String {
    val normalized = raw.replace(',', '.').filter { it.isDigit() || it == '.' }
    val dotIdx = normalized.indexOf('.')
    return if (dotIdx < 0) normalized
    else normalized.substring(0, dotIdx + 1) + normalized.substring(dotIdx + 1).filter { it.isDigit() }
}

private fun parseWeight(text: String): Double? = text.replace(',', '.').toDoubleOrNull()

private fun weightToText(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString()
    else "%.2f".format(value).trimEnd('0').trimEnd('.')
