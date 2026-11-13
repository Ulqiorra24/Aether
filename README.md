# Aether (Android x-ui VPN Client)

[![Download APK](https://img.shields.io/badge/Download-APK_v0.5-2EA043?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Ulqiorra24/Aether/releases/latest)
[![Release](https://img.shields.io/github/v/release/Ulqiorra24/Aether?style=for-the-badge&label=Latest%20Release&color=0969DA)](https://github.com/Ulqiorra24/Aether/releases/latest)

> 📦 **[Скачать Aether APK (v0.5)](https://github.com/Ulqiorra24/Aether/releases/latest)** — официальный готов к установке файл релиза для Android.

---

Android-клиент подписок x-ui / 3x-ui. VPN реализован через Xray-core
(AndroidLibXrayLite) и встроенный TUN inbound.

## Что нового в v0.5

- 🛠️ **Исправление обфускатора адреса сервера**:
  - Исправлен зашифрованный массив байт хоста `SecretObfuscator`, предотвращающий ошибку `no protocol` при авторизации.
  - Добавлен автоматический юнит-тест проверки декодирования адреса сервера.
- 🔐 **Личный кабинет и авторизация**:
  - Вход по Email (логин) и Паролю.
  - Регистрация новых пользователей с автоматической выдачей подписки.
  - Активация существующих подписок по Email + Sub ID с назначением пароля.
- 🔀 **Расширенная маршрутизация**:
  - Исключение локальной сети (LAN).
  - Исключение российских сайтов (`.ru`, `.рф`, `.su`, госресурсов) и IP-адресов РФ мимо VPN.
- 📊 **Логи трафика**:
  - Просмотр TCP/UDP соединений, DIRECT/PROXY статусов с возможностью копирования и очистки.

## Что было в v0.2 – v0.4

- 🎨 **Новый современный UI (Material 3)**: нижняя панель навигации (Главная, Подписки, Настройки), фоновые градиенты и обновлённая цветовая палитра.
- 🌐 **Удобный выбор серверов**: модальная шторка (ModalBottomSheet) со списком узлов и проверкой пинга (задержки).
- 🔑 **Управление подписками**: добавление и удаление аккаунтов с подтверждением, переключение подписок.

## Подключение

1. Выполните вход, регистрацию или активацию аккаунта.
2. Выберите узел VLESS + REALITY.
3. Нажмите кнопку подключения и подтвердите системное разрешение Android.
4. Статус «Подключено» появляется после HTTPS-проверки через выбранный сервер.

## Сборка и проверки

Требуются JDK 17+, Android SDK 37 и доступ к Maven/Google/GitHub.

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Release APK: `app/build/outputs/apk/release/app-release.apk`. В GitHub Releases файл называется `aether.apk`.

## VPN-ядро

Используется официальный [AndroidLibXrayLite v26.11.9](https://github.com/2dust/AndroidLibXrayLite/releases/tag/v26.11.9).
