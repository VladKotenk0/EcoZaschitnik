# ЭкоЗащитник

Мобильное приложение и веб-сайт для фиксации несанкционированных мусорных свалок: фото, GPS-координаты, облачное хранение и отображение на карте.

Выпускная квалификационная работа.

## Состав репозитория

| Папка | Описание |
|-------|----------|
| `app/` | Android-приложение |
| `EcoSite/` | Веб-сайт (Firebase Hosting) |

## Основной сценарий

1. Пользователь создаёт отчёт о свалке (название, описание, фото).
2. Приложение получает GPS-координаты.
3. Формируется текстовый отчёт с помощью языковой модели (OpenRouter).
4. Данные сохраняются в Firebase Firestore и отображаются на карте в приложении и на сайте.

## Технологии

- **Android:** Kotlin, CameraX, ViewModel, osmdroid (OpenStreetMap)
- **Backend:** Firebase Firestore, Firebase Storage, Firebase Hosting
- **ИИ:** OpenRouter API (генерация текста по описанию и координатам)
- **Веб:** HTML, JavaScript, Leaflet

## Сборка Android-приложения

1. Откройте проект в Android Studio.
2. Скопируйте `local.properties.example` → `local.properties`.
3. Укажите в `local.properties`:
   - `sdk.dir` — путь к Android SDK;
   - `OPENROUTER_API_KEY` — ключ [OpenRouter](https://openrouter.ai/keys);
   - `LLM_MODEL` — ID модели из [каталога OpenRouter](https://openrouter.ai/models).
4. Добавьте `google-services.json` в `app/` (Firebase Console).
5. Соберите и запустите на устройстве или эмуляторе.

## Веб-сайт

Статические файлы в `EcoSite/public/`. Публикация через Firebase Hosting (`EcoSite/firebase.json`).

## Автор

Котенко Владислав · гр. ЦПИ-41
