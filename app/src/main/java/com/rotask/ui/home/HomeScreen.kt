package com.rotask.ui.home

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotask.R
import com.rotask.domain.TaskStatus
import com.rotask.ui.format.formatClock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onStartWork: (Long) -> Unit
) {
    val state by vm.uiState.collectAsState()

    LaunchedEffect(Unit) {
        vm.navToWork.collect { onStartWork(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    TextButton(onClick = { vm.showConfigDialog() }) {
                        Icon(Icons.Filled.Schedule, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text(stringResource(R.string.daily_minutes_value, state.dailyMinutes))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.showAddDialog() }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_task))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (state.statuses.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.empty_tasks),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.statuses, key = { it.task.id }) { status ->
                            TaskRow(
                                status = status,
                                onToggleEnabled = { vm.toggleEnabled(status.task) },
                                onEdit = { vm.startEditing(status.task) },
                                onDelete = { vm.startDeleting(status.task) }
                            )
                        }
                    }
                }
            }

            StartWorkButton(
                enabled = state.hasWorkRemaining,
                onClick = { vm.startWork() }
            )
        }
    }

    if (state.showAdd) {
        TaskEditDialog(
            title = stringResource(R.string.add_task),
            initialName = "",
            initialDescription = "",
            initialWeight = 1,
            initialEnabled = true,
            onSave = { name, description, weight, enabled ->
                vm.addTask(name, description, weight, enabled)
            },
            onCancel = { vm.dismissDialogs() }
        )
    }

    state.editing?.let { task ->
        TaskEditDialog(
            title = stringResource(R.string.edit_task),
            initialName = task.name,
            initialDescription = task.description,
            initialWeight = task.weight,
            initialEnabled = task.enabled,
            onSave = { name, description, weight, enabled ->
                vm.updateTask(task, name, description, weight, enabled)
            },
            onCancel = { vm.dismissDialogs() }
        )
    }

    state.deleting?.let { task ->
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
            }
        )
    }

    if (state.showConfig) {
        DailyMinutesDialog(
            initialMinutes = state.dailyMinutes,
            onSave = { vm.setDailyMinutes(it) },
            onCancel = { vm.dismissDialogs() }
        )
    }
}

@Composable
private fun TaskRow(
    status: TaskStatus,
    onToggleEnabled: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val enabled = status.task.enabled
    val nameColor = if (enabled) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val secondaryColor = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = status.task.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = nameColor
                    )
                    if (status.task.description.isNotBlank()) {
                        Text(
                            text = status.task.description,
                            fontSize = 13.sp,
                            color = secondaryColor
                        )
                    }
                }
                Text(
                    text = "x${status.task.weight}",
                    color = if (enabled) MaterialTheme.colorScheme.primary else secondaryColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.size(8.dp))
                Switch(checked = enabled, onCheckedChange = { onToggleEnabled() })
            }

            if (enabled) {
                Spacer(Modifier.height(8.dp))
                val target = status.targetSecondsToday
                val worked = status.workedSecondsToday
                val progress = if (target > 0) (worked.toFloat() / target).coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${formatClock(worked)} / ${formatClock(target)}",
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.task_paused),
                    color = secondaryColor,
                    fontStyle = FontStyle.Italic,
                    fontSize = 13.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = null, tint = secondaryColor)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = secondaryColor)
                }
            }
        }
    }
}

@Composable
private fun StartWorkButton(enabled: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.surface)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(
                text = if (enabled) {
                    stringResource(R.string.start_work)
                } else {
                    stringResource(R.string.all_done_today)
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TaskEditDialog(
    title: String,
    initialName: String,
    initialDescription: String,
    initialWeight: Int,
    initialEnabled: Boolean,
    onSave: (name: String, description: String, weight: Int, enabled: Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    var weightText by remember { mutableStateOf(initialWeight.toString()) }
    var enabled by remember { mutableStateOf(initialEnabled) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.task_name)) },
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.task_description)) },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier.heightIn(min = 88.dp),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { v -> weightText = v.filter { it.isDigit() } },
                    label = { Text(stringResource(R.string.task_weight)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.task_enabled),
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && (weightText.toIntOrNull() ?: 0) > 0,
                onClick = {
                    onSave(name, description, weightText.toIntOrNull() ?: 1, enabled)
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun DailyMinutesDialog(
    initialMinutes: Int,
    onSave: (Int) -> Unit,
    onCancel: () -> Unit
) {
    var text by remember { mutableStateOf(initialMinutes.toString()) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.config_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { v -> text = v.filter { it.isDigit() } },
                label = { Text(stringResource(R.string.minutes_per_day)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            TextButton(
                enabled = (text.toIntOrNull() ?: 0) > 0,
                onClick = { onSave(text.toIntOrNull() ?: 1) }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
