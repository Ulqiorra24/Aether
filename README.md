# Aether (Android x-ui VPN Client)

[![Download APK](https://img.shields.io/badge/Download-APK_v0.8.8-2EA043?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Ulqiorra24/Aether/releases/latest)
[![Release](https://img.shields.io/github/v/release/Ulqiorra24/Aether?style=for-the-badge&label=Latest%20Release&color=0969DA)](https://github.com/Ulqiorra24/Aether/releases/latest)

> 📦 **[Скачать Aether APK (v0.8.8)](https://github.com/Ulqiorra24/Aether/releases/latest)** — официальный готов к установке файл релиза для Android.

---

Android-клиент подписок x-ui / 3x-ui. VPN реализован через Xray-core (AndroidLibXrayLite) и встроенный TUN inbound.

## Что нового в v0.8.8

- 🏢 **Полный обход обнаружения VPN в Битрикс24 и банках**: Пакеты Битрикс24 (`com.bitrix24.android`), Сбера, ВТБ, Альфы, Т-Банка и Яндекса изолированы от системного VPN-транспорта (`addDisallowedApplication`), плашка обнаружения VPN исчезла.
- 🔔 **Векторная иконка в статус-баре и шторке**: Белый кружок `●` заменён на векторный логотип Aether в статусе VPN и шторке уведомлений.
- 🎨 **Иконка в Quick Settings Tile**: В плитках быстрых настроек Android установлена нативная векторная иконка Aether.
- ⚡ **Обход РФ, AdGuard DNS & Kill Switch**: Нативный доступ к сайтам РФ по умолчанию, автоматическая фильтрация рекламы, защита от ТСПУ и Kill Switch при сбое туннеля.
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
