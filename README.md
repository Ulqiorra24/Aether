# Aether (Android x-ui VPN Client)

[![Download APK](https://img.shields.io/badge/Download-APK_v0.7-2EA043?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Ulqiorra24/Aether/releases/latest)
[![Release](https://img.shields.io/github/v/release/Ulqiorra24/Aether?style=for-the-badge&label=Latest%20Release&color=0969DA)](https://github.com/Ulqiorra24/Aether/releases/latest)

> 📦 **[Скачать Aether APK (v0.7)](https://github.com/Ulqiorra24/Aether/releases/latest)** — официальный готов к установке файл релиза для Android.

---

Android-клиент подписок x-ui / 3x-ui. VPN реализован через Xray-core
(AndroidLibXrayLite) и встроенный TUN inbound.

## Что нового в v0.7

- ✉️ **Email для оплаты в профиле и при регистрации**:
  - Поле «Email для оплаты» при регистрации аккаунта.
  - Отображение «Email для оплаты» в Профиле с возможностью копирования в буфер обмена в 1 клик.
  - Кнопка редактирования (иконка Карандаша) в подписках для смены пароля и Email для оплаты.
- 💳 **Интеграция с Lava.top**:
  - Оплата продления подписки картами РФ, СБП, СберПэй, Т-Пэй.
  - Автоматическое продление срока действия в 3X-UI через Webhook.
- 🔐 **Личный кабинет и авторизация**: вход, регистрация, активация подписки.

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
