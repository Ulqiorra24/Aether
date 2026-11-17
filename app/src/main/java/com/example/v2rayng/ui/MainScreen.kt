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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
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
    val serversScroll = rememberScrollState()
    val profileScroll = rememberScrollState()
    val settingsScroll = rememberScrollState()
    val scrollState = when (selectedTab) { 1 -> serversScroll; 2 -> profileScroll; 3 -> settingsScroll; else -> homeScroll }
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
    LaunchedEffect(selectedTab) {
        if (selectedTab == 1 && viewModel.nodes.isNotEmpty()) {
            viewModel.pingNodes()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
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
    var showSplitTunnelingDialog by rememberSaveable { mutableStateOf(false) }
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
                listOf(Color(0xFF536DFE).copy(alpha = if (dark) 0.15f else 0.05f), Color.Transparent),
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
                NavigationBar(
                    containerColor = Color.Transparent,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    modifier = Modifier.height(64.dp)
                ) {
                    val tabs = listOf(
                        "Главная" to Icons.Default.Home,
                        "Серверы" to Icons.Default.Dns,
                        "Профиль" to Icons.Default.Person,
                        "Настройки" to Icons.Default.Settings
                    )
                    tabs.forEachIndexed { index, (title, icon) ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp)) },
                            label = { Text(title, maxLines = 1, fontSize = 10.sp) }
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                        if (viewModel.isConnected) Color(0xFF4DEEEA) else palette.onSurfaceVariant, CircleShape))
                    Text(
                        if (viewModel.isConnected) "Соединение защищено" else "Ваше личное пространство",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            
            // Animation States
            val rotation1 = remember { Animatable(0f) }
            val rotation2 = remember { Animatable(360f) }
            val rotation3 = remember { Animatable(0f) }
            
            // Define ring colors matching the concept
            val cyanColor = Color(0xFF4DEEEA)
            val purpleColor = Color(0xFFC084FC)
            
            LaunchedEffect(viewModel.isConnecting, viewModel.isConnected) {
                val speedMultiplier = when {
                    viewModel.isConnecting -> 4f
                    viewModel.isConnected -> 1.5f
                    else -> 1f
                }
                
                launch {
                    rotation1.animateTo(
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween((16000 / speedMultiplier).toInt(), easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        )
                    )
                }
                launch {
                    rotation2.animateTo(
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween((22000 / speedMultiplier).toInt(), easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        )
                    )
                }
                launch {
                    rotation3.animateTo(
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween((30000 / speedMultiplier).toInt(), easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        )
                    )
                }
            }
            
            Box(
                modifier = Modifier.size(240.dp).padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Radial Glow
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    cyanColor.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                                // Outer Orbit 3
                val ring3Scale = if (viewModel.isConnecting) 0.95f else 1f
                Box(
                    modifier = Modifier
                        .size(200.dp) // Adjusted to fit screen
                        .scale(ring3Scale)
                        .rotate(rotation3.value)
                        .border(
                            width = 1.dp,
                            color = purpleColor.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-4).dp)
                            .size(8.dp)
                            .background(purpleColor, CircleShape)
                            .shadow(10.dp, CircleShape, ambientColor = purpleColor, spotColor = purpleColor)
                    )
                }
                
                // Middle Orbit 2
                val ring2Scale = if (viewModel.isConnecting) 0.92f else 1f
                Canvas(
                    modifier = Modifier
                        .size(172.dp) // Adjusted to fit screen
                        .scale(ring2Scale)
                        .rotate(rotation2.value)
                ) {
                    val brush = Brush.linearGradient(
                        colors = listOf(cyanColor.copy(alpha = 0.85f), Color(0xFFA855F7).copy(alpha = 0.1f), purpleColor.copy(alpha = 0.85f))
                    )
                    drawArc(
                        brush = brush,
                        startAngle = 0f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 1.5f * density, cap = StrokeCap.Round)
                    )
                }
                
                // Inner Orbit 1
                val ring1Scale = if (viewModel.isConnecting) 0.90f else 1f
                Canvas(
                    modifier = Modifier
                        .size(144.dp) // Adjusted to fit screen
                        .scale(ring1Scale)
                        .rotate(rotation1.value)
                ) {
                    val brush = Brush.linearGradient(
                        colors = listOf(cyanColor, Color(0xFFA855F7).copy(alpha = 0f))
                    )
                    drawArc(
                        brush = brush,
                        startAngle = 45f,
                        sweepAngle = 200f,
                        useCenter = false,
                        style = Stroke(width = 2.2f * density, cap = StrokeCap.Round)
                    )
                    drawCircle(
                        color = cyanColor,
                        radius = 3f * density,
                        center = Offset(size.width / 2f, 0f)
                    )
                }

                // Core Button
                Surface(
                    onClick = {
                        if (!viewModel.isConnected && !viewModel.isConnecting) {
                            val intent = VpnService.prepare(context)
                            if (intent != null) vpnPermissionLauncher.launch(intent)
                            else viewModel.startVpnService(context)
                        } else viewModel.toggleVpn(context)
                    },
                    enabled = !viewModel.isStopping,
                    modifier = Modifier
                        .size(96.dp) // Adjusted to fit screen
                        .shadow(30.dp, RoundedCornerShape(26.dp), ambientColor = Color.Black.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(26.dp),
                    color = Color.Transparent, // We use background for gradient
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFF2A1E47), Color(0xFF17112B))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Inner glow
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(cyanColor.copy(alpha = 0f), purpleColor.copy(alpha = 0f), cyanColor.copy(alpha = 0f)) // Invisible for now, but ready for pulses
                                    )
                                )
                        )
                        
                        if (viewModel.isConnecting || viewModel.isStopping) {
                            CircularProgressIndicator(Modifier.size(48.dp), strokeWidth = 2.dp, color = cyanColor)
                        } else {
                            // Minimalist Logo from Concept
                            Canvas(modifier = Modifier.size(48.dp).shadow(12.dp, ambientColor = cyanColor.copy(alpha = 0.25f), spotColor = cyanColor.copy(alpha = 0.25f))) {
                                val w = size.width
                                val h = size.height
                                fun rx(x: Float) = (x / 100f) * w
                                fun ry(y: Float) = (y / 100f) * h
                                
                                val logoGrad = Brush.linearGradient(
                                    colors = listOf(Color(0xFF4DEEEA), Color(0xFF5EA8FA), Color(0xFFC084FC))
                                )
                                val stroke = Stroke(width = 5.5f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                
                                // Shackle (animates up when disconnected)
                                val shackleYOffset = if (viewModel.isConnected) 0f else -3.5f * density
                                withTransform({ translate(top = shackleYOffset) }) {
                                    val shacklePath = Path().apply {
                                        moveTo(rx(36f), ry(33f))
                                        lineTo(rx(36f), ry(24.5f))
                                        cubicTo(rx(36f), ry(16.4919f), rx(42.4919f), ry(10f), rx(50.5f), ry(10f))
                                        lineTo(rx(49.5f), ry(10f)) // Small trick for H50.5 H49.5
                                        cubicTo(rx(57.5081f), ry(10f), rx(64f), ry(16.4919f), rx(64f), ry(24.5f))
                                        lineTo(rx(64f), ry(33f))
                                    }
                                    drawPath(shacklePath, brush = logoGrad, style = stroke)
                                }
                                
                                // Shield
                                val shieldPath = Path().apply {
                                    moveTo(rx(50f), ry(26f))
                                    lineTo(rx(76f), ry(36f))
                                    lineTo(rx(76f), ry(54.5f))
                                    cubicTo(rx(76f), ry(72f), rx(65f), ry(84.5f), rx(50f), ry(91f))
                                    cubicTo(rx(35f), ry(84.5f), rx(24f), ry(72f), rx(24f), ry(54.5f))
                                    lineTo(rx(24f), ry(36f))
                                    close()
                                }
                                drawPath(shieldPath, color = Color(0xFF140F26))
                                drawPath(shieldPath, brush = logoGrad, style = stroke)
                                
                                // Letter A
                                val letterPath = Path().apply {
                                    moveTo(rx(38f), ry(71f))
                                    lineTo(rx(50f), ry(42.5f))
                                    lineTo(rx(62f), ry(71f))
                                    moveTo(rx(41.8f), ry(63f))
                                    lineTo(rx(58.2f), ry(63f))
                                }
                                drawPath(letterPath, brush = logoGrad, style = stroke)
                            }
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    when {
                        viewModel.isConnecting -> "Подключаемся…"
                        viewModel.isStopping -> "Отключаемся…"
                        viewModel.isConnected -> "Вы подключены"
                        else -> "Готовы к подключению"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Text(viewModel.statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.onSurfaceVariant, textAlign = TextAlign.Center)

                val days = viewModel.subscriptionDaysRemaining
                if (viewModel.username.isNotBlank()) {
                    val isYellow = days != null && days in 3..5
                    val isRed = days != null && days <= 2

                    val chipBg = when {
                        isRed -> Color(0xFFEF4444).copy(alpha = 0.12f)
                        isYellow -> Color(0xFFF59E0B).copy(alpha = 0.12f)
                        else -> palette.primaryContainer.copy(alpha = 0.6f)
                    }
                    val chipBorder = when {
                        isRed -> Color(0xFFEF4444).copy(alpha = 0.4f)
                        isYellow -> Color(0xFFF59E0B).copy(alpha = 0.4f)
                        else -> palette.primary.copy(alpha = 0.3f)
                    }
                    val textColor = when {
                        isRed -> Color(0xFFDC2626)
                        isYellow -> Color(0xFFD97706)
                        else -> palette.primary
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Surface(
                            onClick = { showPaymentDialog = true },
                            shape = RoundedCornerShape(20.dp),
                            color = chipBg,
                            border = BorderStroke(1.dp, chipBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(textColor, CircleShape)
                                )
                                Text(
                                    text = when {
                                        days == 0 -> "Подписка истекла (0 дн.)"
                                        days != null -> "Осталось дней: $days"
                                        else -> "Подписка активна"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                if (isYellow || isRed) {
                                    Surface(
                                        onClick = { showPaymentDialog = true },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isRed) Color(0xFFEF4444) else Color(0xFFF59E0B)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ShoppingCart,
                                                contentDescription = "Продлить подписку",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "Продлить",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Surface(
                            onClick = { viewModel.fetchSubscription() },
                            shape = RoundedCornerShape(20.dp),
                            color = chipBg,
                            border = BorderStroke(1.dp, chipBorder),
                            enabled = !viewModel.isLoading
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (viewModel.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = textColor
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Обновить подписку",
                                        tint = textColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            val selectedNode = viewModel.nodes.getOrNull(viewModel.selectedNodeIndex)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = palette.surface.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, palette.outlineVariant),
                modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(shape = RoundedCornerShape(14.dp), color = palette.primaryContainer) {
                                Icon(
                                    Icons.Default.SwapVert,
                                    contentDescription = null,
                                    modifier = Modifier.padding(10.dp).size(22.dp),
                                    tint = palette.onPrimaryContainer
                                )
                            }
                            Column {
                                Text(
                                    text = "СКОРОСТЬ СЕТИ",
                                    style = MaterialTheme.typography.labelSmall,
                                    letterSpacing = 1.5.sp,
                                    color = palette.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (selectedNode != null) "Сервер: ${selectedNode.name}" else "Сервер не выбран",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            onClick = { selectedTab = 1 },
                            shape = RoundedCornerShape(12.dp),
                            color = palette.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Серверы",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = palette.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = palette.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowDownward,
                                            contentDescription = "Загрузка",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.padding(4.dp).size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = "Загрузка",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = viewModel.downSpeedStr,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = palette.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF3B82F6).copy(alpha = 0.15f)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowUpward,
                                            contentDescription = "Отдача",
                                            tint = Color(0xFF3B82F6),
                                            modifier = Modifier.padding(4.dp).size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = "Отдача",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = viewModel.upSpeedStr,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = palette.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF8B5CF6).copy(alpha = 0.15f)
                                    ) {
                                        Icon(
                                            Icons.Default.Schedule,
                                            contentDescription = "Время подключения",
                                            tint = Color(0xFF8B5CF6),
                                            modifier = Modifier.padding(4.dp).size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = "Время",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = viewModel.connectionDurationStr,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }


                }
                1 -> {
                    Text(
                        "Серверы",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = palette.surface.copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, palette.outlineVariant),
                        modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        if (viewModel.showProtocols) "Все серверы (Pro)" else "Выбор локации",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        if (viewModel.nodes.isNotEmpty())
                                            "${viewModel.nodes.size} узлов доступно · ${viewModel.username.ifBlank { viewModel.userEmail }}"
                                        else
                                            "Подписка не загружена",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.pingNodes() },
                                    enabled = viewModel.nodes.isNotEmpty()
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Проверить задержку",
                                        tint = palette.primary
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = !viewModel.showProtocols,
                                    onClick = { viewModel.updateShowProtocols(false) },
                                    label = { Text("Умный режим", fontWeight = FontWeight.SemiBold) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                FilterChip(
                                    selected = viewModel.showProtocols,
                                    onClick = { viewModel.updateShowProtocols(true) },
                                    label = { Text("Все протоколы (Pro)", fontWeight = FontWeight.SemiBold) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            if (viewModel.nodes.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = palette.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            "Серверы не найдены. Добавьте подписку или ключ в профиле.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = palette.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                        Button(
                                            onClick = { selectedTab = 2 },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Перейти в профиль")
                                        }
                                    }
                                }
                            } else if (!viewModel.showProtocols) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val bestIdx = viewModel.getBestNodeIndex()
                                    val bestNode = viewModel.nodes.getOrNull(bestIdx)
                                    val isAutoSelected = viewModel.isAutoSelect
                                    val autoLatency = bestNode?.latencyMs ?: -1L
                                    val autoPingColor = when {
                                        autoLatency in 1..59 -> Color(0xFF10B981)
                                        autoLatency in 60..120 -> Color(0xFFF59E0B)
                                        autoLatency > 120 -> Color(0xFFEF4444)
                                        else -> palette.onSurfaceVariant.copy(alpha = 0.4f)
                                    }

                                    Surface(
                                        onClick = { viewModel.selectAutoFastest(context) },
                                        shape = RoundedCornerShape(18.dp),
                                        color = if (isAutoSelected) palette.primaryContainer else palette.surfaceVariant.copy(alpha = 0.7f),
                                        border = if (isAutoSelected) BorderStroke(1.5.dp, palette.primary) else null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = autoPingColor,
                                                modifier = Modifier.size(10.dp)
                                            ) {}

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "⚡ Автовыбор (Быстрейший)",
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isAutoSelected) palette.onPrimaryContainer else palette.onSurface
                                                )
                                                Text(
                                                    text = if (autoLatency > 0)
                                                        "Лучшая задержка: $autoLatency мс (${bestNode?.name ?: ""})"
                                                    else
                                                        "Задержка проверяется…",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (isAutoSelected) palette.onPrimaryContainer.copy(alpha = 0.8f) else palette.onSurfaceVariant
                                                )
                                            }

                                            RadioButton(
                                                selected = isAutoSelected,
                                                onClick = null
                                            )
                                        }
                                    }

                                    HorizontalDivider(
                                        color = palette.outlineVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )

                                    val groups = viewModel.getLocationGroups()
                                    groups.forEach { group ->
                                        val pingColor = when {
                                            group.bestLatencyMs in 1..59 -> Color(0xFF10B981)
                                            group.bestLatencyMs in 60..120 -> Color(0xFFF59E0B)
                                            group.bestLatencyMs > 120 -> Color(0xFFEF4444)
                                            else -> palette.onSurfaceVariant.copy(alpha = 0.4f)
                                        }

                                        Surface(
                                            onClick = { viewModel.selectNode(group.bestNodeIndex, context) },
                                            shape = RoundedCornerShape(18.dp),
                                            color = if (group.isSelected) palette.primaryContainer else palette.surfaceVariant.copy(alpha = 0.7f),
                                            border = if (group.isSelected) BorderStroke(1.5.dp, palette.primary) else null,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = pingColor,
                                                    modifier = Modifier.size(10.dp)
                                                ) {}

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = group.locationName,
                                                        fontWeight = if (group.isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (group.isSelected) palette.onPrimaryContainer else palette.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = if (group.bestLatencyMs > 0)
                                                            "${group.bestLatencyMs} мс · ${group.nodes.size} серв."
                                                        else
                                                            "Задержка не проверена · ${group.nodes.size} серв.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = if (group.isSelected) palette.onPrimaryContainer.copy(alpha = 0.8f) else palette.onSurfaceVariant
                                                    )
                                                }

                                                RadioButton(
                                                    selected = group.isSelected,
                                                    onClick = null
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    viewModel.nodes.forEachIndexed { index, node ->
                                        val isSelected = index == viewModel.selectedNodeIndex && !viewModel.isAutoSelect
                                        val pingColor = when {
                                            node.latencyMs in 1..59 -> Color(0xFF10B981)
                                            node.latencyMs in 60..120 -> Color(0xFFF59E0B)
                                            node.latencyMs > 120 -> Color(0xFFEF4444)
                                            else -> palette.onSurfaceVariant.copy(alpha = 0.4f)
                                        }

                                        Surface(
                                            onClick = { viewModel.selectNode(index, context) },
                                            shape = RoundedCornerShape(18.dp),
                                            color = if (isSelected) palette.primaryContainer else palette.surfaceVariant.copy(alpha = 0.7f),
                                            border = if (isSelected) BorderStroke(1.5.dp, palette.primary) else null,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = pingColor,
                                                    modifier = Modifier.size(10.dp)
                                                ) {}

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = node.name,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) palette.onPrimaryContainer else palette.onSurface,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = if (node.latencyMs > 0) "${node.latencyMs} мс" else "Задержка не проверена",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = if (isSelected) palette.onPrimaryContainer.copy(alpha = 0.8f) else palette.onSurfaceVariant
                                                    )
                                                }

                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = null
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    Text("Профиль", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth())
                    Surface(shape = RoundedCornerShape(24.dp), color = palette.surface.copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, palette.outlineVariant), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Текущий аккаунт", color = palette.onSurfaceVariant,
                                style = MaterialTheme.typography.labelLarge)
                            Text(viewModel.username.ifBlank { viewModel.userEmail.ifBlank { "Подписка не добавлена" } },
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                            val displayPaymentEmail = viewModel.paymentEmail.ifBlank { viewModel.username.ifBlank { viewModel.userEmail } }
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
                                                    editPaymentEmail = viewModel.paymentEmail.ifBlank { viewModel.username.ifBlank { viewModel.userEmail } }
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
                3 -> {
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

                            HorizontalDivider(color = palette.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 14.dp))

                            // Кнопка: Раздельное туннелирование (Фильтр приложений)
                            Surface(
                                onClick = {
                                    viewModel.loadInstalledApps(context)
                                    showSplitTunnelingDialog = true
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Surface(shape = RoundedCornerShape(12.dp), color = palette.primaryContainer) {
                                        Icon(Icons.Default.Apps, null, Modifier.padding(10.dp).size(22.dp), tint = palette.onPrimaryContainer)
                                    }
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("Фильтр приложений", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        val perAppSummary = when (viewModel.perAppMode) {
                                            "ALLOW_SELECTED" -> "Только выбранные (${viewModel.perAppPackages.size})"
                                            "DISALLOW_SELECTED" -> "Исключенные (${viewModel.perAppPackages.size})"
                                            else -> "Отключено (все приложения)"
                                        }
                                        Text(perAppSummary, style = MaterialTheme.typography.bodySmall, color = palette.onSurfaceVariant)
                                    }
                                    Icon(Icons.Default.ChevronRight, null, tint = palette.onSurfaceVariant)
                                }
                            }

                            HorizontalDivider(color = palette.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 14.dp))

                            // Кнопка: Добавить плитку в шторку Android
                            Surface(
                                onClick = {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        val sbm = context.getSystemService(android.app.StatusBarManager::class.java)
                                        val componentName = android.content.ComponentName(context, com.example.v2rayng.vpn.AetherTileService::class.java)
                                        sbm?.requestAddTileService(
                                            componentName,
                                            "Aether VPN",
                                            android.graphics.drawable.Icon.createWithResource(context, com.example.v2rayng.R.mipmap.aether),
                                            { _ -> },
                                            { _ -> }
                                        )
                                    } else {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Потяните шторку вниз, нажмите 'Редактировать' (✏️) и перетащите 'Aether VPN'",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Surface(shape = RoundedCornerShape(12.dp), color = palette.secondaryContainer) {
                                        Icon(Icons.Default.PowerSettingsNew, null, Modifier.padding(10.dp).size(22.dp), tint = palette.onSecondaryContainer)
                                    }
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("Плитка в шторке Android", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        Text("Добавить кнопку в быстрые настройки телефона", style = MaterialTheme.typography.bodySmall, color = palette.onSurfaceVariant)
                                    }
                                    Icon(Icons.Default.ChevronRight, null, tint = palette.onSurfaceVariant)
                                }
                            }

                            HorizontalDivider(color = palette.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 14.dp))

                            // Кнопка: Показывать протоколы (Pro)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Surface(shape = RoundedCornerShape(12.dp), color = palette.primaryContainer) {
                                        Icon(Icons.Default.Dns, null, Modifier.padding(10.dp).size(22.dp), tint = palette.onPrimaryContainer)
                                    }
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("Показывать протоколы (Pro)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        Text("Детали транспорта (VLESS, gRPC, XHTTP) и полный список узлов", style = MaterialTheme.typography.bodySmall, color = palette.onSurfaceVariant)
                                    }
                                    Switch(
                                        checked = viewModel.showProtocols,
                                        onCheckedChange = { viewModel.updateShowProtocols(it) }
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

    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Редактирование профиля") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
        val notesScrollState = rememberScrollState()
        AlertDialog(
            onDismissRequest = { if (!viewModel.isDownloadingUpdate) viewModel.dismissUpdate() },
            title = { Text("Доступно обновление (${updateInfo.tagName})") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Найдена новая версия Aether. Хотите скачать и обновить приложение прямо сейчас?")

                    if (viewModel.isDownloadingUpdate) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val downloaded = viewModel.updateDownloadedBytes
                                val total = viewModel.updateTotalBytes
                                if (total > 0L) {
                                    val fraction = (downloaded.toDouble() / total).coerceIn(0.0, 1.0)
                                    LinearProgressIndicator(
                                        progress = { fraction.toFloat() },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        text = "${(fraction * 100).toInt()}% · ${android.text.format.Formatter.formatShortFileSize(context, downloaded)} из ${android.text.format.Formatter.formatShortFileSize(context, total)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                } else {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    Text(
                                        text = "Загрузка: ${android.text.format.Formatter.formatShortFileSize(context, downloaded)}…",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                    viewModel.updateDownloadError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    if (updateInfo.releaseNotes.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .verticalScroll(notesScrollState)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = updateInfo.releaseNotes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.downloadAndInstallUpdate(context, updateInfo) },
                    enabled = !viewModel.isDownloadingUpdate
                ) {
                    Text(if (viewModel.isDownloadingUpdate) "Загрузка…" else "Скачать и обновить")
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
            onDismissRequest = {
                if (!viewModel.isPaymentLoading) {
                    showPaymentDialog = false
                    viewModel.resetPaymentError()
                }
            },
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
                val activeLogin = viewModel.username.ifBlank { viewModel.userEmail }
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
                    onValueChange = {
                        promoCodeInput = it.trim()
                        viewModel.resetPaymentError()
                    },
                    label = { Text("Промокод (если есть)") },
                    placeholder = { Text("Введите промокод на скидку") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                viewModel.paymentError?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = {
                        viewModel.openPaymentUrl(context, selectedPaymentMonths, promoCodeInput) {
                            showPaymentDialog = false
                        }
                    },
                    enabled = !viewModel.isPaymentLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (viewModel.isPaymentLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Создание счета…", fontWeight = FontWeight.Bold)
                    } else {
                        Text("Перейти к оплате СБП", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showSplitTunnelingDialog) {
        ModalBottomSheet(
            onDismissRequest = { showSplitTunnelingDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(shape = RoundedCornerShape(12.dp), color = palette.primaryContainer) {
                            Icon(Icons.Default.Apps, null, Modifier.padding(8.dp).size(20.dp), tint = palette.onPrimaryContainer)
                        }
                        Text("Фильтр приложений", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    "Выберите режим раздельного туннелирования для приложений Android:",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val modes = listOf(
                        "OFF" to "Все приложения (Отключено)",
                        "ALLOW_SELECTED" to "Маршрутизировать только выбранные",
                        "DISALLOW_SELECTED" to "Исключить выбранные из VPN"
                    )
                    modes.forEach { (modeKey, modeLabel) ->
                        val isSel = viewModel.perAppMode == modeKey
                        Surface(
                            onClick = { viewModel.updatePerAppMode(modeKey, context) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSel) palette.primaryContainer else palette.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    modeLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) palette.onPrimaryContainer else palette.onSurface
                                )
                                RadioButton(selected = isSel, onClick = null)
                            }
                        }
                    }
                }

                if (viewModel.perAppMode != "OFF") {
                    OutlinedTextField(
                        value = viewModel.appSearchQuery,
                        onValueChange = { viewModel.appSearchQuery = it },
                        placeholder = { Text("Поиск приложений…") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (viewModel.appSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.appSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Очистить")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { viewModel.selectAllApps(context) }) {
                            Text("Выбрать все")
                        }
                        TextButton(onClick = { viewModel.clearSelectedApps(context) }) {
                            Text("Снять все (${viewModel.perAppPackages.size})")
                        }
                    }

                    if (viewModel.isAppsLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val query = viewModel.appSearchQuery.trim().lowercase()
                        val filtered = viewModel.installedApps.filter {
                            query.isEmpty() || it.appName.lowercase().contains(query) || it.packageName.lowercase().contains(query)
                        }

                        val appListScroll = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(appListScroll),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            filtered.forEach { app ->
                                val isChecked = viewModel.perAppPackages.contains(app.packageName)
                                Surface(
                                    onClick = { viewModel.toggleAppPackage(app.packageName, context) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isChecked) palette.primaryContainer.copy(alpha = 0.5f) else palette.surfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        AppIconImage(app.icon, modifier = Modifier.size(36.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                app.appName,
                                                fontWeight = FontWeight.Medium,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                app.packageName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = null
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppIconImage(drawable: android.graphics.drawable.Drawable?, modifier: Modifier = Modifier) {
    val bitmap = remember(drawable) {
        if (drawable == null) null
        else {
            val bmp = android.graphics.Bitmap.createBitmap(
                drawable.intrinsicWidth.coerceAtLeast(1),
                drawable.intrinsicHeight.coerceAtLeast(1),
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp.asImageBitmap()
        }
    }
    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier
        )
    } else {
        Icon(
            Icons.Default.Android,
            contentDescription = null,
            modifier = modifier
        )
    }
}
