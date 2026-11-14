# Aether (Android x-ui VPN Client)

[![Download APK](https://img.shields.io/badge/Download-APK_v0.8.2-2EA043?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Ulqiorra24/Aether/releases/latest)
[![Release](https://img.shields.io/github/v/release/Ulqiorra24/Aether?style=for-the-badge&label=Latest%20Release&color=0969DA)](https://github.com/Ulqiorra24/Aether/releases/latest)

> 📦 **[Скачать Aether APK (v0.8.2)](https://github.com/Ulqiorra24/Aether/releases/latest)** — официальный готов к установке файл релиза для Android.

---

Android-клиент подписок x-ui / 3x-ui. VPN реализован через Xray-core
(AndroidLibXrayLite) и встроенный TUN inbound.

## Что нового в v0.8.2

- 📲 **Улучшенный диалог обновления приложения**:
  - Индикатор и проценты загрузки APK вынесены наверх и всегда видны сразу после нажатия кнопки.
  - Описание изменений (release notes) помещено в отдельный скроллируемый блок и не вытесняет полосу прогресса.
- ⌨️ **Адаптация диалога оплаты под клавиатуру**:
  - Добавлена плавная прокрутка (`verticalScroll`) и учет высоты клавиатуры (`imePadding`, `adjustResize`).
  - Поле ввода промокода и кнопка перехода к оплате автоматически сдвигаются вверх и остаются доступными при открытой клавиатуре.
- 🎟️ **Поддержка промокодов при оплате**:
  - В диалоге продления подписки добавлено поле для ввода промокода.
  - Промокод передается в API Lava.top, скидка рассчитывается автоматически перед переходом к оплате.
- ⚡ **Стандартный HTTPS-порт 443**:
  - Все API-запросы и ссылки оплаты переведены на стандартный защищенный порт 443 (`https://mikaela-go.ru`) без порта :2096.
- 💳 **Прямая интеграция с Lava.top API**:
  - Генерация персональных инвойсов через API Lava.top с гарантированной доставкой Webhook и моментальным автопродлением в 3X-UI.
- ✉️ **Email для оплаты в профиле и при регистрации**:
  - Поле «Email для оплаты» при регистрации аккаунта и отображение в Профиле с копированием в 1 клик.
  - Кнопка редактирования (иконка Карандаша) в подписках для смены пароля и Email для оплаты.
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
