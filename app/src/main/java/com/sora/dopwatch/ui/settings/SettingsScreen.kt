package com.sora.dopwatch.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val saveMessage by viewModel.saveMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(saveMessage) {
        saveMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 制限時間設定
            ThresholdSection(
                totalHours = settings.totalLimitMs.toFloat() / (60 * 60 * 1000),
                snsHours = settings.snsLimitMs.toFloat() / (60 * 60 * 1000),
                videoHours = settings.videoLimitMs.toFloat() / (60 * 60 * 1000),
                onSave = viewModel::saveThresholds
            )

            // LINE設定
            LineConfigSection(
                token = settings.lineToken,
                groupId = settings.lineGroupId,
                onSave = viewModel::saveLineConfig
            )

            // Beeminder設定
            BeeminderConfigSection(
                user = settings.beeminderUser,
                token = settings.beeminderToken,
                goal = settings.beeminderGoal,
                onSave = viewModel::saveBeeminderConfig
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ThresholdSection(
    totalHours: Float,
    snsHours: Float,
    videoHours: Float,
    onSave: (Float, Float, Float) -> Unit
) {
    var total by remember(totalHours) { mutableFloatStateOf(totalHours) }
    var sns by remember(snsHours) { mutableFloatStateOf(snsHours) }
    var video by remember(videoHours) { mutableFloatStateOf(videoHours) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("制限時間", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            SliderWithLabel("総使用時間", total, 1f..8f) { total = it }
            SliderWithLabel("SNS", sns, 0.5f..4f) { sns = it }
            SliderWithLabel("動画", video, 0.5f..4f) { video = it }

            Button(
                onClick = { onSave(total, sns, video) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存")
            }
        }
    }
}

@Composable
private fun SliderWithLabel(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    val hours = value.toInt()
    val minutes = ((value - hours) * 60).toInt()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text("${hours}h ${minutes}m", fontWeight = FontWeight.Bold)
    }
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = range,
        steps = ((range.endInclusive - range.start) * 2 - 1).toInt()
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun LineConfigSection(
    token: String,
    groupId: String,
    onSave: (String, String) -> Unit
) {
    var editToken by remember(token) { mutableStateOf(token) }
    var editGroupId by remember(groupId) { mutableStateOf(groupId) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("LINE Messaging API", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = editToken,
                onValueChange = { editToken = it },
                label = { Text("Channel Access Token") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = editGroupId,
                onValueChange = { editGroupId = it },
                label = { Text("Group ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onSave(editToken, editGroupId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存")
            }
        }
    }
}

@Composable
private fun BeeminderConfigSection(
    user: String,
    token: String,
    goal: String,
    onSave: (String, String, String) -> Unit
) {
    var editUser by remember(user) { mutableStateOf(user) }
    var editToken by remember(token) { mutableStateOf(token) }
    var editGoal by remember(goal) { mutableStateOf(goal) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Beeminder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = editUser,
                onValueChange = { editUser = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = editToken,
                onValueChange = { editToken = it },
                label = { Text("Auth Token") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = editGoal,
                onValueChange = { editGoal = it },
                label = { Text("Goal Slug") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onSave(editUser, editToken, editGoal) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存")
            }
        }
    }
}
