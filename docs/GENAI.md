# GenAI exercise: task management API

> *Using a GenAI coding tool, write the prompt you would use to generate a RESTful API for a task
> management system (CRUD; title, description, status, due_date; tasks belong to a user). Show the output,
> and describe how you validated it, corrected it, and handled edge cases, authentication and validation.*

Tool: **Claude Code** (agentic coding assistant in the terminal/IDE).

---

## 1. The prompt

A good prompt is a **spec**, not a wish. It states the stack, the architecture, the contract, the rules, the
edge cases and what "done" means, so the model doesn't have to guess (because it will guess *something*).

```text
Create a Spring Boot 3.5 / Java 21 REST API for task management, as a new Maven project "task-api".

ARCHITECTURE
- Clean Architecture: domain (plain Java, no Spring/JPA annotations), application (use cases + ports),
  infrastructure (web, persistence, security). Add an ArchUnit test that enforces the dependency rule.
- PostgreSQL via Spring Data JPA, schema owned by Flyway (ddl-auto=validate). H2 for tests.

DOMAIN
- Task: id (UUID), ownerId, title (required, 1-120 chars, trimmed), description (optional, max 2000),
  status (TODO | IN_PROGRESS | DONE), dueDate (LocalDate, optional), createdAt, updatedAt, version.
- Rules: a new task starts as TODO. dueDate cannot be in the past when creating or changing it.
  Status transitions: TODO -> IN_PROGRESS -> DONE, and DONE -> TODO (reopen). Nothing else.
- Assume a User exists; the current user's id comes from the JWT "sub" claim.

API (JSON, under /api/tasks; every endpoint requires a Bearer JWT)
- POST   /api/tasks                -> 201 + Location header
- GET    /api/tasks?status=&dueBefore=&page=0&size=20&sort=dueDate,asc -> paginated, ONLY the caller's tasks
- GET    /api/tasks/{id}
- PUT    /api/tasks/{id}           -> full update, requires "version" (optimistic locking)
- PATCH  /api/tasks/{id}/status    -> {"status": "..."} only
- DELETE /api/tasks/{id}           -> 204
- A task owned by another user must return 404 (not 403): don't reveal that it exists.

SECURITY
- OAuth2 resource server: validate JWTs with a JWKS URI from configuration (issuer + audience checks).
  Do NOT implement login or store passwords.

ERRORS
- RFC 9457 ProblemDetail for every error, with an "errors" map of field -> message for validation failures.
- 400 malformed JSON / invalid values / illegal status transition, 401, 404, 409 stale version,
  415, 500 with a generic message (log the details, never return stack traces).

TESTS (write them first for the domain rules)
- Unit tests for the domain (transitions, due date, title rules) and use cases (Mockito).
- @DataJpaTest for the repository, including the owner filter.
- MockMvc tests with spring-security-test jwt() for every status code above, including
  "user B cannot read/update/delete user A's task".

CONSTRAINTS
- No Lombok. Records for DTOs. Constructor injection only. Inject java.time.Clock (no LocalDate.now()).
- Don't add dependencies beyond spring-boot starters, flyway, postgresql, h2, archunit.
- When done: run `mvn test`, and report what passed and anything you could not verify.
```

**Why it's written this way:**
- **Edge cases spelled out** (ownership → 404, past due dates, transitions, stale versions). Unstated cases
  are exactly where generated code is weakest.
- **Security boundaries** ("validate JWTs, don't implement login"), so the model doesn't invent a homemade
  auth system.
- **"Inject Clock"**, so date rules are testable, and **"no Lombok"**, to keep the code explicit and reviewable.
- **A definition of done** ("run `mvn test` and report"). Agentic tools can execute and self-correct;
  asking for it turns "looks right" into "proved right".

---

## 2. Representative output

Trimmed sample of what the tool produced, after review (corrections are marked in §3).

**Domain: plain Java, rules in the entity**

```java
public final class Task {

    private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED = Map.of(
            TaskStatus.TODO, Set.of(TaskStatus.IN_PROGRESS),
            TaskStatus.IN_PROGRESS, Set.of(TaskStatus.DONE),
            TaskStatus.DONE, Set.of(TaskStatus.TODO));

    private final UUID id;
    private final String ownerId;
    private String title;
    private String description;
    private TaskStatus status;
    private LocalDate dueDate;
    private Long version;
    // createdAt, updatedAt ...

    public static Task create(String ownerId, String title, String description, LocalDate dueDate, Clock clock) {
        Task task = new Task(UUID.randomUUID(), ownerId);
        task.status = TaskStatus.TODO;
        task.apply(title, description, dueDate, clock);
        return task;
    }

    public void changeStatus(TaskStatus next, Clock clock) {
        if (status == next) {
            return;                                    // idempotent: PATCH with the same status is fine
        }
        if (!ALLOWED.get(status).contains(next)) {
            throw new ValidationException("status", "cannot change from " + status + " to " + next);
        }
        status = next;
        touch(clock);
    }

    private void apply(String title, String description, LocalDate dueDate, Clock clock) {
        Map<String, String> errors = new LinkedHashMap<>();
        String trimmed = title == null ? "" : title.trim();
        if (trimmed.isEmpty() || trimmed.length() > 120) errors.put("title", "must be 1-120 characters");
        if (description != null && description.length() > 2000) errors.put("description", "must be at most 2000 characters");
        if (dueDate != null && !dueDate.equals(this.dueDate) && dueDate.isBefore(LocalDate.now(clock))) {
            errors.put("dueDate", "must not be in the past");
        }
        if (!errors.isEmpty()) throw new ValidationException(errors);
        this.title = trimmed;
        this.description = description;
        this.dueDate = dueDate;
        touch(clock);
    }
}
```

**Use case: ownership enforced in the application layer**

```java
public class TaskService implements ManageTasksUseCase {

    public Task get(String callerId, UUID id) {
        return repository.findByIdAndOwnerId(id, callerId)           // owner filter in the query
                .orElseThrow(() -> new NotFoundException("Task", id)); // other users' tasks -> 404
    }

    public Task update(String callerId, UUID id, TaskChanges changes, long expectedVersion) {
        Task task = get(callerId, id);
        if (task.getVersion() != expectedVersion) {
            throw new ConflictException("Task was modified by someone else");
        }
        task.update(changes.title(), changes.description(), changes.dueDate(), clock);
        return repository.save(task);
    }
}
```

**Controller: the owner comes from the token, never from the request body**

```java
@PostMapping
ResponseEntity<TaskResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateTaskRequest body) {
    Task task = tasks.create(jwt.getSubject(), body.title(), body.description(), body.dueDate());
    return ResponseEntity.created(URI.create("/api/tasks/" + task.getId())).body(TaskResponse.from(task));
}
```

**Test: the edge case that matters most**

```java
@Test
void anotherUsersTaskIsNotFound() throws Exception {
    UUID id = createTaskAs("alice");

    mvc.perform(get("/api/tasks/{id}", id).with(jwt().jwt(j -> j.subject("bob"))))
       .andExpect(status().isNotFound());
}
```

---

## 3. How I validated, corrected and improved the output

I treat AI output like a pull request from a fast but overconfident teammate: **read every line, run
everything, and test the edges myself.**

### Validation process
1. **Run it.** `mvn test` plus starting the app, before reading in depth. Code that doesn't run isn't worth reviewing.
2. **Review the diff against the spec**, one requirement at a time (a checklist made from the prompt).
3. **Probe the edges by hand** (Postman/curl): another user's task, past dates, stale versions, broken JSON,
   no token, an expired token.
4. **Check the architecture** with the ArchUnit test, and by grepping the domain for `org.springframework`.
5. **Ask the model to review its own code** against the spec in a fresh prompt ("list every requirement
   and where it's implemented; flag gaps"). It finds real issues, but **I verify each claim** instead of
   trusting the summary.

### What was wrong in the first draft, and how it was fixed

| Issue in the generated code | Why it matters | Fix |
|---|---|---|
| `findById(id)` then `if (!task.getOwnerId().equals(caller)) throw new AccessDeniedException()` → **403** | Leaks that the task exists (ID enumeration); the spec said 404 | Owner filter in the query: `findByIdAndOwnerId`, which returns 404 |
| `ownerId` accepted in the **request body** of POST | Any user could create tasks for someone else | Removed from the DTO; taken from `jwt.getSubject()` |
| `LocalDate.now()` in the entity | Untestable, timezone-dependent | Injected `Clock` |
| Past-date check also ran on **every update** | Editing the title of an overdue task failed | Check only when the due date *changes* |
| `@Version` on the JPA entity but the client's version was ignored | Lost updates in concurrent edits | Explicit `expectedVersion` comparison, plus the JPA check as a backstop |
| `catch (Exception e) { return ResponseEntity.status(500).body(e.getMessage()); }` | Leaks internals (SQL, stack details) | Global `@RestControllerAdvice` returning ProblemDetail; details only in logs |
| `HttpMessageNotReadableException` fell through to 500 | Malformed JSON must be 400 | Explicit handler → 400 |
| Status `PATCH` from `DONE` to `IN_PROGRESS` was allowed | Broke the transition rules | Transition table in the domain + parameterized tests for every pair |
| Added **Lombok** and **ModelMapper** "for convenience" | Hidden magic, against the constraints | Records + explicit mapping |
| Tests asserted only status codes | A 200 with the wrong body would pass | Asserted bodies, the owner filter and the `errors` map |

### Edge cases, authentication and validation
- **Authentication:** delegated to a resource server validating JWTs (JWKS, issuer, audience). No passwords
  in this service, so there is less to get wrong.
- **Authorization:** ownership enforced in the **query**, not after loading the data, so it can't be
  forgotten in one endpoint.
- **Validation in two layers:** Bean Validation on DTOs for shape (missing or blank fields, sizes) → fast
  400s; domain rules in the entity (transitions, dates) → enforced regardless of the entry point.
- **Edge cases covered by tests:** empty/whitespace title, 121-character title, past due date on create,
  overdue task edited, every status transition pair, idempotent status change, stale version, unknown id,
  another user's id, malformed JSON, missing token, page size over the limit.

---

## 4. How GenAI was used in this project

I defined the architecture and the requirements: the split into gateway, auth server and resource server,
the OAuth2 flow with local JWT validation through JWKS at the gateway, and the user stories. I gave these to
Claude Code as input. Based on them, the AI generated code, especially the frontend and the test cases.

I reviewed the output, asked for changes where it didn't fit the design (for example, moving identity into
a dedicated auth service), and validated each step by running the tests and the services.
