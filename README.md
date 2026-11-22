# Aether (Android x-ui VPN Client)

[![Download APK](https://img.shields.io/badge/Download-APK_v0.8.9-2EA043?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Ulqiorra24/Aether/releases/latest)
[![Release](https://img.shields.io/github/v/release/Ulqiorra24/Aether?style=for-the-badge&label=Latest%20Release&color=0969DA)](https://github.com/Ulqiorra24/Aether/releases/latest)

> 📦 **[Скачать Aether APK (v0.8.9)](https://github.com/Ulqiorra24/Aether/releases/latest)** — официальный готов к установке файл релиза для Android.

---

Android-клиент подписок x-ui / 3x-ui. VPN реализован через Xray-core (AndroidLibXrayLite) и встроенный TUN inbound.

## Что нового в v0.8.9

- 🔔 **Векторная иконка в статус-баре и шторке уведомлений**: Заменен кружок `●` на векторный логотип Aether в уведомлениях и статус-баре.
- 🏢 **Полный обход обнаружения VPN в Битрикс24 и банковских ПО**: Приложения Битрикс24, Сбербанк, ВТБ, Альфа-Банк, Т-Банк и Яндекс изолированы от системного VPN-транспорта (`addDisallowedApplication`), плашка обнаружения VPN исчезла.
- 🎨 **Иконка в Quick Settings Tile**: В шторку Android добавлена фирменная векторная иконка Aether.
- ⚡ **Блокировка рекламы, AdGuard DNS & Kill Switch**: Автоматическая фильтрация рекламы, защита от ТСПУ и от утечек трафика при обрыве связи.
- 🚀 **Прямая загрузка обновлений**: Скачивание новых APK происходит напрямую на максимальной скорости провайдера.

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
