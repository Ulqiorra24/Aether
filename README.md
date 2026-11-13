# Aether (Android x-ui VPN Client)

[![Download APK](https://img.shields.io/badge/Download-APK_v0.6-2EA043?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Ulqiorra24/Aether/releases/latest)
[![Release](https://img.shields.io/github/v/release/Ulqiorra24/Aether?style=for-the-badge&label=Latest%20Release&color=0969DA)](https://github.com/Ulqiorra24/Aether/releases/latest)

> 📦 **[Скачать Aether APK (v0.6)](https://github.com/Ulqiorra24/Aether/releases/latest)** — официальный готов к установке файл релиза для Android.

---

Android-клиент подписок x-ui / 3x-ui. VPN реализован через Xray-core
(AndroidLibXrayLite) и встроенный TUN inbound.

## Что нового в v0.6

- 💳 **Интеграция с Lava.top**:
  - Генерация ссылок оплаты подписки через Lava.top (приём карт РФ, СБП, СберПэй, Т-Пэй).
  - Автоматическое продление срока подписки в панели 3X-UI при поступлении Webhook от Lava.top.
- 🛠️ **Стабильность работы**:
  - Оптимизированный модуль обфускатора хостов `SecretObfuscator`.
  - Повышенная безопасность сетевых запросов.

## Что было в v0.2 – v0.5

- 🔐 **Личный кабинет и авторизация**: вход, регистрация, активация подписки.
- 🔀 **Расширенная маршрутизация**: исключение LAN и российских сайтов/IP.
- 📊 **Логи трафика**: просмотр TCP/UDP соединений.
- 🎨 **Современный UI (Material 3)**.

## Подключение

1. Выполните вход, регистрацию или активацию аккаунта.
2. Выберите узел VLESS + REALITY.
3. Нажмите кнопку подключения и подтвердите системное разрешение Android.

## Сборка и проверки

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Release APK: `app/build/outputs/apk/release/app-release.apk`. В GitHub Releases файл называется `aether.apk`.

## VPN-ядро

Используется официальный [AndroidLibXrayLite v26.11.9](https://github.com/2dust/AndroidLibXrayLite/releases/tag/v26.11.9).
