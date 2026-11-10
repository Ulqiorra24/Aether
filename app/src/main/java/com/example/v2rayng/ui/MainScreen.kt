package com.example.v2rayng.ui

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val versionName = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "—"
    }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var newAccountUsername by remember { mutableStateOf("") }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.startVpnService(context)
        } else {
            viewModel.onVpnPermissionDenied()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    Text("Aether", style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp))
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    NavigationDrawerItem(
                        label = { Text("Настройки") },
                        selected = showSettings,
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        onClick = {
                            showSettings = true
                            scope.launch { drawerState.close() }
                        }
                    )
                    Spacer(Modifier.weight(1f))
                    NavigationDrawerItem(
                        label = { Text("О приложении") },
                        selected = showAbout,
                        icon = { Icon(Icons.Default.Info, contentDescription = null) },
                        onClick = {
                            showAbout = true
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        }
    ) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aether", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "Открыть меню")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (showSettings) {
                ModalBottomSheet(onDismissRequest = { showSettings = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("Настройки и аккаунты", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Text("Тема оформления", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val themes = mapOf("Dark" to "Темная", "Light" to "Светлая", "System" to "Системная")
                            themes.forEach { (key, label) ->
                                val selected = viewModel.themeMode == key
                                OutlinedButton(
                                    onClick = { viewModel.updateThemeMode(key) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Text(label, fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Аккаунты / Подписки", style = MaterialTheme.typography.titleSmall)
                            IconButton(onClick = { showAddAccountDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Добавить аккаунт")
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            viewModel.accounts.forEach { acc ->
                                val isSelected = acc == viewModel.username
                                Surface(
                                    onClick = { viewModel.selectAccount(acc, context) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = acc,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (viewModel.accounts.size > 1) {
                                            IconButton(
                                                onClick = { viewModel.removeAccount(acc) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Удалить аккаунт",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { viewModel.fetchSubscription() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !viewModel.isLoading,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (viewModel.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("Обновить текущую подписку")
                        }
                    }
                }
            }

            }

            if (showAddAccountDialog) {
                AlertDialog(
                    onDismissRequest = { showAddAccountDialog = false },
                    title = { Text("Добавить новый аккаунт") },
                    text = {
                        OutlinedTextField(
                            value = newAccountUsername,
                            onValueChange = { newAccountUsername = it },
                            label = { Text("Имя пользователя (ник в x-ui)") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (newAccountUsername.isNotBlank()) {
                                viewModel.addAccount(newAccountUsername, context)
                                newAccountUsername = ""
                                showAddAccountDialog = false
                            }
                        }) {
                            Text("Добавить")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddAccountDialog = false }) {
                            Text("Отмена")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(
                        if (viewModel.isConnected)
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.primaryContainer
                    )
            ) {
                FilledIconButton(
                    onClick = {
                        if (!viewModel.isConnected && !viewModel.isConnecting) {
                            val intent = VpnService.prepare(context)
                            if (intent != null) {
                                vpnPermissionLauncher.launch(intent)
                            } else {
                                viewModel.startVpnService(context)
                            }
                        } else {
                            viewModel.toggleVpn(context)
                        }
                    },
                    enabled = !viewModel.isStopping,
                    modifier = Modifier.size(140.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (viewModel.isConnected)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (viewModel.isConnected) Icons.Default.Lock else Icons.Default.PlayArrow,
                        contentDescription = "Подключение",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Text(
                text = when {
                    viewModel.isConnecting -> "ПОДКЛЮЧЕНИЕ…"
                    viewModel.isStopping -> "ОТКЛЮЧЕНИЕ…"
                    viewModel.isConnected -> "ПОДКЛЮЧЕНО"
                    else -> "ОТКЛЮЧЕНО"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (viewModel.isConnected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )

            Text(
                text = viewModel.statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Direct Nodes List on Screen (Replacing Dropdown)
            if (viewModel.nodes.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Доступные узлы (${viewModel.username})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { viewModel.pingNodes() }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Проверить пинг", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.nodes.forEachIndexed { index, node ->
                        val isSelected = index == viewModel.selectedNodeIndex
                        val pingText = if (node.latencyMs >= 0) "${node.latencyMs} мс" else "Нет связи"

                        Surface(
                            onClick = { viewModel.selectNode(index, context) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = node.name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = pingText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.selectNode(index, context) }
                                )
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Маршрутизация", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val routingModes = mapOf("Global (Proxy All)" to "Весь трафик", "Bypass LAN" to "Обход LAN")
                        routingModes.forEach { (key, label) ->
                            val selected = viewModel.routingMode == key
                            OutlinedButton(
                                onClick = { viewModel.updateRoutingMode(key, context) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Text(label, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }

    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("О приложении") },
            text = { Text("Aether\nВерсия $versionName") },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text("Закрыть") } }
        )
    }

    val updateInfo = viewModel.availableUpdate
    if (updateInfo != null) {
        AlertDialog(
            onDismissRequest = { if (!viewModel.isDownloadingUpdate) viewModel.dismissUpdate() },
            title = { Text("Доступно обновление (${updateInfo.tagName})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Найдена новая версия Aether. Хотите скачать и обновить приложение прямо сейчас?")
                    if (updateInfo.releaseNotes.isNotBlank()) {
                        Text(
                            text = updateInfo.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    viewModel.updateDownloadError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                    if (viewModel.isDownloadingUpdate) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val downloaded = viewModel.updateDownloadedBytes
                        val total = viewModel.updateTotalBytes
                        if (total > 0L) {
                            val fraction = (downloaded.toDouble() / total).coerceIn(0.0, 1.0)
                            LinearProgressIndicator(progress = { fraction.toFloat() }, modifier = Modifier.fillMaxWidth())
                            Text("${(fraction * 100).toInt()}% · ${android.text.format.Formatter.formatShortFileSize(context, downloaded)} / ${android.text.format.Formatter.formatShortFileSize(context, total)}")
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text("${android.text.format.Formatter.formatShortFileSize(context, downloaded)} · размер уточняется")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.downloadAndInstallUpdate(context, updateInfo) },
                    enabled = !viewModel.isDownloadingUpdate
                ) {
                    Text("Установить")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUpdate() }, enabled = !viewModel.isDownloadingUpdate) {
                    Text("Позже")
                }
            }
        )
    }
}
