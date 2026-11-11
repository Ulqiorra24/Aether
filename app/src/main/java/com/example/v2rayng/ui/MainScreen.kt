package com.example.v2rayng.ui

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
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
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val homeScroll = rememberScrollState()
    val profileScroll = rememberScrollState()
    val settingsScroll = rememberScrollState()
    val scrollState = when (selectedTab) { 1 -> profileScroll; 2 -> settingsScroll; else -> homeScroll }
    var coreVersion by rememberSaveable { mutableStateOf("Загрузка…") }
    LaunchedEffect(Unit) {
        coreVersion = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                libv2ray.Libv2ray.checkVersionX().ifBlank { "Недоступна" }
            } catch (_: LinkageError) {
                "Недоступна"
            } catch (_: Exception) {
                "Недоступна"
            }
        }
    }
    var showServers by rememberSaveable { mutableStateOf(false) }
    val palette = MaterialTheme.colorScheme
    val dark = palette.background.luminance() < 0.5f
    val versionName = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "—"
    }
    var accountToDelete by rememberSaveable { mutableStateOf<String?>(null) }
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

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(palette.background).drawBehind {
            drawRect(Brush.radialGradient(
                listOf(Color(0xFF7928CA).copy(alpha = if (dark) 0.36f else 0.12f), Color.Transparent),
                center = Offset(size.width, size.height * 0.12f), radius = size.width * 0.95f
            ))
            drawRect(Brush.radialGradient(
                listOf(Color(0xFF9B2DC3).copy(alpha = if (dark) 0.22f else 0.09f), Color.Transparent),
                center = Offset(0f, size.height * 0.85f), radius = size.width
            ))
        },
        bottomBar = {
            Surface(
                modifier = Modifier.navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp),
                shape = RoundedCornerShape(32.dp),
                color = palette.surfaceVariant.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, palette.outlineVariant)
            ) {
                NavigationBar(containerColor = Color.Transparent, windowInsets = WindowInsets(0, 0, 0, 0)) {
                    val tabs = listOf(
                        "Главная" to Icons.Default.Home,
                        "Профиль" to Icons.Default.Person,
                        "Настройки" to Icons.Default.Settings
                    )
                    tabs.forEachIndexed { index, (title, icon) ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(icon, contentDescription = null) },
                            label = { Text(title, maxLines = 1) }
                        )
                    }
                }
            }
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Aether", fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            when (selectedTab) {
                0 -> {
            Surface(
                shape = CircleShape,
                color = palette.surface.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, palette.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(6.dp).background(
                        if (viewModel.isConnected) Color(0xFF58D9AF) else palette.onSurfaceVariant, CircleShape))
                    Text(
                        if (viewModel.isConnected) "Соединение защищено" else "Ваше личное пространство",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier.size(220.dp)
                    .border(1.dp, palette.primary.copy(alpha = 0.22f), RoundedCornerShape(68.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(188.dp)
                    .border(1.dp, palette.primary.copy(alpha = 0.12f), RoundedCornerShape(56.dp)))
                Surface(
                    onClick = {
                        if (!viewModel.isConnected && !viewModel.isConnecting) {
                            val intent = VpnService.prepare(context)
                            if (intent != null) vpnPermissionLauncher.launch(intent)
                            else viewModel.startVpnService(context)
                        } else viewModel.toggleVpn(context)
                    },
                    enabled = !viewModel.isStopping,
                    modifier = Modifier.size(152.dp),
                    shape = RoundedCornerShape(44.dp),
                    color = if (viewModel.isConnected) palette.primaryContainer else palette.surfaceVariant,
                    border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.4f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (viewModel.isConnecting || viewModel.isStopping) {
                            CircularProgressIndicator(Modifier.size(90.dp), strokeWidth = 2.dp)
                        }
                        Icon(Icons.Default.PowerSettingsNew,
                            contentDescription = when {
                                viewModel.isStopping -> "Отключение"
                                viewModel.isConnecting -> "Отменить подключение"
                                viewModel.isConnected -> "Отключить VPN"
                                else -> "Подключить VPN"
                            },
                            modifier = Modifier.size(64.dp),
                            tint = if (viewModel.isConnected) palette.primary else palette.onSurface)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    when {
                        viewModel.isConnecting -> "Подключаемся…"
                        viewModel.isStopping -> "Отключаемся…"
                        viewModel.isConnected -> "Вы подключены"
                        else -> "Готовы к подключению"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Text(viewModel.statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.onSurfaceVariant, textAlign = TextAlign.Center)
                if (viewModel.username.isNotBlank()) {
                    Text(
                        text = "Осталось дней: ${viewModel.subscriptionDaysRemaining ?: "—"}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            val selectedNode = viewModel.nodes.getOrNull(viewModel.selectedNodeIndex)
            Surface(
                onClick = {
                    when {
                        viewModel.accounts.isEmpty() -> showAddAccountDialog = true
                        viewModel.nodes.isEmpty() -> selectedTab = 1
                        else -> showServers = true
                    }
                },
                modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
                shape = RoundedCornerShape(24.dp),
                color = palette.surface.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, palette.outlineVariant)
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(shape = RoundedCornerShape(16.dp), color = palette.primaryContainer) {
                        Icon(Icons.Default.Dns, null, Modifier.padding(12.dp).size(24.dp),
                            tint = palette.onPrimaryContainer)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("СЕРВЕР", style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.5.sp, color = palette.onSurfaceVariant)
                        Text(selectedNode?.name ?: "Добавьте подписку",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(selectedNode?.let {
                            if (it.latencyMs >= 0) "${it.latencyMs} мс · Выбрать сервер"
                            else "Задержка недоступна · Выбрать сервер"
                        } ?: "Добавьте ключ подписки для подключения",
                            style = MaterialTheme.typography.bodySmall, color = palette.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = palette.onSurfaceVariant)
                }
            }


                }
                1 -> {
                    Text("Профиль", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth())
                    Surface(shape = RoundedCornerShape(24.dp), color = palette.surface.copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, palette.outlineVariant), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Текущая подписка", color = palette.onSurfaceVariant,
                                style = MaterialTheme.typography.labelLarge)
                            Text(viewModel.username.ifBlank { "Подписка не добавлена" },
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(if (viewModel.username.isBlank()) "Добавьте ключ, чтобы получить список серверов"
                                else "Серверов в подписке: ${viewModel.nodes.size}",
                                style = MaterialTheme.typography.bodyMedium, color = palette.onSurfaceVariant)
                        }
                    }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Подписки", style = MaterialTheme.typography.titleSmall)
                            IconButton(onClick = { showAddAccountDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Добавить подписку")
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
                                        IconButton(
                                            onClick = { accountToDelete = acc },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Удалить подписку",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { viewModel.fetchSubscription() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !viewModel.isLoading && viewModel.username.isNotBlank(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (viewModel.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("Обновить текущую подписку")
                        }
                }
                2 -> {
                    Text("Настройки", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth())
                    Surface(shape = RoundedCornerShape(24.dp), color = palette.surface.copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, palette.outlineVariant), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        }
                    }
            Card(
                modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = palette.surface.copy(alpha = 0.75f)),
                border = BorderStroke(1.dp, palette.outlineVariant.copy(alpha = 0.7f))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Shield, null, Modifier.size(18.dp), tint = palette.primary)
                        Text("Режим трафика", style = MaterialTheme.typography.labelLarge)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        mapOf("Global (Proxy All)" to "Весь трафик", "Bypass LAN" to "Обход LAN").forEach { (key, label) ->
                            val selected = viewModel.routingMode == key
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.updateRoutingMode(key, context) },
                                label = { Text(label, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }
                    }
                }
            }

                    Spacer(Modifier.height(24.dp))
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Aether · $versionName", style = MaterialTheme.typography.bodyMedium,
                            color = palette.onSurfaceVariant)
                        Text("Ядро Xray · $coreVersion", style = MaterialTheme.typography.bodySmall,
                            color = palette.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            }


        }
    }

            if (showAddAccountDialog) {
                AlertDialog(
                    onDismissRequest = { showAddAccountDialog = false },
                    title = { Text("Добавить подписку") },
                    text = {
                        OutlinedTextField(
                            value = newAccountUsername,
                            onValueChange = { newAccountUsername = it },
                            label = { Text("Ключ подписки") },
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





    if (showServers) {
        ModalBottomSheet(onDismissRequest = { showServers = false }) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Выберите сервер", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                        Text("${viewModel.nodes.size} доступно · ${viewModel.username}",
                            style = MaterialTheme.typography.bodySmall, color = palette.onSurfaceVariant)
                    }
                    IconButton(onClick = { viewModel.pingNodes() }) {
                        Icon(Icons.Default.Refresh, "Проверить задержку")
                    }
                }
                TextButton(onClick = {
                    showServers = false
                    selectedTab = 1
                }) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Управление подписками")
                }
                viewModel.nodes.forEachIndexed { index, node ->
                    val selected = index == viewModel.selectedNodeIndex
                    Surface(
                        onClick = { viewModel.selectNode(index, context); showServers = false },
                        shape = RoundedCornerShape(18.dp),
                        color = if (selected) palette.primaryContainer else palette.surfaceVariant
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(node.name, fontWeight = FontWeight.Medium)
                                Text(if (node.latencyMs >= 0) "${node.latencyMs} мс" else "Задержка недоступна",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            RadioButton(selected = selected, onClick = null)
                        }
                    }
                }
            }
        }
    }

    accountToDelete?.let { account ->
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("Удалить подписку?") },
            text = { Text("Ключ будет удалён с этого устройства. Его можно добавить снова.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeAccount(account, context)
                    accountToDelete = null
                }) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) { Text("Отмена") }
            }
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
