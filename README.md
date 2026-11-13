# Aether (Android x-ui VPN Client)

[![Download APK](https://img.shields.io/badge/Download-APK_v0.4-2EA043?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Ulqiorra24/Aether/releases/latest)
[![Release](https://img.shields.io/github/v/release/Ulqiorra24/Aether?style=for-the-badge&label=Latest%20Release&color=0969DA)](https://github.com/Ulqiorra24/Aether/releases/latest)

> 📦 **[Скачать Aether APK (v0.4)](https://github.com/Ulqiorra24/Aether/releases/latest)** — официальный готов к установке файл релиза для Android.

---

Android-клиент подписок x-ui / 3x-ui. VPN реализован через Xray-core
(AndroidLibXrayLite) и встроенный TUN inbound.

## Что нового в v0.4

- 🔐 **Личный кабинет и авторизация**:
  - Вход по Email (логин) и Паролю.
  - Регистрация новых пользователей с автоматической выдачей подписки.
  - Активация существующих подписок по Email + Sub ID с назначением пароля.
- 🔀 **Расширенная маршрутизация**:
  - Исключение локальной сети (LAN).
  - Исключение российских сайтов (`.ru`, `.рф`, `.su`, госресурсов) и IP-адресов РФ мимо VPN.
- 📊 **Логи трафика**:
  - Просмотр TCP/UDP соединений, DIRECT/PROXY статусов с возможностью копирования и очистки.
- ⚙️ **Модульные настройки**:
  - Вкладка «Настройки» переработана в чистые кнопки с вызовом нижних модальных панелей (ModalBottomSheet) для Темы, Маршрутизации и Логов.
- 🛡️ **Обфускация и безопасность**:
  - Шифрование служебных хостов и конфигурационных адресов в бинарнике (XOR-обфускация).

## Что было в v0.2 – v0.3

- 🎨 **Новый современный UI (Material 3)**: нижняя панель навигации (Главная, Подписки, Настройки), фоновые градиенты и обновлённая цветовая палитра.
- 🌐 **Удобный выбор серверов**: модальная шторка (ModalBottomSheet) со списком узлов и проверкой пинга (задержки).
- 🔑 **Управление подписками**: добавление и удаление аккаунтов с подтверждением, переключение подписок.
- ⏳ **Срок действия и информация**: парсинг оставшихся дней подписки и просмотр версии ядра Xray.

## Подключение

1. Выполните вход, регистрацию или активацию аккаунта.
2. Выберите узел VLESS + REALITY.
3. Нажмите кнопку подключения и подтвердите системное разрешение Android.
4. Статус «Подключено» появляется после HTTPS-проверки через выбранный сервер.
   Отключение доступно в приложении, в уведомлении и в настройках VPN Android.

Поддерживаются TCP (в том числе `flow=xtls-rprx-vision`) и gRPC без Vision.
Из ссылки используются UUID, адрес, порт, `sni`, `pbk`, `sid`, `fp`,
`flow`, `spx` и `serviceName`.

Режимы маршрутизации позволяют автоматически направлять частные IPv4/IPv6-адреса и домены РФ напрямую мимо VPN.

## Сборка и проверки

Требуются JDK 17+, Android SDK 37 и доступ к Maven/Google/GitHub.

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

Подписанная сборка при наличии локального ключа:

```bash
./gradlew :app:assembleRelease
```

Release APK: `app/build/outputs/apk/release/app-release.apk`. В GitHub Releases файл называется `aether.apk`.

## VPN-ядро

Используется официальный [AndroidLibXrayLite v26.11.9](https://github.com/2dust/AndroidLibXrayLite/releases/tag/v26.11.9).
