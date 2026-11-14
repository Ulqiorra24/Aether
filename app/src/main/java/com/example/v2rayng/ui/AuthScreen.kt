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
    isLoading: Boolean,
    errorMessage: String?,
    onLogin: (email: String, pass: String) -> Unit,
    onRegister: (email: String, pass: String, paymentEmail: String) -> Unit,
    onActivate: (email: String, subId: String, pass: String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Login, 1: Register, 2: Activate

    var email by remember { mutableStateOf("") }
    var paymentEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var subId by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var localError by remember { mutableStateOf<String?>(null) }

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
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Логотип и Название
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

            Spacer(Modifier.height(28.dp))

            // Форма авторизации в карточке
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Переключатель вкладок
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val tabs = listOf("Вход", "Регистрация", "Активация")
                        tabs.forEachIndexed { index, label ->
                            SegmentedButton(
                                selected = selectedTab == index,
                                onClick = {
                                    selectedTab = index
                                    localError = null
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size)
                            ) {
                                Text(label, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }

                    val displayError = localError ?: errorMessage
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

                    // Поле Email (логин)
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            localError = null
                        },
                        label = { Text("Email (логин)") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Если выбран шаг "Активация" - поле ID подписки
                    if (selectedTab == 2) {
                        OutlinedTextField(
                            value = subId,
                            onValueChange = {
                                subId = it
                                localError = null
                            },
                            label = { Text("ID подписки / Ключ") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Поле Пароль
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            localError = null
                        },
                        label = {
                            Text(if (selectedTab == 2) "Новый пароль" else "Пароль")
                        },
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
                            imeAction = if (selectedTab == 1) ImeAction.Next else ImeAction.Done
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Поля для Регистрации
                    if (selectedTab == 1) {
                        OutlinedTextField(
                            value = paymentEmail,
                            onValueChange = {
                                paymentEmail = it
                                localError = null
                            },
                            label = { Text("Email для оплаты (необязательно)") },
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
                    }

                    Spacer(Modifier.height(4.dp))

                    // Главная кнопка действия
                    Button(
                        onClick = {
                            when (selectedTab) {
                                0 -> {
                                    if (email.isBlank() || password.isBlank()) {
                                        localError = "Заполните Email и Пароль"
                                        return@Button
                                    }
                                    onLogin(email, password)
                                }
                                1 -> {
                                    if (email.isBlank() || password.isBlank()) {
                                        localError = "Заполните Email и Пароль"
                                        return@Button
                                    }
                                    if (password != confirmPassword) {
                                        localError = "Пароли не совпадают"
                                        return@Button
                                    }
                                    onRegister(email, password, paymentEmail)
                                }
                                2 -> {
                                    if (email.isBlank() || subId.isBlank() || password.isBlank()) {
                                        localError = "Заполните Email, ID подписки и Новый пароль"
                                        return@Button
                                    }
                                    onActivate(email, subId, password)
                                }
                            }
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = palette.onPrimary
                            )
                        } else {
                            Text(
                                text = when (selectedTab) {
                                    0 -> "Войти"
                                    1 -> "Зарегистрироваться"
                                    else -> "Активировать аккаунт"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
