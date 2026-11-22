# Aether (Android x-ui VPN Client)

[![Download APK](https://img.shields.io/badge/Download-APK_v0.8.8-2EA043?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Ulqiorra24/Aether/releases/latest)
[![Release](https://img.shields.io/github/v/release/Ulqiorra24/Aether?style=for-the-badge&label=Latest%20Release&color=0969DA)](https://github.com/Ulqiorra24/Aether/releases/latest)

> 📦 **[Скачать Aether APK (v0.8.8)](https://github.com/Ulqiorra24/Aether/releases/latest)** — официальный готов к установке файл релиза для Android.

---

Android-клиент подписок x-ui / 3x-ui. VPN реализован через Xray-core (AndroidLibXrayLite) и встроенный TUN inbound.

## Что нового в v0.8.8

- 🎨 **Иконка в Quick Settings Tile**: В шторку Android добавлена фирменная векторная иконка Aether.
- 🛡️ **Обход РФ по умолчанию (`bypassRu`)**: Нативный доступ мимо VPN для банков (Сбер, ВТБ, Альфа, Т-Банк), маркетплейсов (Ozon, WB, Яндекс Маркет), операторов связи (МТС, Теле2, Ростелеком), сервисов Яндекса, Госуслуг, 1С и Битрикс24.
- ⚡ **Блокировка рекламы & AdGuard DNS**: Интегрирован AdGuard DNS (`94.140.14.14`) для автоматической фильтрации баннеров, трекеров и вредоносных сайтов.
- 🔒 **Защита от ТСПУ & Kill Switch**: Включены Kill Switch и шифрование всех DNS внутри туннеля Xray для блокировки подмены DNS и утечек трафика.
- 🚀 **Прямое скачивание обновлений**: Загрузка новых версий APK происходит напрямую вне VPN на максимальной скорости вашего провайдера.

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
