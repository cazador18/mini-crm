# Mini-CRM / Task Tracker — контекст проекта для Claude Code

## Что это
Учебный MVP-проект (фуллстек): мини-CRM с клиентами и задачами.
Цель — освоить Claude Code (модели, skills, subagents, MCP, slash-команды, плагины),
а не построить прод-систему. См. `docs/ТЗ.md` (или исходное задание) для полного контекста.

## Стек
- **Backend**: Java 21, Spring Boot 3, Spring Data JPA, REST API
- **DB**: PostgreSQL (через Docker), профиль `test` — H2
- **Frontend**: Next.js (App Router) + TypeScript, Tailwind
- **Тесты**: JUnit 5, Mockito, Spring Boot Test, Testcontainers (интеграционные)
- **Инфраструктура**: Docker + docker-compose (backend + frontend + db одной командой)

## Структура репозитория
```
mini-crm/
├── backend/                 # Spring Boot приложение
│   ├── src/main/java/com/evogroup/minicrm/
│   │   ├── controller/      # REST контроллеры (тонкие, без бизнес-логики)
│   │   ├── service/         # бизнес-логика
│   │   ├── repository/      # Spring Data JPA репозитории
│   │   ├── model/           # JPA-сущности (Client, Task, статусы/приоритеты)
│   │   ├── dto/             # DTO для запросов/ответов (не отдаём Entity наружу)
│   │   ├── exception/       # кастомные исключения + @ControllerAdvice
│   │   └── config/          # конфигурация (CORS, OpenAPI и т.п.)
│   ├── src/test/            # unit + интеграционные тесты
│   └── Dockerfile
├── frontend/                # Next.js приложение
│   ├── app/                 # страницы (App Router): clients, tasks, dashboard
│   ├── components/          # UI-компоненты
│   ├── lib/                 # API-клиент, типы, утилиты
│   └── Dockerfile
├── docker-compose.yml
├── .claude/
│   ├── skills/               # кастомные Skills проекта
│   ├── agents/                # кастомные субагенты
│   └── commands/              # кастомные slash-команды
└── README.md
```

## Архитектурные принципы (SOLID)
- Контроллер: только маппинг HTTP ↔ DTO, без логики.
- Сервис: вся бизнес-логика, одна ответственность на класс.
- Репозиторий: только доступ к данным (Spring Data JPA).
- Зависимости между слоями — через интерфейсы там, где это оправдано (например, `ClientService` интерфейс + `ClientServiceImpl`).
- DTO отдельно от Entity — не возвращать JPA-сущности напрямую из контроллеров.

## Домейн
- **Client**: id, name, email, phone, createdAt
- **Task**: id, title, description, status (NEW / IN_PROGRESS / DONE), priority (LOW / MEDIUM / HIGH), deadline, clientId

## Команды разработки
```bash
# Запуск всего стека
docker compose up --build

# Backend локально
cd backend && ./mvnw spring-boot:run

# Frontend локально
cd frontend && npm run dev

# Тесты backend
cd backend && ./mvnw test
```

## Чек-лист перед сдачей
- [ ] `docker compose up` поднимает весь стек
- [ ] CRUD клиентов и задач работает end-to-end через UI
- [ ] Фильтры по статусу/клиенту + дашборд работают
- [ ] Минимум 1 свой Skill и 1 свой субагент применены, описаны в README
- [ ] Тесты: unit / интеграционные / регрессионные, % покрытия в README
- [ ] Пройдены `/review` и `/security-review`, замечания исправлены
- [ ] README заполнен (что сделано, как запустить, что освоено в Claude Code)

## Заметки для Claude Code
- Перед крупными изменениями использовать режим планирования (plan mode).
- Коммитить осмысленно и часто.
- Не писать код руками — формулировать задачи и итеративно проверять результат.
