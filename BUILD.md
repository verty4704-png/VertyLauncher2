# VertyLauncher — Сборка APK

## Требования
- Android Studio Hedgehog (2023.1.1) или новее
- JDK 17
- Android SDK API 34
- NDK 25.2.9519653 (для нативного моста)
- CMake 3.22.1+

## Шаги сборки

### 1. Импорт проекта
```bash
# Распакуйте архив
# Откройте папку VertyLauncher в Android Studio
# Дождитесь синхронизации Gradle (5-10 минут первый раз)
```

### 2. Добавление JRE рантаймов (опционально)
Скопируйте в `app/src/main/assets/runtimes/`:
- `java8.zip` — JRE 8 для Minecraft 1.12.2 и ниже
- `java17.zip` — JRE 17 для Minecraft 1.17-1.20
- `java21.zip` — JRE 21 для Minecraft 1.21+

Или оставьте пустым — рантаймы скачаются автоматически при первом запуске.

### 3. Добавление нативных библиотек (опционально)
Скопируйте в `app/src/main/jniLibs/arm64-v8a/`:
- `liblwjgl.so`
- `libopenal.so`
- `libgl4es_114.so` (или `libEGL_angle.so` / `libGLESv2_angle.so`)

### 4. Сборка
```
Build → Build Bundle(s) / APK(s) → Build APK(s)
```

### 5. Установка
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Архитектура
- `arm64-v8a` — основная (телефоны 2016+)
- `x86_64` — эмуляторы, Chromebook

## Первый запуск
1. Выберите версию ("Версии")
2. Укажите никнейм ("Настройки")
3. Нажмите "Играть"
4. При первом запуске скачается JRE (100-150 МБ) и клиент Minecraft
