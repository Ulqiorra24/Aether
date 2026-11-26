# 🛡️ Aether — Modern Android X-UI & VLESS VPN Client

[![Download APK](https://img.shields.io/badge/Download-APK_v0.8.9-2EA043?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Ulqiorra24/Aether/releases/latest)
[![Release](https://img.shields.io/github/v/release/Ulqiorra24/Aether?style=for-the-badge&label=Latest%20Release&color=0969DA)](https://github.com/Ulqiorra24/Aether/releases/latest)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Compose-Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)

> 📦 **[Скачать Aether APK (v0.8.9)](https://github.com/Ulqiorra24/Aether/releases/latest)** — официальный готовый файл релиза для Android.

---

## 📌 Обзоp проекта

**Aether** — современный, быстрый и безопасный Android-клиент для работы с подписками **x-ui / 3x-ui**. Приложение построено на современной декларативной платформе **Jetpack Compose (Material 3)** и использует официальное высокопроизводительное ядро **Xray-core** ([AndroidLibXrayLite v26.11.9](https://github.com/2dust/AndroidLibXrayLite)) со встроенным TUN-интерфейсом.

Приложение спроектировано с акцентом на высокую скорость подключения, обход средств ТСПУ/блокировок, интеллектуальную маршрутизацию и полную незаметность для банковских приложений и сервисов Битрикс24.

---

## 🔥 Что нового в v0.8.9

- 📊 **Детальные логи трафика в реальном времени**: Полное отслеживание каждого входящего пакета (TCP/UDP), DNS-запросов и решений маршрутизации (`PROXY` / `DIRECT` / `BLOCK` / `DNS` / `INBOUND`) с наглядной цветовой индикацией.
- 🔐 **Исправление регистрации и подтверждения**:
  - Полностью восстановлена регистрация и привязка подписок из мобильного приложения (устранена ошибка `403 Запрос отклонён` и `503 Не удалось создать профиль`).
  - Добавлена возможность повторного вызова окна ввода 6-значного кода подтверждения из письма, если пользователь закрыл его во время регистрации.
- ⏳ **Защита от запуска при истекшей подписке**:
  - При завершении срока действия подписки (`0 дней` / «Исчерпано») попытка включить VPN предотвращается.
  - Автоматически выводится фирменное диалоговое окно **«Срок подписки исчерпан»** с предупреждающей иконкой замка и прямой кнопкой **«Продлить подписку»**.
- 🎯 **Точный расчёт дней без двойного счёта**:
  - Исправлен алгоритм вычисления дней подписки. Данные синхронизируются напрямую с сервера 3x-ui без двойного начисления локальных дней.
  - Устранена off-by-one ошибка («Осталось дней: 1» при истечении подписки сегодня).

---

## 🛠️ Архитектура и структура компонентов

### 🎨 UI Слой (Jetpack Compose & Material 3)
* `MainScreen.kt` — Главный экран управления VPN, мониторинг скорости (Download/Upload), список серверов с автовыбором наименьшего пинга, плашка статуса подписки, модальное окно логов трафика и диалоги продления.
* `AuthScreen.kt` — Экран входа, двухшаговой регистрации (с подтверждением Email по 6-значному коду) и привязки существующих подписок.
* `SplashScreen.kt` — Стартовый экран анимации запуска.
* `MainViewModel.kt` — Центральная ViewModel, управляющая состоянием VPN-сервиса, авторизацией, загрузкой подписок и подсчетом времени.

### 💾 Data Слой
* `AuthRepository.kt` — Взаимодействие с API авторизации и регистрации на сервере Aether (`/api/account/register/start`, `/api/account/register/confirm`, `/api/auth/login`).
* `SubscriptionRepository.kt` — Загрузка и декодирование подписок по протоколу VLESS / 3x-ui.
* `VpnPrefs.kt` — Локальное шифрованное и обычное хранилище настроек (SharedPreferences), выбранных узлов и флагов триала.
* `UpdateChecker.kt` — Прямая загрузка обновлений APK в обход VPN на скорости локального провайдера.

### ⚡ VPN и Xray Ядро
* `V2RayVpnService.kt` — Нативный Android `VpnService`. Управляет циклом жизни ядра Xray, создает TUN-интерфейс (`10.0.0.2`), читает файлы логов `access.log` в реальном времени и ведет фоновый мониторинг связи.
* `V2RayConfigParser.kt` — Парсер и генератор конфигураций JSON для Xray-core (VLESS + REALITY, gRPC, XHTTP, QUIC, обход РФ и LAN, AdGuard DNS, Kill Switch).
* `AetherTileService.kt` — Нативная плитка быстрой настройки в шторке Android (Quick Settings Tile).

---

## 🖥️ Серверная инфраструктура и скрипты управления (`moscow`)

На основном сервере управления **`moscow`** действуют две связанные подсистемы:
1. **`aether-auth`** (`/opt/aether-auth/`) — Python/aiohttp сервис авторизации, верификации Email и управления оплатой (порт 8000).
2. **`3x-ui`** (`/usr/local/x-ui/`) — Панель управления VPN и ядро Xray.

### 📜 Администраторские скрипты на сервере (`/root/`):

- **`bash /root/7days.sh <username>`**:
  Снимает с указанного аккаунта блокировку повторного использования 7-дневного пробного периода. Удаляет запись из таблицы `trial_claims` в `aether.db`.
  
- **`bash /root/deluser.sh <username>`**:
  Полностью удаляет аккаунт отовсюду (из баз `aether.db` и `x-ui.db`). Освобождает логин и email для чистой повторной регистрации с нуля.

- **`/root/README.md`**:
  Справочник администратора сервера со списком всех команд и структурой сервисов.

---

## 🔧 Сборка и тестирование

### Требования к окружению
- **JDK 17** (Temurin или Oracle)
- **Android SDK 35** (Build Tools 35.0.0)
- **Gradle 8.11.1+**

### Полезные команды Gradle

```bash
# Запуск юнит-тестов
./gradlew :app:testDebugUnitTest

# Сборка Debug APK
./gradlew :app:assembleDebug

# Сборка подписи Подписанного Release APK
./gradlew :app:assembleRelease -PaetherVersionName=0.8.9 -PaetherVersionCode=20
```

Собраный APK файла релиза находится по пути:
`app/build/outputs/apk/release/aether.apk`

---

## 📝 Полная история диалога и изменений за сегодня

Все детали проведённых сегодня тестов, отладки скриптов сервера и реализации функций зафиксированы в файле:
📄 **[CHAT_HISTORY_2026-11-26.md](CHAT_HISTORY_2026-11-26.md)**
