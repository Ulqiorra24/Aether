package com.example.v2rayng.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthScreen(
    viewModel: MainViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Login, 1: Register, 2: With Subscription

    var loginOrEmail by remember { mutableStateOf("") }
    var login by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var subId by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var localError by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    val palette = MaterialTheme.colorScheme
    val dark = palette.background.luminance() < 0.5f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0xFF7928CA).copy(alpha = if (dark) 0.36f else 0.12f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.15f),
                        radius = size.width * 0.95f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0xFF9B2DC3).copy(alpha = if (dark) 0.22f else 0.09f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.85f),
                        radius = size.width
                    )
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically)
        ) {
            Spacer(Modifier.height(12.dp))

            // Logo & Title
            Surface(
                shape = CircleShape,
                color = palette.primaryContainer.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.4f)),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = palette.primary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Aether",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Личный кабинет VPN",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            // Auth Card
            Card(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = palette.surface.copy(alpha = 0.85f)),
                border = BorderStroke(1.dp, palette.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Segmented Tabs
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val tabs = listOf("Вход", "Регистрация", "С подпиской")
                        tabs.forEachIndexed { index, label ->
                            SegmentedButton(
                                selected = selectedTab == index,
                                onClick = {
                                    selectedTab = index
                                    localError = null
                                    viewModel.cancelRegistrationCodeStep()
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size)
                            ) {
                                Text(label, fontSize = 12.sp, maxLines = 1, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Success Message Banner
                    val bannerMsg = viewModel.authMessage
                    if (!bannerMsg.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = palette.primaryContainer.copy(alpha = 0.8f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = bannerMsg,
                                color = palette.onPrimaryContainer,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Error Message Banner
                    val displayError = localError ?: viewModel.authError
                    if (displayError != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = palette.errorContainer.copy(alpha = 0.8f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = displayError,
                                color = palette.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    val challengeId = viewModel.authChallengeId

                    when (selectedTab) {
                        // TAB 0: LOGIN (BY USERNAME OR EMAIL)
                        0 -> {
                            OutlinedTextField(
                                value = loginOrEmail,
                                onValueChange = {
                                    loginOrEmail = it
                                    localError = null
                                },
                                label = { Text("Логин или Email") },
                                placeholder = { Text("Введите ваш логин или Email") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    localError = null
                                },
                                label = { Text("Пароль") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null
                                        )
                                    }
                                },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(4.dp))

                            Button(
                                onClick = {
                                    if (loginOrEmail.isBlank() || password.isBlank()) {
                                        localError = "Заполните Логин/Email и Пароль"
                                        return@Button
                                    }
                                    viewModel.loginUser(loginOrEmail, password)
                                },
                                enabled = !viewModel.isAuthLoading,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                if (viewModel.isAuthLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp,
                                        color = palette.onPrimary
                                    )
                                } else {
                                    Text("Войти", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }

                        // TAB 1: REGISTRATION (WITH REQUIRED EMAIL & CODE VERIFICATION)
                        1 -> {
                            if (challengeId == null) {
                                OutlinedTextField(
                                    value = login,
                                    onValueChange = {
                                        login = it
                                        localError = null
                                    },
                                    label = { Text("Логин") },
                                    placeholder = { Text("Придумайте логин (латиница)") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = email,
                                    onValueChange = {
                                        email = it
                                        localError = null
                                    },
                                    label = { Text("Email (обязательно)") },
                                    placeholder = { Text("На него придет код подтверждения") },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Next
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = password,
                                    onValueChange = {
                                        password = it
                                        localError = null
                                    },
                                    label = { Text("Пароль (от 8 символов)") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Next
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = confirmPassword,
                                    onValueChange = {
                                        confirmPassword = it
                                        localError = null
                                    },
                                    label = { Text("Повторите пароль") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                    singleLine = true,
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(Modifier.height(4.dp))

                                Button(
                                    onClick = {
                                        if (login.isBlank()) {
                                            localError = "Введите логин"
                                            return@Button
                                        }
                                        if (email.isBlank() || !email.contains("@")) {
                                            localError = "Введите корректный Email"
                                            return@Button
                                        }
                                        if (password.length < 8) {
                                            localError = "Пароль должен содержать минимум 8 символов"
                                            return@Button
                                        }
                                        if (password != confirmPassword) {
                                            localError = "Пароли не совпадают"
                                            return@Button
                                        }
                                        viewModel.startRegistration(
                                            login = login,
                                            email = email,
                                            pass = password,
                                            mode = "register"
                                        )
                                    },
                                    enabled = !viewModel.isAuthLoading,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    if (viewModel.isAuthLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            strokeWidth = 2.dp,
                                            color = palette.onPrimary
                                        )
                                    } else {
                                        Text("Получить код подтверждения", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                }
                            } else {
                                // STEP 2: VERIFICATION CODE
                                Text(
                                    text = "На ваш Email ${viewModel.authPendingEmail} отправлен 6-значный код подтверждения.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                OutlinedTextField(
                                    value = verificationCode,
                                    onValueChange = {
                                        verificationCode = it.take(6)
                                        localError = null
                                    },
                                    label = { Text("Код из письма") },
                                    placeholder = { Text("например: 123456") },
                                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(Modifier.height(4.dp))

                                Button(
                                    onClick = {
                                        if (verificationCode.length < 6) {
                                            localError = "Введите 6-значный код из письма"
                                            return@Button
                                        }
                                        viewModel.confirmRegistration(challengeId, verificationCode)
                                    },
                                    enabled = !viewModel.isAuthLoading,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    if (viewModel.isAuthLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            strokeWidth = 2.dp,
                                            color = palette.onPrimary
                                        )
                                    } else {
                                        Text("Подтвердить код и войти", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                }

                                TextButton(
                                    onClick = { viewModel.cancelRegistrationCodeStep() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Изменить данные", color = palette.primary)
                                }
                            }
                        }

                        // TAB 2: WITH SUBSCRIPTION (С ПОДПИСКОЙ)
                        2 -> {
                            if (challengeId == null) {
                                OutlinedTextField(
                                    value = login,
                                    onValueChange = {
                                        login = it
                                        localError = null
                                    },
                                    label = { Text("Логин подписки в 3X-UI") },
                                    placeholder = { Text("Ваш логин в панели 3X-UI") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = subId,
                                    onValueChange = {
                                        subId = it
                                        localError = null
                                    },
                                    label = { Text("ID подписки (sub_id) / Ключ") },
                                    placeholder = { Text("например: 9s6js2edwiclhe8j") },
                                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = email,
                                    onValueChange = {
                                        email = it
                                        localError = null
                                    },
                                    label = { Text("Email (для подтверждения)") },
                                    placeholder = { Text("Email для получения кода") },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Next
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = password,
                                    onValueChange = {
                                        password = it
                                        localError = null
                                    },
                                    label = { Text("Пароль для входа (от 8 символов)") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Next
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = confirmPassword,
                                    onValueChange = {
                                        confirmPassword = it
                                        localError = null
                                    },
                                    label = { Text("Повторите пароль") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                    singleLine = true,
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(Modifier.height(4.dp))

                                Button(
                                    onClick = {
                                        if (login.isBlank() || subId.isBlank()) {
                                            localError = "Заполните Логин подписки и ID подписки"
                                            return@Button
                                        }
                                        if (email.isBlank() || !email.contains("@")) {
                                            localError = "Введите корректный Email для подтверждения"
                                            return@Button
                                        }
                                        if (password.length < 8) {
                                            localError = "Пароль должен содержать не менее 8 символов"
                                            return@Button
                                        }
                                        if (password != confirmPassword) {
                                            localError = "Пароли не совпадают"
                                            return@Button
                                        }
                                        viewModel.startRegistration(
                                            login = login,
                                            email = email,
                                            pass = password,
                                            mode = "activate",
                                            subId = subId
                                        )
                                    },
                                    enabled = !viewModel.isAuthLoading,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    if (viewModel.isAuthLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            strokeWidth = 2.dp,
                                            color = palette.onPrimary
                                        )
                                    } else {
                                        Text("Привязать подписку", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                }
                            } else {
                                // STEP 2: VERIFICATION CODE FOR ACTIVATION
                                Text(
                                    text = "На ваш Email ${viewModel.authPendingEmail} отправлен 6-значный код подтверждения привязки.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                OutlinedTextField(
                                    value = verificationCode,
                                    onValueChange = {
                                        verificationCode = it.take(6)
                                        localError = null
                                    },
                                    label = { Text("Код из письма") },
                                    placeholder = { Text("например: 123456") },
                                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(Modifier.height(4.dp))

                                Button(
                                    onClick = {
                                        if (verificationCode.length < 6) {
                                            localError = "Введите 6-значный код из письма"
                                            return@Button
                                        }
                                        viewModel.confirmRegistration(challengeId, verificationCode)
                                    },
                                    enabled = !viewModel.isAuthLoading,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    if (viewModel.isAuthLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            strokeWidth = 2.dp,
                                            color = palette.onPrimary
                                        )
                                    } else {
                                        Text("Подтвердить привязку подписки", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                }

                                TextButton(
                                    onClick = { viewModel.cancelRegistrationCodeStep() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Изменить данные", color = palette.primary)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
