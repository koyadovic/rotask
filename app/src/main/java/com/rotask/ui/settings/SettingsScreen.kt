package com.rotask.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotask.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by vm.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri -> uri?.let { vm.exportData(it) } },
    )
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> pendingImportUri = uri },
    )

    val statusMessage = state.status?.let { status ->
        stringResource(
            when (status) {
                SettingsStatus.EXPORT_SUCCESS -> R.string.export_success
                SettingsStatus.EXPORT_ERROR -> R.string.export_failed
                SettingsStatus.IMPORT_SUCCESS -> R.string.import_success
                SettingsStatus.IMPORT_ERROR -> R.string.import_failed
            }
        )
    }
    LaunchedEffect(state.statusId) {
        if (statusMessage != null) snackbarHostState.showSnackbar(statusMessage)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                SettingsSectionTitle(stringResource(R.string.sound_settings))
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.current_completion_sound, state.completionSoundTitle),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    enabled = !state.busy,
                    onClick = { vm.openSoundPicker() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.change_completion_sound))
                }
            }

            item {
                SettingsSectionTitle(stringResource(R.string.data_management))
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    enabled = !state.busy,
                    onClick = { exportLauncher.launch("rotask-backup.json") },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.FileDownload, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.export_tasks))
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    enabled = !state.busy,
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.FileUpload, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.import_tasks))
                }
            }
        }
    }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text(stringResource(R.string.import_replace_title)) },
            text = { Text(stringResource(R.string.import_replace_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingImportUri = null
                        vm.importData(uri)
                    },
                ) {
                    Text(stringResource(R.string.import_tasks))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (state.soundPickerOpen) {
        AlertDialog(
            onDismissRequest = { vm.closeSoundPicker() },
            title = { Text(stringResource(R.string.choose_completion_sound)) },
            text = {
                if (state.soundOptionsLoading) {
                    Text(
                        text = "...",
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 420.dp),
                    ) {
                        items(
                            count = state.soundOptions.size,
                            key = { index -> state.soundOptions[index].sound.toString() },
                        ) { index ->
                            val option = state.soundOptions[index]
                            CompletionSoundRow(
                                option = option,
                                selected = state.draftCompletionSound == option.sound,
                                enabled = !state.busy,
                                onClick = { vm.previewDraftCompletionSound(option.sound) },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !state.soundOptionsLoading,
                    onClick = { vm.confirmDraftCompletionSound() },
                ) {
                    Text(stringResource(R.string.confirm_sound_change))
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.closeSoundPicker() }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(6.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.surface)
}

@Composable
private fun CompletionSoundRow(
    option: CompletionSoundOption,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            enabled = enabled,
            onClick = onClick,
        )
        Text(
            text = option.title,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
    }
}
