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

## Аутентификация (JWT)

Все `/api/**` эндпоинты, кроме `/api/auth/**` и `/h2-console/**`, требуют JWT-токен.

**Дефолтный ADMIN** (создаётся Flyway-миграцией `V4__seed_admin.sql`): `admin` / `admin123`.

Регистрация нового пользователя — всегда с ролью `MANAGER`:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@example.com","password":"password123"}'
```

Логин (ответ — `{ "token", "username", "role" }`):

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

Использование токена:

```bash
curl http://localhost:8080/api/clients \
  -H "Authorization: Bearer <token>"
```

**Роли:**
- `ADMIN` — видит и правит все ресурсы; единственный, кто может вернуть задачу из `DONE` в `IN_PROGRESS`.
- `MANAGER` — видит и правит только своих клиентов (и их задачи/заметки); на чужих — `403`.
- `VIEWER` — доступ только на чтение ко всем ресурсам.

**Rate limiting:** 5 попыток `/api/auth/login` в минуту (по IP), 100 запросов в минуту на
остальной API (по пользователю). При превышении — `429 Too Many Requests` с заголовком
`Retry-After`.

## Функционал
- Клиенты — CRUD
- Задачи — CRUD, привязаны к клиенту (статус, приоритет, дедлайн)
- Фильтрация задач по статусу / клиенту
- Мини-дашборд по статусам задач

## Тесты

```bash
cd backend
./mvnw test -Dtest='!*IT'   # unit-тесты — реально прогоняются в любой среде (H2, без Docker)
./mvnw test                 # unit + интеграционные — требует Docker (Testcontainers PostgreSQL)
```

**80 unit-тестов, Failures: 0** (профиль `test`, H2 in-memory) + **52 интеграционных теста в 6
классах** (профиль `integration-test`, Testcontainers PostgreSQL) — написаны, компилируются,
но **не выполнялись в среде разработки этой сессии** (нет Docker CLI); проверены вручную
эквивалентными curl-сценариями против локального PostgreSQL 16 (см. `CLAUDE.md`).

| Вид | Классы | Кол-во | Что проверяют |
|---|---|---|---|
| Unit (Mockito/AssertJ) | `ClientServiceImplTest`, `TaskServiceImplTest`, `NoteServiceImplTest`, `DashboardServiceImplTest`, `AuthServiceImplTest`, `AuditServiceImplTest` | — | Бизнес-логика сервисов в изоляции от БД |
| Unit — security-слой | `JwtServiceTest`, `RateLimitFilterTest`, `OwnershipGuardTest`, `CurrentUserServiceImplTest` | — | JWT-валидация (tampered/expired/wrong-secret/garbage/null), лимиты Bucket4j напрямую через `doFilterInternal`, ownership-правила, чтение текущего пользователя из `SecurityContextHolder` |
| Интеграционные (MockMvc + Testcontainers) | `ClientControllerIT` (13), `TaskControllerIT` (23), `DashboardControllerIT` (3), `AuditControllerIT` (5) | 44 | Полный стек HTTP → Spring Security → PostgreSQL; регрессионная защита API-контракта под JWT-токеном (переведены на `adminToken()`/`registerManagerAndLogin()` в B1/B2) |
| Интеграционные — auth/rate-limit | `AuthControllerIT` (7), `RateLimitIT` (1, изолированный Spring-контекст) | 8 | `register`/`login` контракт (201/401/409), happy-path логин → доступ к `/api/clients`, 429 + `Retry-After` на 6-й попытке логина за минуту |

Покрытие (JaCoCo, **только non-IT прогон** — IT-классы не исполнялись в этой среде, поэтому
фильтры/security-конфиг, покрываемые преимущественно через них, недосчитаны):

| Метрика | Результат |
|---|---|
| Строки | **76.1%** (501 / 658) |
| Ветки | **79.8%** (67 / 84) |
| Классы | **70.8%** (34 / 48) |

Ниже прежних 99.1%/85.7% — не регресс, а следствие роста охвата: тогда было 4 сервиса и
39 тестов на весь MVP, сейчас 48 классов включают весь security-слой B1–B4 (`JwtService`,
`JwtAuthenticationFilter`, `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`,
`RateLimitFilter`, `SecurityConfig`, `CorsConfig`), часть которого по природе проверяется
через реальный Spring-контекст в IT-тестах — а те в этой среде не запускались. С учётом
IT-тестов (недоступных здесь для прогона) фактическое покрытие выше.

## Что освоено в Claude Code

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

Генерирует полный CRUD-слой для новой JPA-сущности по паттерну проекта: entity, repository (с сортировкой по id через `findAllByOrderByIdAsc`), DTO Request/Response, service interface + impl (с 404 на зависимых сущностях через `findOrThrow`), controller, exception, unit-тест по образцу `TaskServiceImplTest` — 8 новых файлов + обновление `GlobalExceptionHandler`. Применён для генерации сущности `Note` (заметки, привязанные к `Client`). Расширение (генерация Flyway-миграции к новой сущности вместе с остальным слоем) запланировано отдельным шагом.

### 3. Subagents
**`code-reviewer`** — `.claude/agents/code-reviewer.md`

Архитектурный ревьюер кода mini-crm. Проверяет переданные файлы (или дефолтный набор контроллеров/сервисов) на соответствие 4 принципам слоёной архитектуры из `CLAUDE.md`: **P1** — контроллеры без бизнес-логики, **P2** — сервис как `interface` + `impl`, **P3** — entity не утекают из публичных методов сервисов/контроллеров (только DTO), **P4** — конструкторная инъекция зависимостей. По каждому принципу и файлу выносит вердикт ✅/❌/⚠️ и отдельно перечисляет нарушения/замечания. Вызов: `"запусти code-reviewer на <список файлов>"`. Пока применялся к MVP-слою (Client/Task/Note); прогон на security-код B1–B4 (JWT/RBAC/rate-limit/audit) — отдельный следующий шаг, ещё не выполнен.

### 4. MCP
Не подключался ни один MCP-сервер в рамках этого проекта — честно фиксирую как незакрытый пункт, а не выдумываю использование. Запланировано отдельным шагом.

### 5. Slash-команды
**`/security-check`** — `.claude/commands/security-check.md`

Security-ревью backend-кода по 5 категориям: **S1** — валидация входных данных (`@Valid` на `@RequestBody`, Bean Validation в DTO), **S2** — SQL-инъекции (нативные запросы, конкатенация строк в `@Query`), **S3** — открытые эндпоинты без авторизации (наличие Spring Security, `permitAll()` на чувствительных маршрутах), **S4** — небезопасные CORS-настройки, **S5** — утечка стек-трейсов в HTTP-ответах (`server.error.include-stacktrace`, `GlobalExceptionHandler`). Проверяет заданный список контроллеров/DTO/репозиториев/конфигов и выдаёт отчёт с вердиктами и списком критичных находок/предупреждений.

### 6. Плагины
Не устанавливались и не использовались в рамках этого проекта — честно фиксирую как незакрытый пункт.

## Код-ревью и security-ревью

Ниже — сведённые находки `/security-check` и code-review, применённых на MVP-слое (Part A v2), и их фактический статус.

## Известные ограничения

Прод-готовность, нагрузочное тестирование и 100% покрытие тестами — вне скоупа (см. ТЗ).

### Security (выявлено /security-check)

| # | Ограничение | Риск | Что сделать перед production |
|---|---|---|---|
| ⚠️1 | Frontend закреплён на Next.js 14.2.35 (последний патч в линии 14.x); `npm audit` фиксирует 1 high-severity уязвимость (DoS/cache poisoning/XSS, диапазон 9.x–16.3.0-canary.5), фикс которой требует мажорного апгрейда до 16.x | Средний | Мигрировать на Next.js 16 (App Router/fetch-caching изменения) с последующей ручной проверкой в браузере |
| ⚠️2 | Дефолтный сид ADMIN (`admin`/`admin123`, `V4__seed_admin.sql`) — пароль публичен в репозитории; API для управления пользователями (создание/повышение до ADMIN/VIEWER) нет — только Flyway-сид и прямые вставки в БД | Высокий (если задеплоено как есть) | Сменить пароль admin сразу после первого логина; добавить эндпоинты управления пользователями с проверкой роли ADMIN |

Находки #1 (Spring Security отсутствовал) и #2 (`allowedHeaders("*")`, захардкоженный origin) —
**исправлены**: #1 — Part B, B1 (JWT-аутентификация + Spring Security на всех `/api/**` кроме
`/api/auth/**`); #2 — Part A v2, A7 (явные заголовки `Content-Type`/`Authorization`,
origin вынесен в `app.cors.allowed-origins`). Предупреждение #3 из /security-check
(`DataIntegrityViolationException`, `HttpMessageNotReadableException`) — тоже **исправлено**:
добавлены обработчики в `GlobalExceptionHandler`, возвращающие 409 / 400 с нейтральными сообщениями.

### Технический долг (выявлено code review)

Сознательно не устранено — избыточно для учебного MVP, но зафиксировано на будущее:

| # | Находка | Риск | Что сделать перед production |
|---|---|---|---|
| ⚠️1 | DTO (`ClientRequest/Response`, `TaskRequest/Response`, `NoteRequest/Response`, `DashboardResponse`) — вручную написанные классы с getter/setter вместо Java `record` | Низкий (стиль/поддерживаемость) | Переписать DTO на `record` (Java 21 + Spring Boot 3.3 это поддерживают из коробки) |

Находки #1 (race condition в `delete()`) и #3 (4 отдельных запроса в `DashboardServiceImpl.getStats()`) —
**исправлены** (см. Part A v2: A2 — атомарный `delete()` + маппинг `EmptyResultDataAccessException`
в `GlobalExceptionHandler`; A4 — единый `@Query` с `GROUP BY`).

> **Открытый пункт:** код Part B (B1–B4 — JWT-аутентификация, RBAC/ownership, rate limiting,
> status-transition правила, audit log) ещё не прогонялся ни через `/review`, ни через субагент
> `code-reviewer`, ни через `/security-check` — эти таблицы отражают только MVP-слой (Part A v2).
> Применение `code-reviewer` к B1–B4 запланировано отдельным шагом.

<img width="1438" height="737" alt="image" src="https://github.com/user-attachments/assets/ad438edd-15d7-4e91-b596-d8f6649ff4be" />
<img width="1429" height="503" alt="image" src="https://github.com/user-attachments/assets/1343bc63-1d00-4dbb-b909-b130bc790615" />
<img width="1424" height="658" alt="image" src="https://github.com/user-attachments/assets/dfc1cbec-f390-4e37-898c-6fab8817a856" />

## Part G — об авторстве коммитов

Часть истории этого репозитория закоммичена под именем и почтой «Zalkar Aseinov» — это git-конфиг
ноутбука друга, с которого начиналась работа над частью задания. Само задание от начала до конца
выполнялось мной самостоятельно; остальные коммиты (начиная с Part A v2) сделаны уже с личного
аккаунта.
