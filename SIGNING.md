# Подпись APK

Публикуемые APK собираются через `:app:assembleRelease` и подписываются постоянным ключом с alias `aether`.

Локальный ключ: `.signing/aether-release.jks`. Пароль: `.signing/password`.
Оба файла исключены из Git. Сохраните их резервную копию в защищённом хранилище: без этого ключа нельзя выпускать совместимые обновления. GitHub Secrets нельзя скачать обратно.

GitHub Actions использует Secrets `AETHER_KEYSTORE_BASE64` и `AETHER_KEYSTORE_PASSWORD`. Ключ восстанавливается во временный файл и удаляется после сборки.

Локальная сборка: `./gradlew :app:assembleRelease`.
APK: `app/build/outputs/apk/release/app-release.apk`.

Для CI путь к ключу передаётся через `AETHER_KEYSTORE_PATH`, пароль — через `AETHER_KEYSTORE_PASSWORD`.

Версии до v1.3 включительно выпускались с временными debug-ключами. Для перехода на постоянную подпись может потребоваться удалить старую установку (с потерей локальных настроек) и установить новый release APK. Последующие обновления должны использовать тот же ключ, applicationId и возрастающий versionCode.
