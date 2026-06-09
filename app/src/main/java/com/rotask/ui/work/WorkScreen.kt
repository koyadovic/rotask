package com.rotask.ui.work

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotask.R
import com.rotask.ui.format.formatClock

@Composable
fun WorkScreen(
    vm: WorkViewModel,
    onFinish: () -> Unit
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(state.finished) {
        if (state.finished) onFinish()
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (state.loading) {
                Text(
                    "...",
                    color = MaterialTheme.colorScheme.onBackground
                )
            } else {
                if (state.timed) {
                    WorkBody(
                        taskName = state.taskName,
                        taskDescription = state.taskDescription,
                        elapsedSeconds = state.sessionElapsedSeconds,
                        targetSeconds = state.sessionTargetSeconds,
                        paused = state.paused,
                        onPauseToggle = { vm.togglePause() },
                        onSkip = { vm.skip() },
                        onStop = { vm.stop() }
                    )
                } else {
                    UntimedWorkBody(
                        taskName = state.taskName,
                        taskDescription = state.taskDescription,
                        onMarkDone = { vm.markDone() },
                        onSkip = { vm.skip() },
                        onStop = { vm.stop() },
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkBody(
    taskName: String,
    taskDescription: String,
    elapsedSeconds: Long,
    targetSeconds: Long,
    paused: Boolean,
    onPauseToggle: () -> Unit,
    onSkip: () -> Unit,
    onStop: () -> Unit,
) {
    val progress = if (targetSeconds > 0) {
        (elapsedSeconds.toFloat() / targetSeconds).coerceIn(0f, 1f)
    } else 0f
    val timerColor = if (paused) {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.primary
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = taskName,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            if (taskDescription.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = taskDescription,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(40.dp))
            Text(
                text = stringResource(R.string.elapsed),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = formatClock(elapsedSeconds),
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = timerColor,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.target),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = formatClock(targetSeconds),
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(24.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
            ) {
                Icon(Icons.Filled.SkipNext, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.skip_work))
            }
            OutlinedButton(
                onClick = onStop,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
            ) {
                Icon(Icons.Filled.Stop, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.stop_work))
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onPauseToggle,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.size(10.dp))
            val label = when {
                !paused -> stringResource(R.string.pause_work)
                elapsedSeconds == 0L -> stringResource(R.string.start_session)
                else -> stringResource(R.string.resume_work)
            }
            Text(
                text = label,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun UntimedWorkBody(
    taskName: String,
    taskDescription: String,
    onMarkDone: () -> Unit,
    onSkip: () -> Unit,
    onStop: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = taskName,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            if (taskDescription.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = taskDescription,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
            ) {
                Icon(Icons.Filled.SkipNext, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.skip_work))
            }
            OutlinedButton(
                onClick = onStop,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
            ) {
                Icon(Icons.Filled.Stop, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.stop_work))
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onMarkDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = stringResource(R.string.mark_task_done),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
