# Aether (Android x-ui VPN Client)

[![Download APK](https://img.shields.io/badge/Download-APK_v0.8.9-2EA043?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Ulqiorra24/Aether/releases/latest)
[![Release](https://img.shields.io/github/v/release/Ulqiorra24/Aether?style=for-the-badge&label=Latest%20Release&color=0969DA)](https://github.com/Ulqiorra24/Aether/releases/latest)

> 📦 **[Скачать Aether APK (v0.8.9)](https://github.com/Ulqiorra24/Aether/releases/latest)** — официальный готов к установке файл релиза для Android.

---

Android-клиент подписок x-ui / 3x-ui. VPN реализован через Xray-core (AndroidLibXrayLite) и встроенный TUN inbound.

## Что нового в v0.8.9

- 📊 **Детальные логи трафика в реальном времени**: Отслеживание всех входящих TCP/UDP пакетов, DNS-запросов и маршрутов (`PROXY` / `DIRECT` / `BLOCK`).
- 🔐 **Исправление регистрации и подтверждения**: Исправлена регистрация из приложения и повторный вызов окна ввода 6-значного кода из письма при закрытии окна.
- ⏳ **Защита от запуска при истекшей подписке**: Если подписка завершилась (`0 дней`), запуск VPN блокируется и выводится окно с кнопкой продления.
- 🎯 **Точный расчёт дней**: Автоматическая синхронизация оставшихся дней подписки с сервера 3x-ui без двойного начисления.

---

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
