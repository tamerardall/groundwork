---
name: java-package-structure
description: Use when creating or extending a Java or Spring Boot project, adding a new entity, endpoint, controller, service or repository, deciding which package a Java class belongs in, or when a controller and service end up in the same file.
license: MIT
---

# Java Package Structure

A module-based package layout for Java / Spring Boot. Each business module owns its own layers (controller, service, repository, model, mapper). Code shared across modules lives in `common`.

This skill applies the core `structure-rules` (S-01 to S-05) to Java. **REQUIRED BACKGROUND:** structure-rules.

## The layout

```
com.company.project
├── ProjectApplication.java              # @SpringBootApplication, bootstrap only
├── common/                              # shared by 2+ modules, domain-free
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── DatabaseConfig.java
│   │   └── SwaggerConfig.java
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java  # @RestControllerAdvice
│   │   ├── ResourceNotFoundException.java
│   │   └── ValidationException.java
│   ├── security/
│   │   └── JwtTokenProvider.java
│   └── util/
│       └── DateUtil.java
├── user/
│   ├── controller/
│   │   └── UserController.java
│   ├── service/
│   │   ├── UserService.java
│   │   └── UserServiceImpl.java
│   ├── repository/
│   │   └── UserRepository.java
│   ├── model/
│   │   ├── entity/
│   │   │   └── User.java
│   │   └── dto/
│   │       ├── UserDTO.java
│   │       └── CreateUserRequest.java
│   ├── mapper/
│   │   └── UserMapper.java
│   └── security/
│       └── CustomUserDetailsService.java  # user-specific, so it lives in user/
└── product/
    ├── controller/
    │   └── ProductController.java
    ├── service/
    │   ├── ProductService.java
    │   └── ProductServiceImpl.java
    ├── repository/
    │   └── ProductRepository.java
    ├── model/
    │   ├── entity/
    │   │   └── Product.java
    │   └── dto/
    │       └── ProductDTO.java
    └── mapper/
        └── ProductMapper.java
```

## Rules

### J-01: One public top-level class per file
Never put a controller and a service, or an entity and its DTO, in the same `.java` file. Nested static classes are allowed only when they are private to the enclosing class.

### J-02: A module contains only its own classes
Everything under `user/` concerns users only. A class used by exactly one module lives in that module, even if it looks generic. A class moves to `common/` only when two or more modules use it **and** it contains no module-specific logic.

### J-03: Modules talk through services only
`product` may call `UserService`. It must not inject `UserRepository`, use `User` entities, or call `UserMapper`. Only DTOs and service interfaces cross module boundaries.

### J-04: Each layer does one job

| Package | Contains | Never contains |
|---|---|---|
| `controller` | `@RestController`: maps HTTP to service calls, `@Valid` on request DTOs, returns DTOs and status codes | business rules, repository calls, entity returns, try/catch for error responses |
| `service` | `UserService` interface + `UserServiceImpl`: business rules, `@Transactional` boundaries | HTTP types (`HttpServletRequest`, `ResponseEntity`) |
| `repository` | Spring Data interfaces, custom queries | business rules |
| `model/entity` | JPA `@Entity` classes | API annotations, presentation logic |
| `model/dto` | request/response records or classes | JPA annotations |
| `mapper` | entity ↔ DTO conversion (MapStruct or plain) | queries, business rules |
| `security` (module) | security code tied to this module | generic token handling (that goes in `common/security`) |
| `common/exception` | shared exceptions + `GlobalExceptionHandler` | module-specific business logic |

### J-05: Entities never leave the service layer
Controllers receive and return DTOs. Conversion happens in the module's `mapper`.

### J-06: Errors are handled centrally
Services throw exceptions (`ResourceNotFoundException`, ...). `common/exception/GlobalExceptionHandler` turns them into HTTP responses. Controllers do not catch and convert them.

### J-07: No empty packages
Create a layer package only when it holds a class. A module without a mapper has no `mapper/` folder.

### J-08: The application class only boots
`ProjectApplication.java` holds `main` and annotations. No beans, endpoints or logic.

### J-09: Single-use private methods are inlined (S-03)
A private method in a service or controller that has one call site is written in place, unless an S-03 exception applies.

## Adding a new module

For a new business concept `quote`:

1. `quote/model/entity/Quote.java`
2. `quote/model/dto/` with the request/response DTOs the endpoints need
3. `quote/repository/QuoteRepository.java`
4. `quote/service/QuoteService.java` + `QuoteServiceImpl.java`
5. `quote/mapper/QuoteMapper.java`
6. `quote/controller/QuoteController.java`

Create only the DTOs and packages that are actually used (J-07). See `examples.md` for a before/after refactor.

## Common mistakes

| Mistake | Fix |
|---|---|
| `@Service` class declared at the bottom of `QuoteController.java` | Move it to `quote/service/QuoteServiceImpl.java` (J-01) |
| Top-level `controller/`, `service/` packages holding every module | Move each class under its module: `user/controller/...` (J-02) |
| `OrderServiceImpl` injects `ProductRepository` | Inject `ProductService` instead (J-03) |
| Controller returns `ResponseEntity<User>` | Return `UserDTO` via `UserMapper` (J-05) |
| `try { ... } catch (NotFound e) { return ResponseEntity.status(404)... }` in a controller | Let it throw and handle it in `GlobalExceptionHandler` (J-06) |
| `common/util/QuoteUtil.java` used only by quote | Move the code into `quote/service` (J-02, S-04) |
