# Skill: CRUD Generator

Генерирует полный CRUD-слой для новой JPA-сущности по паттерну проекта mini-crm.

## Вызов

```
/crud-generator <EntityName> "<краткое описание>" [поля через запятую]
```

Пример:
```
/crud-generator Note "заметка, привязанная к Client" "content:String(@NotBlank), clientId:Long(@NotNull FK→Client)"
```

---

## Что генерируется (10 шагов)

Для сущности `Xyz` создаётся 9 новых файлов + обновляется 1 существующий.

| # | Файл | Назначение |
|---|---|---|
| 1 | `model/Xyz.java` | JPA-entity |
| 2 | `repository/XyzRepository.java` | Spring Data JPA |
| 2.5 | `db/migration/Vn__add_xyzs.sql` | Flyway-миграция — таблица под сущность |
| 3 | `dto/XyzRequest.java` | Входящий DTO (Bean Validation) |
| 4 | `dto/XyzResponse.java` | Исходящий DTO |
| 5 | `service/XyzService.java` | Интерфейс сервиса |
| 6 | `service/XyzServiceImpl.java` | Реализация сервиса |
| 7 | `controller/XyzController.java` | REST-контроллер |
| 8 | `exception/XyzNotFoundException.java` | Исключение 404 |
| ✏️ | `exception/GlobalExceptionHandler.java` | Добавить `XyzNotFoundException` в `@ExceptionHandler` |
| 9 | `test/.../service/XyzServiceImplTest.java` | Unit-тесты (Mockito) |

---

## Референсные файлы

Все паттерны берутся из существующего кода:

- **Entity**: `backend/src/main/java/com/evogroup/minicrm/model/Task.java`
- **Repository**: `backend/src/main/java/com/evogroup/minicrm/repository/TaskRepository.java`
- **DTO**: `backend/src/main/java/com/evogroup/minicrm/dto/TaskRequest.java`, `TaskResponse.java`
- **Service**: `backend/src/main/java/com/evogroup/minicrm/service/TaskService.java`, `TaskServiceImpl.java`
- **Controller**: `backend/src/main/java/com/evogroup/minicrm/controller/TaskController.java`
- **Exception**: `backend/src/main/java/com/evogroup/minicrm/exception/TaskNotFoundException.java`
- **Unit-тест**: `backend/src/test/java/com/evogroup/minicrm/service/TaskServiceImplTest.java`
- **GlobalExceptionHandler**: `backend/src/main/java/com/evogroup/minicrm/exception/GlobalExceptionHandler.java`
- **Migration**: `backend/src/main/resources/db/migration/` — посмотреть текущий самый старший
  `Vn__*.sql`, чтобы знать следующий свободный номер (не гадать и не доверять номеру, который
  мог быть указан в формулировке задачи)

---

## Пошаговые инструкции

### Шаг 1 — Entity (`model/Xyz.java`)

```java
@Entity
@Table(name = "xyzs")          // snake_case, множественное число
public class Xyz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Поля с @NotBlank / @NotNull / @Email где нужно
    // Временны́е поля: createdAt → Instant с @Column(updatable = false)
    // FK: @ManyToOne(fetch = FetchType.LAZY) + @JoinColumn(name = "..._id")

    // Геттеры и сеттеры для каждого поля (без Lombok — проект без него)
}
```

**Правила:**
- Всегда `FetchType.LAZY` для `@ManyToOne`
- `createdAt` инициализируется `= Instant.now()`; `@Column(updatable = false)`
- Нет `@NotNull` на `@ManyToOne` — вместо этого `@NotNull` на `clientId` в DTO

---

### Шаг 2 — Repository (`repository/XyzRepository.java`)

```java
public interface XyzRepository extends JpaRepository<Xyz, Long> {
    List<Xyz> findAllByOrderByIdAsc();

    // Если есть FK на Client — добавить:
    List<Xyz> findByClientIdOrderByIdAsc(Long clientId);
}
```

**Правило:** Никогда `findAll()` — всегда `findAllByOrderByIdAsc()` для детерминированного порядка.

---

### Шаг 2.5 — Flyway-миграция (`db/migration/Vn__add_xyzs.sql`)

**Правило нумерации (важно, частый источник ошибок):** сначала посмотреть самый большой
существующий `Vn__*.sql` в `backend/src/main/resources/db/migration/` (`ls` этой папки) и
использовать следующий свободный номер. **Не доверять** номеру версии, который мог быть
явно указан в формулировке задачи/тикета — если он не совпадает с тем, что реально следующее
в папке, использовать реальный следующий номер, а не запрошенный.

```sql
CREATE TABLE xyzs (
    id BIGSERIAL PRIMARY KEY,
    -- остальные поля сущности, snake_case, NOT NULL там же, где @NotBlank/@NotNull в Request DTO
    client_id BIGINT NOT NULL REFERENCES clients(id),   -- только если есть FK на Client
    created_at TIMESTAMP NOT NULL DEFAULT now()          -- только если есть createdAt
);

CREATE INDEX idx_xyzs_client_id ON xyzs(client_id);      -- только если есть FK и findByClientId...
```

**Правила:**
- Имя таблицы — snake_case, множественное число, совпадает с `@Table(name = "xyzs")` из Шага 1.
- Колонки — snake_case (`client_id`, не `clientId`), типы соответствуют полям entity.
- `NOT NULL` — ровно там, где в Entity/Request реально обязательность (не шире и не уже
  Bean Validation из Шага 3).
- FK — `REFERENCES <parent_table>(id)` + индекс на FK-колонке, если Repository (Шаг 2)
  использует `findByClientId...`.
- Эта миграция обязательна, даже если локальная разработка идёт на H2 с `ddl-auto: update`
  (профили `dev`/`test`) — профили `docker`/`integration-test` работают строго через Flyway
  (`ddl-auto: validate`), без неё схема там не создастся и Hibernate провалит валидацию
  маппинга при старте.

---

### Шаг 3 — XyzRequest (`dto/XyzRequest.java`)

- `@NotBlank` на строковые обязательные поля
- `@NotNull` на `clientId` (Long), если есть FK
- Нет бизнес-логики; только поля + геттеры/сеттеры

---

### Шаг 4 — XyzResponse (`dto/XyzResponse.java`)

- Зеркало entity, но без JPA-аннотаций
- FK представлен как `Long clientId` (не объект `Client`)
- Временны́е поля: `Instant createdAt`
- Только поля + геттеры/сеттеры

---

### Шаг 5 — Service interface (`service/XyzService.java`)

Стандартный набор методов:

```java
public interface XyzService {
    XyzResponse create(XyzRequest request);
    XyzResponse findById(Long id);
    List<XyzResponse> findAll(/* опциональные фильтры, напр. Long clientId */);
    XyzResponse update(Long id, XyzRequest request);
    void delete(Long id);
}
```

---

### Шаг 6 — ServiceImpl (`service/XyzServiceImpl.java`)

```java
@Service
@Transactional
public class XyzServiceImpl implements XyzService {

    private final XyzRepository xyzRepository;
    // + репозиторий зависимой сущности (напр. ClientRepository), если есть FK

    // Конструктор инжекции (не @Autowired на поле)

    // create: найти зависимую сущность через findOrThrow, замапить request→entity, save
    // findById: findOrThrow(id)
    // findAll: использовать findAllByOrderByIdAsc() или findByClientIdOrderByIdAsc()
    // update: findOrThrow(id), замапить request→entity, save
    // delete: findOrThrow(id), deleteById

    private Xyz findOrThrow(Long id) {
        return xyzRepository.findById(id)
                .orElseThrow(() -> new XyzNotFoundException(id));
    }

    // Если есть FK на Client:
    private Client findClientOrThrow(Long clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));
    }

    private void mapRequest(XyzRequest req, Xyz entity, /* зависимости */) {
        // маппинг поле за полем
    }

    private XyzResponse toResponse(Xyz entity) {
        XyzResponse r = new XyzResponse();
        // маппинг поле за полем, включая entity.getClient().getId() → clientId
        return r;
    }
}
```

**Правила:**
- `@Transactional` на классе; `@Transactional(readOnly = true)` на findById/findAll
- Никогда не возвращать JPA entity из публичных методов — только DTO
- Проверка существования зависимых сущностей через `findClientOrThrow` → 404

---

### Шаг 7 — Controller (`controller/XyzController.java`)

```java
@RestController
@RequestMapping("/api/xyzs")
public class XyzController {

    private final XyzService service;

    public XyzController(XyzService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<XyzResponse> create(@Valid @RequestBody XyzRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @GetMapping
    public ResponseEntity<List<XyzResponse>> findAll(
            @RequestParam(required = false) Long clientId) {   // если есть FK
        return ResponseEntity.ok(service.findAll(clientId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<XyzResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<XyzResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody XyzRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

**Правила:**
- HTTP-маппинг только; никакой бизнес-логики
- `@Valid` на всех `@RequestBody`
- `POST` → 201 Created; `DELETE` → 204 No Content; остальные → 200

---

### Шаг 8 — Exception (`exception/XyzNotFoundException.java`)

```java
public class XyzNotFoundException extends RuntimeException {
    public XyzNotFoundException(Long id) {
        super("Xyz not found: " + id);
    }
}
```

---

### Шаг 8.5 — Обновить `GlobalExceptionHandler.java`

Добавить `XyzNotFoundException.class` в аргументы существующего `@ExceptionHandler`:

```java
@ExceptionHandler({ClientNotFoundException.class, TaskNotFoundException.class, XyzNotFoundException.class})
```

---

### Шаг 9 — Unit-тест (`test/.../service/XyzServiceImplTest.java`)

Структура теста по образцу `TaskServiceImplTest`:

```java
@ExtendWith(MockitoExtension.class)
class XyzServiceImplTest {

    @Mock XyzRepository xyzRepository;
    @Mock ClientRepository clientRepository;   // если есть FK

    @InjectMocks XyzServiceImpl service;

    private Client client;
    private Xyz entity;
    private XyzRequest request;

    @BeforeEach
    void setUp() { /* инициализация client, entity, request */ }

    // Обязательные тест-методы:
    // create_mapsRequestAndReturnsResponse
    // create_throwsClientNotFound_whenClientMissing  (если есть FK)
    // findById_returnsResponse_whenExists
    // findById_throwsNotFound_whenMissing
    // findAll_noFilter_returnsAll
    // findAll_byClientId_filtersCorrectly  (если есть FK)
    // update_updatesFieldsAndReturnsResponse
    // delete_callsDeleteById_whenExists
}
```

**Правила:**
- AssertJ (`assertThat`, `assertThatThrownBy`)
- Проверяй `verify(repo, never()).save(any())` в негативных тестах create
- Стабы через `when(...).thenReturn(...)` только для вызываемых методов

---

## Чеклист перед завершением

- [ ] Все 9 файлов созданы в правильных пакетах
- [ ] `GlobalExceptionHandler` обновлён
- [ ] Номер Flyway-миграции проверен по факту (самый старший существующий `Vn__*.sql` + 1),
      а не взят из формулировки задачи
- [ ] Схема в `Vn__add_xyzs.sql` совпадает с Entity (типы, NOT NULL, FK, индексы)
- [ ] `findAll()` нигде не используется — только `findAllByOrderByIdAsc()`
- [ ] Entity никогда не возвращается из контроллера напрямую
- [ ] `FetchType.LAZY` на всех `@ManyToOne`
- [ ] Unit-тест компилируется и покрывает все публичные методы сервиса
- [ ] Запущен `./mvnw test` — BUILD SUCCESS
