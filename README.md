# Mini-CRM / Task Tracker

Учебный фуллстек-MVP: мини-CRM с клиентами и задачами.
Сделан в рамках знакомства с Claude Code (см. `CLAUDE.md` для деталей архитектуры).

## Стек
Java 21 + Spring Boot 3 · PostgreSQL · Next.js + TypeScript · Docker Compose

## Запуск

Перед первым запуском скопируй `.env.example` в `.env` и заполни секреты (в первую очередь
`JWT_SECRET` — минимум 32 случайных символа; без него `docker compose up` откажется
стартовать backend):

```bash
cp .env.example .env
# отредактировать .env — задать реальный JWT_SECRET
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
Подключён **GitHub MCP** — использован для чтения issues и pull request'ов репозитория прямо из Claude Code, без переключения в браузер.

Пример: запрос списка issues и PR репозитория `cazador18/mini-crm` (`mcp__github__list_issues`, `mcp__github__list_pull_requests`) вернул:

| # | Тип | Заголовок | Статус |
|---|---|---|---|
| [#1](https://github.com/cazador18/mini-crm/issues/1) | Issue | Production improvements | open |
| [#2](https://github.com/cazador18/mini-crm/pull/2) | PR | feat: Level-up Mini-CRM v2 — JWT аутентификация, роли, rate limiting, аудит-лог, Flyway | open |

Issue #1 (`Production improvements`) перечисляет три пункта, выявленных при архитектурном анализе перед production: Flyway вместо `ddl-auto: update`, пагинация на списочных эндпоинтах, оптимистичная блокировка через `@Version`. PR #2 — это ветка `feature/level-up` с доработками Part A–E (JWT, RBAC, rate limiting, аудит-лог, Flyway, расширенные тесты). Два из трёх пунктов issue #1 (Flyway, пагинация) уже закрыты в этой ветке; `@Version` остаётся открытым.

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

> **Part E — код B1–B4 проверен:** субагент `code-reviewer` (JwtService, RateLimitFilter,
> AuditServiceImpl, AuthController, TaskServiceImpl/status-transition-правила) — нарушений P1–P4
> не найдено. `/security-check` на всём security-слое B1–B4 нашёл 1 критичную находку —
> захардкоженный дефолтный `JWT_SECRET` в `application.yml` подхватывался и в `docker`-профиле
> (в `docker-compose.yml` переменная не пробрасывалась), то есть любой, кто видел этот
> публичный репозиторий, мог подделать валидный JWT для любой роли, включая ADMIN, без пароля.
> **Исправлено**: дефолт убран из общего блока `application.yml`, `docker`-профиль теперь
> обязан получить `JWT_SECRET` через переменную окружения (`docker-compose.yml` + `.env`,
> см. `## Запуск`); `dev`/`test`/`integration-test` сохранили свои безопасные
> дефолты-заглушки, чтобы `./mvnw spring-boot:run`/`./mvnw test` продолжали работать без `.env`.

<img width="1438" height="737" alt="image" src="https://github.com/user-attachments/assets/ad438edd-15d7-4e91-b596-d8f6649ff4be" />
<img width="1429" height="503" alt="image" src="https://github.com/user-attachments/assets/1343bc63-1d00-4dbb-b909-b130bc790615" />
<img width="1424" height="658" alt="image" src="https://github.com/user-attachments/assets/dfc1cbec-f390-4e37-898c-6fab8817a856" />

## Part F — Ответы на вопросы

### По MVP

**1. Почему сервисы через интерфейс + impl (`ClientService`/`ClientServiceImpl`)?**

Контроллер зависит от интерфейса `ClientService`, а не от `ClientServiceImpl` напрямую — это
изоляция контракта от реализации. На практике в этом MVP у каждого сервиса всего одна
реализация, но паттерн даёт: (1) контроллер и тесты контроллера можно мокать по интерфейсу,
не зная деталей impl; (2) `@Service` стоит только на `impl`, интерфейс остаётся чистым
описанием методов — это прямо проверяется принципом P2 в `code-reviewer`; (3) если завтра
понадобится вторая реализация (например, кэширующая обёртка) — она подключается без
изменения контроллера. Плата за это — лишний файл на каждый сервис, для учебного MVP
осознанно принятый trade-off ради единообразия со всем остальным кодом.

**2. Как работает фильтрация `findAll(status, clientId)` и почему `findAllByOrderByIdAsc`?**

`TaskServiceImpl.findAll(status, clientId, pageable)` — это по сути `if/else`-выбор между
восемью derived-query методами `TaskRepository` (`findAllByOrderByIdAsc`,
`findByStatusOrderByIdAsc`, `findByClientIdOrderByIdAsc`,
`findByStatusAndClientIdOrderByIdAsc` и их `...ClientOwnerId...`-варианты для MANAGER),
в зависимости от того, какие из `status`/`clientId` пришли непустыми и видит ли текущий
пользователь всё (ADMIN/VIEWER) или только своих клиентов (MANAGER — тогда ownerId
подставляется в запрос). Все методы названы `...OrderByIdAsc`, а не просто `findAll`/
`findByStatus`, потому что без явной сортировки Spring Data JPA/Hibernate не гарантирует
порядок строк между двумя вызовами — на паджинированном списке это давало нестабильный
порядок между страницами при повторном запросе. Сортировка по `id` — самый дешёвый
детерминированный вариант (первичный ключ уже проиндексирован).

**3. Почему из контроллеров DTO, а не entity, и как это связано с `FetchType.LAZY`?**

Entity никогда не покидают сервисный слой — контроллеры и публичные методы сервисов всегда
возвращают `ClientResponse`/`TaskResponse`/`NoteResponse` (проверяется P3 в `code-reviewer`).
Причины две: (1) DTO — это явный контракт API, отвязанный от структуры БД (можно поменять
entity, не сломав фронтенд, и наоборот); (2) `Task.client` и `Note.client` объявлены
`@ManyToOne(fetch = FetchType.LAZY)` — Hibernate подставляет туда прокси, который лениво
подгружает `Client` из БД только при первом обращении. Если бы `Task`-entity напрямую
сериализовался в JSON контроллером, Jackson попытался бы прочитать это поле уже за пределами
транзакции/Hibernate-сессии — либо `LazyInitializationException`, либо (без явного контроля)
случайный N+1 запрос на каждую задачу в списке. Маппинг в DTO происходит внутри
`@Transactional`-метода сервиса, пока сессия ещё открыта, и явно берёт только
`task.getClient().getId()` — ровно то, что нужно, без лишней подгрузки всего объекта `Client`.

**4. В чём был баг с null-статусом задачи и как починили?**

`TaskRequest.status`/`priority` изначально не имели `@NotNull` — если клиент присылал JSON
без поля `status`, Jackson молча ставил `null`, Bean Validation это пропускала (нет аннотации
— нечего проверять), и `null` долетал до `TaskServiceImpl`, где сохранялся как есть
(H2/Postgres допускают `NULL` в колонке без `NOT NULL`-констрейнта на уровне схемы) — задача
создавалась в неопределённом статусе, а фронтенд, ожидающий один из трёх enum-значений для
рендера бейджа, падал или показывал пустое поле. Починили добавлением `@NotNull` на оба поля
в `TaskRequest` (A1) — теперь такой запрос отклоняется на входе контроллера с `400 Bad
Request` ещё до того, как дойдёт до сервиса, плюс покрыли негативным тестом
(`create_missingStatus_returns400` и аналог для `priority`).

**5. Объясни race condition в `delete()` и выбранное решение.**

Первая версия `delete(id)` делала один вызов `repository.deleteById(id)` без предварительной
проверки существования. Если два запроса на удаление одного и того же `id` приходили
параллельно (или второй запрос приходил уже после того, как первый успешно удалил
запись), `deleteById` на несуществующем `id` в Spring Data JPA не бросает исключение — а
запрос ожидал `404 Not Found` на уже удалённый ресурс. Первый фикс (A2) — обернуть в
`try { deleteById } catch (EmptyResultDataAccessException) { throw 404 }`, что решало сам
404-контракт, но не убирало более широкую проблему: без предварительного `findById`
метод ничего не знает о ресурсе, который удаляет, — а как только в B2 появилась
владельческая проверка (`MANAGER` может удалять только своих клиентов), стало необходимо
сначала *прочитать* сущность, чтобы узнать её `owner`, и только потом решить — 404 (не
существует) или 403 (существует, но не твоя) или удаление. Итоговая, ныне действующая форма
всех трёх `delete()` (`Client`/`Task`/`Note`) — `findOrThrow(id)` → `ownershipGuard.check(...)`
→ `repository.delete(entity)`, что осознанно возвращает поведение к
read-then-delete (не строго атомарно на уровне БД — между чтением и удалением теоретически
возможна гонка на уровне двух параллельных транзакций), но это неизбежная цена корректных
404 и 403 в правильном порядке; полная атомарность потребовала бы отдельной блокировки
(`SELECT ... FOR UPDATE` / `@Version`), которую в код сознательно не добавляли — избыточно
для учебного MVP, но зафиксировано в разделе "Технический долг".

### По v2

**6. Где решается доступ по роли, а где по владению (MANAGER)? Что вернётся при нехватке прав и почему?**

Две независимые проверки, на разных уровнях: **роль** — `@PreAuthorize("hasAnyRole('ADMIN',
'MANAGER')")` на write-методах `ClientController`/`TaskController`/`NoteController` (и
`hasRole('ADMIN')` на `AuditController`) — это самый первый барьер, Spring Security
перехватывает запрос ещё до входа в метод контроллера; VIEWER не проходит этот барьер вообще
и получает `403` даже не долетев до сервиса. **Владение** — внутри сервиса, после того как
роль уже пройдена: `ownershipGuard.check(currentUser, client)` в
`findById`/`update`/`delete` всех трёх `*ServiceImpl` — ADMIN/VIEWER проходят всегда, MANAGER
проходит только если `client.getOwner()` равен текущему пользователю, иначе
`OwnershipGuard` бросает `AccessDeniedException`. Важен порядок: сервис сперва делает
`findOrThrow(id)` (сущности вообще нет → `404`), и только потом `ownershipGuard.check(...)`
(сущность есть, но чужая → `403`) — если поменять порядок, MANAGER мог бы по коду ответа
отличить "не существует" от "существует, но чужое", что раскрывает больше информации, чем
нужно. Оба исключения ловятся в `GlobalExceptionHandler`
(`AccessDeniedException` → 403, `EntityNotFoundException`-подобные → 404) и превращаются в
нейтральные JSON-сообщения без деталей о внутреннем состоянии.

**7. Где хранится и как проверяется JWT? Почему BCrypt, а не обычный хеш?**

Токен нигде не хранится на сервере — аутентификация полностью stateless. `JwtService`
подписывает токен HMAC-ключом (`app.jwt.secret`) и кладёт в него только `subject`
(username), `iat`, `exp` — никакой роли или прав внутри токена. `JwtAuthenticationFilter`
на каждый запрос читает заголовок `Authorization: Bearer <token>`, через `JwtService`
проверяет подпись и срок действия, достаёт username и **заново** идёт в БД за актуальным
`User` (через `CustomUserDetailsService`) — то есть роль/статус пользователя читаются
свежими на каждый запрос, а не из самого токена. Это сознательный выбор: если ADMIN у
пользователя отозвать роль или заблокировать аккаунт, эффект применится немедленно на
следующем же запросе, а не только после истечения уже выданного токена.
Пароль хранится как BCrypt-хеш (`BCryptPasswordEncoder`), а не обычный
хеш (SHA-256 и т.п.), потому что BCrypt — специально спроектированный для паролей
адаптивный алгоритм: он встраивает соль автоматически (одинаковые пароли дают разные хеши)
и намеренно медленный (настраиваемый cost-фактор), что делает brute-force и rainbow-table
атаки на утёкшую БД на порядки дороже — обычный быстрый хеш вроде SHA-256 для этого не
годится, он специально спроектирован быть *быстрым*.

**8. Как реализован rate limiting и почему такой лимит на login?**

`RateLimitFilter` (Bucket4j) стоит в цепочке фильтров Spring Security **перед**
`JwtAuthenticationFilter` — чтобы запрос, уже превысивший лимит, не тратил ресурсы на разбор
JWT. Два независимых in-memory набора бакетов (`ConcurrentHashMap`), каждый — классический
bucket с "жадным" рефиллом (`Bandwidth.classic(capacity, Refill.greedy(capacity, period))`,
токены прибывают непрерывно пропорционально прошедшему времени, а не разом по границе
периода): (1) `POST /api/auth/login` — 5 попыток/мин, ключ — IP-адрес (на момент логина
токена ещё нет, идентифицировать пользователя больше нечем); (2) весь остальной `/api/**`
(включая `register`) — 100 запросов/мин, ключ — `user:<username>`, если в запросе валидный
JWT, иначе `ip:<addr>`. При превышении — `429` + заголовок `Retry-After` (секунды до
следующего доступного токена) + `{"error": "Too many requests"}`. Лимит на login специально
намного строже (5 vs 100) — это самая частая мишень brute-force подбора пароля: пять попыток
в минуту делают перебор паролей практически бесполезным, не мешая при этом обычному
пользователю, который пару раз опечатался.

**9. Что и когда пишется в аудит-лог, откуда берётся "кто"?**

`AuditService.log(action, entity, entityId)` вызывается явно (без AOP/аннотаций) в конце
каждого успешного `create`/`update`/`delete` в `ClientServiceImpl`/`TaskServiceImpl`, в той же
`@Transactional`-транзакции, что и сама мутация — если транзакция откатится, аудит-запись
откатится вместе с ней, никогда не остаётся "осиротевшей" записи об операции, которая не
случилась. `action` — один из `CREATE`/`UPDATE`/`DELETE`/`STATUS_CHANGE` (обновление задачи,
у которой поменялся статус, логируется одной записью `STATUS_CHANGE`, а не отдельными
`STATUS_CHANGE`+`UPDATE`), `entity` — `"CLIENT"` или `"TASK"` (Note не аудируется — вне
скоупа). Поле `who` — денормализованная строка username, а не FK на таблицу `users`; она
берётся не из параметра, а сам `AuditServiceImpl` внутри `log(...)` вызывает
`currentUserService.getCurrentUser().getUsername()` — то есть "кто" всегда читается из
`SecurityContextHolder` в момент записи, а не передаётся вызывающим кодом (меньше шанс
случайно залогировать не того пользователя).

**10. Что показал `/security-check` после изменений и что сделали?**

На момент последнего прогона (после Part A v2, до Part B) `/security-check` фиксировал два
предупреждения: ⚠️1 — Spring Security в проекте отсутствовал вообще, все `/api/**` были
полностью открыты без аутентификации; ⚠️2 — `CorsConfig` использовал `allowedHeaders("*")` и
захардкоженный origin вместо конфигурируемого значения. Оба закрыты в последующих частях: ⚠️1
— Part B/B1 (JWT-аутентификация + `SecurityConfig` требует валидный токен на всех `/api/**`,
кроме `/api/auth/**`); ⚠️2 — Part A v2/A7 (явные `allowedHeaders` вместо wildcard, origin
вынесен в `app.cors.allowed-origins`). Третье, более мелкое предупреждение —
необработанные `DataIntegrityViolationException`/`HttpMessageNotReadableException` (утекали
как голый `500` со стек-трейсом) — закрыто добавлением явных `@ExceptionHandler` в
`GlobalExceptionHandler`, возвращающих `409`/`400` с нейтральными сообщениями без деталей
исключения. Как отмечено в разделе выше, сам код Part B (JWT/RBAC/rate limiting/audit) через
`/security-check` ещё не прогонялся — это остаётся открытым следующим шагом.

## Part G — об авторстве коммитов

Часть истории этого репозитория закоммичена под именем и почтой «Zalkar Aseinov» — это git-конфиг
ноутбука друга, с которого начиналась работа над частью задания. Само задание от начала до конца
выполнялось мной самостоятельно; остальные коммиты (начиная с Part A v2) сделаны уже с личного
аккаунта.
