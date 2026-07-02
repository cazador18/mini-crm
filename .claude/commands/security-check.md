# /security-check

Проводит security-ревью backend-кода mini-crm по пяти категориям уязвимостей.

## Что проверяется

### S1: Валидация входных данных
- Все `@RequestBody` в контроллерах помечены `@Valid`
- DTO-классы содержат аннотации Bean Validation (`@NotBlank`, `@NotNull`, `@Email`, и т.д.)
- Нет принятия произвольных `Map<String, Object>` или `Object` как тела запроса

### S2: SQL-инъекции
- Репозитории не используют нативные SQL-запросы со строковой конкатенацией
- `@Query` с `nativeQuery = true` — отсутствуют или используют только именованные параметры (`:param` / `?1`)
- Derived query methods (Spring Data) — безопасны по определению, помечать ✅

### S3: Открытые эндпоинты без авторизации
- Проверить наличие Spring Security в `pom.xml`
- Если Spring Security отсутствует — зафиксировать как ⚠️ (все эндпоинты открыты) с пояснением, критично ли для MVP
- Если присутствует — проверить конфиг `SecurityFilterChain`: нет ли `permitAll()` на чувствительных маршрутах

### S4: Небезопасные CORS-настройки
- Проверить `CorsConfig` или аналог
- Опасно: `allowedOrigins("*")` совместно с `allowCredentials(true)` — браузеры заблокируют, но это ошибка конфигурации
- Опасно: `allowedOrigins("*")` в production без ограничений методов/заголовков
- Приемлемо для MVP: `allowedOrigins("*")` без credentials и с ограниченными методами — фиксировать как ⚠️

### S5: Утечка стек-трейсов в HTTP-ответах
- Проверить `GlobalExceptionHandler`: возвращает ли он `ex.getMessage()` или только контролируемое сообщение
- Убедиться что нет `e.printStackTrace()` или логирования stacktrace в HTTP-ответ
- Spring Boot по умолчанию возвращает `/error` с `trace` полем — проверить `application.yml` на `server.error.include-stacktrace`

## Файлы для проверки

**Контроллеры:**
- `backend/src/main/java/com/evogroup/minicrm/controller/ClientController.java`
- `backend/src/main/java/com/evogroup/minicrm/controller/TaskController.java`
- `backend/src/main/java/com/evogroup/minicrm/controller/NoteController.java`
- `backend/src/main/java/com/evogroup/minicrm/controller/DashboardController.java`

**DTO (валидация):**
- `backend/src/main/java/com/evogroup/minicrm/dto/ClientRequest.java`
- `backend/src/main/java/com/evogroup/minicrm/dto/TaskRequest.java`
- `backend/src/main/java/com/evogroup/minicrm/dto/NoteRequest.java`

**Репозитории (SQL-инъекции):**
- `backend/src/main/java/com/evogroup/minicrm/repository/ClientRepository.java`
- `backend/src/main/java/com/evogroup/minicrm/repository/TaskRepository.java`
- `backend/src/main/java/com/evogroup/minicrm/repository/NoteRepository.java`

**Конфигурация и безопасность:**
- `backend/src/main/java/com/evogroup/minicrm/config/CorsConfig.java`
- `backend/src/main/java/com/evogroup/minicrm/exception/GlobalExceptionHandler.java`
- `backend/src/main/resources/application.yml`
- `backend/pom.xml` (наличие spring-boot-starter-security)

## Формат отчёта

```
# Security Check — mini-crm

## Итог
<общий вердикт: критичных уязвимостей N, предупреждений M>

## S1: Валидация входных данных
<по каждому контроллеру и DTO — ✅/❌/⚠️ + одна фраза>

## S2: SQL-инъекции
<по каждому репозиторию — ✅/❌/⚠️ + одна фраза>

## S3: Авторизация эндпоинтов
✅/❌/⚠️ — <статус + объяснение>

## S4: CORS-настройки
✅/❌/⚠️ — <что именно настроено и риск>

## S5: Утечка стек-трейсов
✅/❌/⚠️ — <что возвращается в ответах на ошибки>

## Критичные уязвимости (❌)
<список или "Не обнаружено">

## Предупреждения (⚠️)
<список с пояснением риска и рекомендацией>
```
