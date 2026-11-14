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
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Edit
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current

    if (!viewModel.isLoggedIn) {
        AuthScreen(
            isLoading = viewModel.isAuthLoading,
            errorMessage = viewModel.authError,
            onLogin = { email, pass -> viewModel.loginUser(email, pass) },
            onRegister = { email, pass, payEmail -> viewModel.registerUser(email, pass, payEmail) },
            onActivate = { email, subId, pass -> viewModel.activateUser(email, subId, pass) }
        )
        return
    }

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
    var showPaymentDialog by rememberSaveable { mutableStateOf(false) }
    var selectedPaymentMonths by rememberSaveable { mutableIntStateOf(1) }
    var promoCodeInput by rememberSaveable { mutableStateOf("") }
    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showRoutingDialog by rememberSaveable { mutableStateOf(false) }
    var showLogsDialog by rememberSaveable { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editPaymentEmail by remember { mutableStateOf("") }
    var editNewPassword by remember { mutableStateOf("") }

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
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Текущий аккаунт", color = palette.onSurfaceVariant,
                                style = MaterialTheme.typography.labelLarge)
                            Text(viewModel.userEmail.ifBlank { viewModel.username.ifBlank { "Подписка не добавлена" } },
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                            val displayPaymentEmail = viewModel.paymentEmail.ifBlank { viewModel.userEmail.ifBlank { viewModel.username } }
                            if (displayPaymentEmail.isNotBlank()) {
                                HorizontalDivider(color = palette.outlineVariant.copy(alpha = 0.4f))
                                Surface(
                                    onClick = {
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Payment Email", displayPaymentEmail))
                                        android.widget.Toast.makeText(context, "Email для оплаты скопирован", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = palette.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Email для оплаты (нажмите для копирования):", style = MaterialTheme.typography.labelSmall, color = palette.onSurfaceVariant)
                                            Text(displayPaymentEmail, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = palette.primary)
                                        }
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Скопировать Email", tint = palette.primary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            if (viewModel.username.isNotBlank()) {
                                HorizontalDivider(color = palette.outlineVariant.copy(alpha = 0.4f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Статус подписки", style = MaterialTheme.typography.labelSmall, color = palette.onSurfaceVariant)
                                        Text(
                                            text = viewModel.subscriptionDaysRemaining?.let { "Осталось: $it дн." } ?: "Активна",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.primary
                                        )
                                    }
                                    Button(
                                        onClick = { showPaymentDialog = true },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Продлить")
                                    }
                                }
                            }
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
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    editPaymentEmail = viewModel.paymentEmail.ifBlank { viewModel.userEmail }
                                                    editNewPassword = ""
                                                    showEditProfileDialog = true
                                                },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Редактировать профиль",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { accountToDelete = acc },
                                                modifier = Modifier.size(40.dp)
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

                        OutlinedButton(
                            onClick = { viewModel.logout(context) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Выйти из аккаунта")
                        }
                }
                2 -> {
                    Text("Настройки", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth())

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = palette.surface.copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, palette.outlineVariant),
                        modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {

                            // Кнопка: Тема оформления
                            Surface(
                                onClick = { showThemeDialog = true },
                                shape = RoundedCornerShape(16.dp),
                                color = Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Surface(shape = RoundedCornerShape(12.dp), color = palette.primaryContainer) {
                                        Icon(Icons.Default.Palette, null, Modifier.padding(10.dp).size(22.dp), tint = palette.onPrimaryContainer)
                                    }
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("Тема оформления", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        val themeLabel = when (viewModel.themeMode) {
                                            "Dark" -> "Тёмная"
                                            "Light" -> "Светлая"
                                            else -> "Системная"
                                        }
                                        Text(themeLabel, style = MaterialTheme.typography.bodySmall, color = palette.onSurfaceVariant)
                                    }
                                    Icon(Icons.Default.ChevronRight, null, tint = palette.onSurfaceVariant)
                                }
                            }

                            HorizontalDivider(color = palette.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 14.dp))

                            // Кнопка: Маршрутизация
                            Surface(
                                onClick = { showRoutingDialog = true },
                                shape = RoundedCornerShape(16.dp),
                                color = Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Surface(shape = RoundedCornerShape(12.dp), color = palette.primaryContainer) {
                                        Icon(Icons.Default.Shield, null, Modifier.padding(10.dp).size(22.dp), tint = palette.onPrimaryContainer)
                                    }
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("Маршрутизация", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        val activeRules = mutableListOf<String>()
                                        if (viewModel.bypassLan) activeRules.add("LAN")
                                        if (viewModel.bypassRu) activeRules.add("RU сайты")
                                        val summary = if (activeRules.isEmpty()) "Без исключений" else "Обход: ${activeRules.joinToString(", ")}"
                                        Text(summary, style = MaterialTheme.typography.bodySmall, color = palette.onSurfaceVariant)
                                    }
                                    Icon(Icons.Default.ChevronRight, null, tint = palette.onSurfaceVariant)
                                }
                            }

                            HorizontalDivider(color = palette.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 14.dp))

                            // Кнопка: Логи трафика
                            Surface(
                                onClick = { showLogsDialog = true },
                                shape = RoundedCornerShape(16.dp),
                                color = Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Surface(shape = RoundedCornerShape(12.dp), color = palette.secondaryContainer) {
                                        Icon(Icons.Default.Info, null, Modifier.padding(10.dp).size(22.dp), tint = palette.onSecondaryContainer)
                                    }
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("Логи трафика", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        val logsCount = viewModel.logs.size
                                        val summary = if (logsCount == 0) "Записей нет" else "Записей: $logsCount"
                                        Text(summary, style = MaterialTheme.typography.bodySmall, color = palette.onSurfaceVariant)
                                    }
                                    Icon(Icons.Default.ChevronRight, null, tint = palette.onSurfaceVariant)
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

    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Редактирование профиля") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editPaymentEmail,
                        onValueChange = { editPaymentEmail = it },
                        label = { Text("Email для оплаты") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNewPassword,
                        onValueChange = { editNewPassword = it },
                        label = { Text("Новый пароль (если хотите изменить)") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateProfile(editPaymentEmail, editNewPassword.ifBlank { null }) { _, msg ->
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                    }
                    showEditProfileDialog = false
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showThemeDialog) {
        ModalBottomSheet(onDismissRequest = { showThemeDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Тема оформления", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Выберите внешний вид приложения", style = MaterialTheme.typography.bodySmall, color = palette.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                val themes = mapOf("Dark" to "Темная", "Light" to "Светлая", "System" to "Системная")
                themes.forEach { (key, label) ->
                    val selected = viewModel.themeMode == key
                    Surface(
                        onClick = {
                            viewModel.updateThemeMode(key)
                            showThemeDialog = false
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) palette.primaryContainer else palette.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, fontWeight = FontWeight.Medium)
                            RadioButton(selected = selected, onClick = null)
                        }
                    }
                }
            }
        }
    }

    if (showRoutingDialog) {
        ModalBottomSheet(onDismissRequest = { showRoutingDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = RoundedCornerShape(12.dp), color = palette.primaryContainer) {
                        Icon(Icons.Default.Shield, null, Modifier.padding(8.dp).size(20.dp), tint = palette.onPrimaryContainer)
                    }
                    Text("Исключения маршрутизации", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                // Исключить локальную сеть (LAN)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Исключить локальную сеть (LAN)", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text("Локальный трафик и адреса роутеров идут напрямую мимо VPN", style = MaterialTheme.typography.bodySmall, color = palette.onSurfaceVariant)
                    }
                    Switch(
                        checked = viewModel.bypassLan,
                        onCheckedChange = { viewModel.updateBypassLan(it, context) }
                    )
                }

                HorizontalDivider(color = palette.outlineVariant.copy(alpha = 0.4f))

                // Исключить российские сайты и IP-адреса
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Исключить российские сайты и IP-адреса", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text("Сайты .ru, гос ресурсы и IP-адреса РФ идут напрямую мимо VPN", style = MaterialTheme.typography.bodySmall, color = palette.onSurfaceVariant)
                    }
                    Switch(
                        checked = viewModel.bypassRu,
                        onCheckedChange = { viewModel.updateBypassRu(it, context) }
                    )
                }
            }
        }
    }

    if (showLogsDialog) {
        ModalBottomSheet(onDismissRequest = { showLogsDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(shape = RoundedCornerShape(12.dp), color = palette.secondaryContainer) {
                            Icon(Icons.Default.Info, null, Modifier.padding(8.dp).size(20.dp), tint = palette.onSecondaryContainer)
                        }
                        Text("Логи трафика", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Row {
                        if (viewModel.logs.isNotEmpty()) {
                            IconButton(onClick = {
                                val text = viewModel.logs.joinToString("\n") { "[${it.time}] [${it.tag}] ${it.message}" }
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("VPN Logs", text))
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Скопировать логи", tint = palette.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { viewModel.clearLogs() }) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Очистить логи", tint = palette.error, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                if (viewModel.logs.isEmpty()) {
                    Text(
                        "Нет логов. Подключите VPN для отслеживания TCP/UDP соединений.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)
                    ) {
                        val logsScroll = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .verticalScroll(logsScroll),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            viewModel.logs.forEach { log ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = log.time,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = palette.onSurfaceVariant
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = when (log.tag) {
                                            "TCP" -> palette.primaryContainer
                                            "UDP" -> palette.secondaryContainer
                                            "DIRECT" -> Color(0xFF10B981).copy(alpha = 0.2f)
                                            "PROXY" -> Color(0xFF8B5CF6).copy(alpha = 0.2f)
                                            "ERROR" -> palette.errorContainer
                                            "WARN" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                            else -> palette.surfaceVariant
                                        }
                                    ) {
                                        Text(
                                            text = log.tag,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                            color = when (log.tag) {
                                                "TCP" -> palette.onPrimaryContainer
                                                "UDP" -> palette.onSecondaryContainer
                                                "DIRECT" -> Color(0xFF059669)
                                                "PROXY" -> Color(0xFF7C3AED)
                                                "ERROR" -> palette.onErrorContainer
                                                "WARN" -> Color(0xFFD97706)
                                                else -> palette.onSurfaceVariant
                                            }
                                        )
                                    }
                                    Text(
                                        text = log.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = palette.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
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

    if (showPaymentDialog) {
        val paymentSheetScroll = rememberScrollState()
        val isImeVisible = WindowInsets.isImeVisible
        LaunchedEffect(isImeVisible) {
            if (isImeVisible) {
                paymentSheetScroll.animateScrollTo(paymentSheetScroll.maxValue)
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showPaymentDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .verticalScroll(paymentSheetScroll)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val activeLogin = viewModel.userEmail.ifBlank { viewModel.username }
                val activePaymentEmail = viewModel.paymentEmail.ifBlank { activeLogin }

                Text(
                    "Продление подписки",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Выберите тариф для оплаты для $activeLogin",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    "Ниже обязательно укажите email для оплаты и при оплате укажите его для моментального продления тарифа.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.onSurfaceVariant
                )

                if (activePaymentEmail.isNotBlank()) {
                    Surface(
                        onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Payment Email", activePaymentEmail))
                            android.widget.Toast.makeText(context, "Email для оплаты скопирован", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = palette.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Email для оплаты (нажмите для копирования):", style = MaterialTheme.typography.labelSmall, color = palette.onSurfaceVariant)
                                Text(activePaymentEmail, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = palette.primary)
                            }
                            Icon(Icons.Default.ContentCopy, contentDescription = "Скопировать", tint = palette.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                val tariffs = listOf(
                    Triple(1, "1 месяц", "150 ₽"),
                    Triple(3, "3 месяца", "420 ₽ (скидка 30 ₽)"),
                    Triple(6, "6 месяцев", "800 ₽ (выгодно, 133 ₽/мес)")
                )

                tariffs.forEach { (months, title, price) ->
                    val isSelected = selectedPaymentMonths == months
                    Surface(
                        onClick = { selectedPaymentMonths = months },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) palette.primaryContainer else palette.surfaceVariant,
                        border = if (isSelected) BorderStroke(2.dp, palette.primary) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(title, fontWeight = FontWeight.Bold)
                                Text(price, style = MaterialTheme.typography.bodySmall, color = palette.onSurfaceVariant)
                            }
                            RadioButton(selected = isSelected, onClick = null)
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = palette.primary)
                        Text(
                            "Оплата по СБП (Без VPN, в 1 клик через приложение банка)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                OutlinedTextField(
                    value = promoCodeInput,
                    onValueChange = { promoCodeInput = it.trim() },
                    label = { Text("Промокод (если есть)") },
                    placeholder = { Text("Введите промокод на скидку") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        viewModel.openPaymentUrl(context, selectedPaymentMonths, promoCodeInput)
                        showPaymentDialog = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Перейти к оплате СБП", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
