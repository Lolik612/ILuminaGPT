package com.lolik612.luminagpt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LuminaTheme {
                ChatScreen()
            }
        }
    }
}

private val LuminaColors = darkColorScheme(
    primary = Color(0xFFB794FF),
    onPrimary = Color(0xFF241046),
    primaryContainer = Color(0xFF3B2A63),
    onPrimaryContainer = Color(0xFFEBDDFF),
    background = Color(0xFF0E0B14),
    onBackground = Color(0xFFE8E1F2),
    surface = Color(0xFF16121F),
    onSurface = Color(0xFFE8E1F2),
    surfaceVariant = Color(0xFF241E33),
    onSurfaceVariant = Color(0xFFD5CCE6),
    error = Color(0xFFFF8A8A),
)

@Composable
fun LuminaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LuminaColors, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: ChatViewModel = viewModel()) {
    var input by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val messages = vm.messages

    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.lastIndex)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("LuminaGPT") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                actions = {
                    IconButton(onClick = { vm.clear() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Очистить чат")
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Сообщение…") },
                    maxLines = 6,
                    shape = RoundedCornerShape(24.dp),
                )
                FilledIconButton(
                    onClick = {
                        if (vm.generating) {
                            vm.stop()
                        } else if (input.isNotBlank()) {
                            vm.send(input)
                            input = ""
                        }
                    },
                    modifier = Modifier
                        .padding(start = 8.dp, bottom = 4.dp),
                ) {
                    Text(if (vm.generating) "■" else "➤", fontSize = 18.sp)
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("✦", fontSize = 48.sp, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "LuminaGPT",
                        fontSize = 26.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        "Спроси что-нибудь. Адрес сервера задаётся в настройках ⚙",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(messages.size) { i ->
                        val m = messages[i]
                        val isUser = m.role == "user"
                        val shownText =
                            if (m.content.isEmpty() && vm.generating && i == messages.lastIndex) "…" else m.content
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (isUser) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.widthIn(max = 320.dp),
                            ) {
                                SelectionContainer {
                                    Text(
                                        shownText,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            initial = vm.settings,
            onDismiss = { showSettings = false },
            onSave = {
                vm.saveSettings(it)
                showSettings = false
            },
        )
    }
}

@Composable
fun SettingsDialog(initial: Settings, onDismiss: () -> Unit, onSave: (Settings) -> Unit) {
    var url by remember { mutableStateOf(initial.serverUrl) }
    var key by remember { mutableStateOf(initial.apiKey) }
    var system by remember { mutableStateOf(initial.systemPrompt) }
    var temp by remember { mutableStateOf(initial.temperature) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Настройки") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Адрес сервера") },
                    placeholder = { Text("http://1.2.3.4:8080") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("API-ключ (если задан)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = system,
                    onValueChange = { system = it },
                    label = { Text("Системный промпт") },
                    maxLines = 4,
                )
                Text("Температура: ${"%.2f".format(temp)}")
                Slider(value = temp, onValueChange = { temp = it }, valueRange = 0f..1.5f)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(Settings(url, key, system, temp)) }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}
