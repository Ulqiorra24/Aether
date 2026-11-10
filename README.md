# Aether (Android x-ui VPN Client)

[![Download APK](https://img.shields.io/badge/Download-APK_v1.0-2EA043?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Ulqiorra24/Aether/releases/latest)
[![Release](https://img.shields.io/github/v/release/Ulqiorra24/Aether?style=for-the-badge&label=Latest%20Release&color=0969DA)](https://github.com/Ulqiorra24/Aether/releases/latest)

> 📦 **[Скачать Aether APK (v1.0)](https://github.com/Ulqiorra24/Aether/releases/latest)** — официальный готов к установке файл релиза для Android.

---

Android-клиент подписок x-ui / 3x-ui. VPN реализован через Xray-core.
(AndroidLibXrayLite) и встроенный TUN inbound.

## Подключение

1. Добавьте аккаунт в настройках и обновите подписку.
2. Выберите узел VLESS + REALITY.
3. Нажмите подключение и подтвердите системное разрешение Android.
4. Статус «Подключено» появляется после HTTPS-проверки через выбранный сервер.
   Отключение доступно в приложении, в уведомлении и в настройках VPN Android.

Поддерживаются TCP (в том числе `flow=xtls-rprx-vision`) и gRPC без Vision.
Из ссылки используются UUID, адрес, порт, `sni`, `pbk`, `sid`, `fp`,
`flow`, `spx` и `serviceName`. Другие протоколы и транспорты могут
отображаться в подписке, но при подключении возвращают явную ошибку.

Режим «Обход LAN» направляет частные IPv4/IPv6-адреса напрямую.
Весь остальной трафик, включая DNS на 1.1.1.1 / 8.8.8.8, идёт через сервер.
Само приложение исключено из VPN: это предотвращает зацикливание сокетов
ядра и позволяет обновлять подписку. Проверка подключения обращается к
`https://www.gstatic.com/generate_204` через локальный SOCKS-вход Xray;
доступность этого адреса необходима для успешной проверки.

Выбор узла и режима маршрутизации применяется до подключения.
Автоматическое восстановление VPN после завершения процесса пока не реализовано.

## Сборка и проверки

Требуются JDK 17+, Android SDK 37 и доступ к Maven/Google/GitHub.

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Нативный smoke-тест на подключённом устройстве/эмуляторе проверяет загрузку
Xray, конфигурацию REALITY, SOCKS handshake и повторный запуск ядра:

```bash
./gradlew :app:connectedDebugAndroidTest
```

Он не подключается к реальному серверу. Проверку TUN, трафика приложений,
DNS, остановки и повторного подключения нужно выполнить на устройстве
с действительной подпиской.

## VPN-ядро

Используется официальный [AndroidLibXrayLite v26.11.9](https://github.com/2dust/AndroidLibXrayLite/releases/tag/v26.11.9),
загружаемый Gradle как AAR из закрепленного GitHub-релиза.
SHA-256: `cdfef146218233a31a8b52c8b1016557730d3b63fc121d4c84cacea50e669155`.

Исходники и лицензии:
[AndroidLibXrayLite](https://github.com/2dust/AndroidLibXrayLite),
[Xray-core (MPL-2.0)](https://github.com/XTLS/Xray-core).
При распространении приложения соблюдайте лицензии ядра и его зависимостей.
