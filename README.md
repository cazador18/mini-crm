# Mini-CRM / Task Tracker

Учебный фуллстек-MVP: мини-CRM с клиентами и задачами.
Сделан в рамках знакомства с Claude Code (см. `CLAUDE.md` для деталей архитектуры).

## Стек
Java 21 + Spring Boot 3 · PostgreSQL · Next.js + TypeScript · Docker Compose

## Запуск

```bash
docker compose up --build
```

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- PostgreSQL: localhost:5432 (minicrm/minicrm)

## Функционал
- Клиенты — CRUD
- Задачи — CRUD, привязаны к клиенту (статус, приоритет, дедлайн)
- Фильтрация задач по статусу / клиенту
- Мини-дашборд по статусам задач

## Тесты

```bash
cd backend
./mvnw test
```

**39 тестов, Failures: 0** — unit + интеграционные запускаются одной командой.

| Вид | Классы | Кол-во | Что проверяют |
|---|---|---|---|
| Unit (Mockito) | `ClientServiceImplTest`, `TaskServiceImplTest`, `DashboardServiceImplTest` | 20 | Бизнес-логика сервисов в изоляции от БД |
| Интеграционные (MockMvc + Testcontainers) | `ClientControllerIT`, `TaskControllerIT`, `DashboardControllerIT` | 19 | Полный стек HTTP → Spring → PostgreSQL; регрессионная защита API-контракта |

Покрытие (JaCoCo):

| Метрика | Результат |
|---|---|
| Строки | **99.1%** (217 / 219) |
| Ветки | **85.7%** (12 / 14) |

Непокрытые 2 строки — метод `main` в `MiniCrmApplication` (стандартный Spring Boot entry point, не тестируется). 2 непокрытые ветки — защитные `null`-ветки в `TaskServiceImpl`, недостижимые через валидированный API.

## Что освоено в Claude Code

> Заполнить по ходу работы — по каждой из 6 тем.

### 1. Модели (Opus / Sonnet / Haiku)

Основная рабочая модель — **claude-sonnet-4-6**: быстрая, хорошо справляется с генерацией кода по шаблону (CRUD-слой сущности `Note`, unit- и интеграционные тесты, конфигурация JaCoCo). Для рутинных задач с чётким паттерном её скорости и качества достаточно.

Для **архитектурного анализа** переключились на **Opus** через `/model`. На задаче «предложи 3 улучшения перед production» разница проявилась наглядно:

| | claude-sonnet-4-6 | Opus |
|---|---|---|
| Глубина | Более общие рекомендации | Глубокий анализ с конкретными ссылками на код |
| Примеры находок | Общие best practices | Flyway вместо `ddl-auto: update`, пагинация на списочных эндпоинтах, оптимистичная блокировка через `@Version` — с указанием строк и trade-offs |

**Вывод:** Sonnet — для генерации кода и рутинных задач; Opus — для архитектурных решений и анализа trade-offs, где важна глубина и привязка к конкретному коду.

### 2. Skills
**`/crud-generator`** — `.claude/skills/crud-generator/SKILL.md`

Генерирует полный CRUD-слой для новой JPA-сущности: entity, repository (с сортировкой по id), DTO Request/Response, service interface + impl (с 404 на зависимых сущностях), controller, exception, unit-тест. Обновляет `GlobalExceptionHandler`. Применён для генерации сущности `Note` (заметки, привязанные к `Client`).

### 3. Subagents
Свой субагент: `.claude/agents/<имя>.md` — _TODO: что делает, как вызывается_

### 4. MCP
_TODO: какой сервер подключали, что попробовали_

### 5. Slash-команды
Своя команда: `.claude/commands/<имя>.md` — _TODO_

### 6. Плагины
_TODO_

## Код-ревью и security-ревью
_TODO: ключевые замечания от `/review` и `/security-review`, что исправлено_

## Известные ограничения

Прод-готовность, нагрузочное тестирование и 100% покрытие тестами — вне скоупа (см. ТЗ).

### Security (выявлено /security-check)

| # | Ограничение | Риск | Что сделать перед production |
|---|---|---|---|
| ⚠️1 | Spring Security отсутствует — все `/api/**` эндпоинты открыты без аутентификации | Высокий | Добавить `spring-boot-starter-security`, настроить JWT или сессионную аутентификацию, ограничить `PUT`/`DELETE` ролью `ADMIN` |
| ⚠️2 | `allowedHeaders("*")` в CORS; origin `http://localhost:3000` захардкожен | Низкий | Явно перечислить заголовки (`Content-Type`, `Authorization`); вынести origin в `application.yml` как `app.cors.allowed-origins` |

Предупреждение #3 (`DataIntegrityViolationException`, `HttpMessageNotReadableException`) — **исправлено**: добавлены обработчики в `GlobalExceptionHandler`, возвращающие 409 / 400 с нейтральными сообщениями.

### Технический долг (выявлено code review)

Сознательно не устранено — избыточно для учебного MVP, но зафиксировано на будущее:

| # | Находка | Риск | Что сделать перед production |
|---|---|---|---|
| ⚠️1 | Race condition в `delete()` (`ClientServiceImpl`, `TaskServiceImpl`, `NoteServiceImpl`): `findOrThrow()` и `deleteById()` — два отдельных вызова без блокировки, при параллельных DELETE-запросах на один и тот же id возможна необработанная гонка | Низкий | Делать `delete()` через один атомарный вызов репозитория либо ловить/маппить `EmptyResultDataAccessException` в `GlobalExceptionHandler` |
| ⚠️2 | DTO (`ClientRequest/Response`, `TaskRequest/Response`, `NoteRequest/Response`, `DashboardResponse`) — вручную написанные классы с getter/setter вместо Java `record` | Низкий (стиль/поддерживаемость) | Переписать DTO на `record` (Java 21 + Spring Boot 3.3 это поддерживают из коробки) |
| ⚠️3 | `DashboardServiceImpl.getStats()` — 4 отдельных запроса (`count()` + 3× `countByStatus()`) вместо одного агрегирующего запроса с `GROUP BY` | Низкий (не масштабируется) | Заменить на один `@Query("select t.status, count(t) from Task t group by t.status")` |
<img width="1438" height="737" alt="image" src="https://github.com/user-attachments/assets/ad438edd-15d7-4e91-b596-d8f6649ff4be" />
<img width="1429" height="503" alt="image" src="https://github.com/user-attachments/assets/1343bc63-1d00-4dbb-b909-b130bc790615" />
<img width="1424" height="658" alt="image" src="https://github.com/user-attachments/assets/dfc1cbec-f390-4e37-898c-6fab8817a856" />


