# 🛡️ Aether — Modern Android X-UI & VLESS VPN Client

[![Download APK](https://img.shields.io/badge/Download-APK_v0.9-2EA043?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Ulqiorra24/Aether/releases/latest)
[![Release](https://img.shields.io/github/v/release/Ulqiorra24/Aether?style=for-the-badge&label=Latest%20Release&color=0969DA)](https://github.com/Ulqiorra24/Aether/releases/latest)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Compose-Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)

> 📦 **[Скачать Aether APK (v0.9)](https://github.com/Ulqiorra24/Aether/releases/latest)** — официальный готовый файл релиза для Android.

---

## 📌 Обзоp проекта

**Aether** — современный, быстрый и безопасный Android-клиент для работы с подписками **x-ui / 3x-ui**. Приложение построено на современной декларативной платформе **Jetpack Compose (Material 3)** и использует официальное высокопроизводительное ядро **Xray-core** ([AndroidLibXrayLite v26.11.9](https://github.com/2dust/AndroidLibXrayLite)) со встроенным TUN-интерфейсом.

Приложение спроектировано с акцентом на высокую скорость подключения, обход средств ТСПУ/блокировок, интеллектуальную маршрутизацию и полную незаметность для банковских приложений и сервисов Битрикс24.

---

## 🔥 Что нового в v0.9

- 🚀 **Добавлены разделы «Новости» и «Поддержка»**:
  - **«Новости»** — оперативные объявления и важные обновления сервиса прямо в приложении.
  - **«Поддержка»** — прямой чат с администрацией для оперативного решения вопросов.
- 💬 **Push-уведомления от поддержки**:
  - Ответы от службы поддержки приходят прямо в системную шторку Android.
  - Нажатие на уведомление открывает приложение сразу на вкладке «Поддержка».
- 🗑️ **Очистка чата**:
  - В верхней панели чата поддержки добавлена кнопка корзины для локальной очистки переписки.

---

## 🛠️ Архитектура и структура компонентов

### 🎨 UI Слой (Jetpack Compose & Material 3)
* `MainScreen.kt` — Главный экран управления VPN, мониторинг скорости (Download/Upload), список серверов с автовыбором наименьшего пинга, плашка статуса подписки, модальное окно логов трафика и диалоги продления.
* `AuthScreen.kt` — Экран входа, двухшаговой регистрации (с подтверждением Email по 6-значному коду) и привязки существующих подписок.
* `SplashScreen.kt` — Стартовый экран анимации запуска.
* `MainViewModel.kt` — Центральная ViewModel, управляющая состоянием VPN-сервиса, авторизацией, загрузкой подписок и подсчетом времени.

### ⚡ VPN и Xray Ядро
* `V2RayVpnService.kt` — Нативный Android `VpnService`. Управляет циклом жизни ядра Xray, создает TUN-интерфейс (`10.0.0.2`), читает файлы логов `access.log` в реальном времени и ведет фоновый мониторинг связи.
* `V2RayConfigParser.kt` — Парсер и генератор конфигураций JSON для Xray-core (VLESS + REALITY, gRPC, XHTTP, QUIC, обход РФ и LAN, AdGuard DNS, Kill Switch).
* `AetherTileService.kt` — Нативная плитка быстрой настройки в шторке Android (Quick Settings Tile).
