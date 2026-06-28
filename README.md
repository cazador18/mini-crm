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

Покрытие: _TODO — указать % после прогона (например, через jacoco)._

Виды тестов:
- Unit — сервисы/бизнес-логика
- Интеграционные — REST API + БД (Testcontainers)
- Регрессионные — ключевые сценарии CRUD/фильтров

## Что освоено в Claude Code

> Заполнить по ходу работы — по каждой из 6 тем.

### 1. Модели (Opus / Sonnet / Haiku)
_TODO_

### 2. Skills
Свой Skill: `.claude/skills/<имя>/` — _TODO: что делает, как вызывается_

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
