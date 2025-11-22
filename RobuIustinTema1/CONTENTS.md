
# 🧪 Laboratory 5 — Migrating the Authentication API from File Storage to PostgreSQL (with JPA)

  

## 🎯 Objective

  

In Lab 4 you built a Spring Boot Authentication API (register + login) and stored users in a **JSON file** using a custom repository.

  

In **Lab 5** you will upgrade that project to use a **real database**:

- add **JPA / Hibernate** support

- run **PostgreSQL in a Docker container**

- create a **DB-backed repository** that implements the same `UserRepository` interface

- keep the **same controllers and DTOs** from Lab 4

  

At the end, the API will work exactly the same, but users will be persisted in PostgreSQL instead of a file.

  


  

## 0) What You Start With (from Lab 4)

  

You should already have:

  

- `User` class (plain POJO, no JPA yet)

- `Role` enum

- `UserRepository` interface

- `UserRepositoryFile` (implementation using JSON file)

- `UserService` / `UserServiceImpl` with `register(...)` and `login(...)`

- `AuthController` with `/api/auth/register` and `/api/auth/login`

- DTOs: `RegisterRequest`, `LoginRequest`, `UserResponse`, `LoginResponse`

- password encoding via `PasswordEncoder`

  

We will **not** change the controller or the DTOs — only the persistence layer and the entity.

  


  

## 1) Add DB & JPA Dependencies

  

Open your `pom.xml` and add the following dependencies inside `<dependencies>`:

  

```xml

<!-- JPA / Hibernate support -->

<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- PostgreSQL JDBC driver -->

<dependency>
	<groupId>org.postgresql</groupId>
	<artifactId>postgresql</artifactId>
	<scope>runtime</scope>
</dependency>

```

  

**Why?**

- `spring-boot-starter-data-jpa` gives you `jakarta.persistence.*` annotations (`@Entity`, `@Id`, etc.) and Hibernate.

- `postgresql` lets Spring Boot connect to your Postgres container.

  

Reload Maven.

  


  

## 2) Run PostgreSQL in Docker

  

Create a file named `docker-compose.yml` in the root of your project (same level as `pom.xml`):

  

```yaml
version: "3.8"
services:
  postgres:
    image: postgres:16
    container_name: auth-postgres
    restart: always
    environment:
      POSTGRES_DB: authdb
      POSTGRES_USER: authuser
      POSTGRES_PASSWORD: authpass
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

  

Start the database:

  

```bash

docker compose up -d

```

  

Now you have a Postgres database running at:

  

- host: `localhost`

- port: `5432`

- database: `authdb`

- user: `authuser`

- password: `authpass`

  

We will connect Spring to this DB next.

  


  

## 3) Configure Spring to Use PostgreSQL

  

Open `src/main/resources/application.properties` and add:

  

```properties

spring.application.name=authentication-api

  

spring.datasource.url=jdbc:postgresql://localhost:5432/authdb

spring.datasource.username=authuser

spring.datasource.password=authpass

  

spring.jpa.hibernate.ddl-auto=update

spring.jpa.show-sql=true

spring.jpa.properties.hibernate.format_sql=true

```

  

**What this does:**

- tells Spring where the DB is

- tells Hibernate to create/update tables automatically

- prints SQL to console so students can see what’s happening

  


  

## 4) Update the `User` Entity for JPA

Replace your old `User` class with this version:

```java
package unitbv.devops.authenticationapi.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", updatable = false, nullable = false)
	private String id;

	@Column(nullable = false, unique = true)
	private String username;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "role")
	private Set<Role> roles;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private boolean enabled;
}

```

Your `Role` enum remains simple:

```java
package unitbv.devops.authenticationapi.user.entity;

public enum Role {
	USER,
	ADMIN

}
```

## 5) Create the JPA Repository


```java
package unitbv.devops.authenticationapi.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import unitbv.devops.authenticationapi.user.entity.User;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<User, String> {

	Optional<User> findByUsername(String username);

	Optional<User> findByEmail(String email);

	boolean existsByUsername(String username);

	boolean existsByEmail(String email);

}
```

## 6) Implement the DB Repository Adapter

```java
package unitbv.devops.authenticationapi.user.repository.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import unitbv.devops.authenticationapi.user.entity.User;
import unitbv.devops.authenticationapi.user.repository.UserJpaRepository;
import unitbv.devops.authenticationapi.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
@RequiredArgsConstructor
public class UserRepositoryDb implements UserRepository {

	private final UserJpaRepository jpaRepo;

	@Override
	public User save(User user) {
		return jpaRepo.save(user);
	}

	@Override
	public Optional<User> findById(String id) {
		return jpaRepo.findById(id);
	}

	@Override
	public Optional<User> findByUsername(String username) {
		return jpaRepo.findByUsername(username);
	}

	@Override
	public Optional<User> findByEmail(String email) {
		return jpaRepo.findByEmail(email);
	}

	@Override
	public boolean existsByUsername(String username) {
		return jpaRepo.existsByUsername(username);
	}

	@Override
	public boolean existsByEmail(String email) {
		return jpaRepo.existsByEmail(email);
	}

	@Override
	public List<User> findAll() {
		return jpaRepo.findAll();
	}

	@Override
	public void deleteById(String id) {
		jpaRepo.deleteById(id);
	}
}
```

## 7) Update the `UserServiceImpl`

  

```java
package unitbv.devops.authenticationapi.user.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import unitbv.devops.authenticationapi.dto.auth.LoginRequest;
import unitbv.devops.authenticationapi.dto.auth.RegisterRequest;
import unitbv.devops.authenticationapi.dto.auth.UserResponse;
import unitbv.devops.authenticationapi.user.entity.Role;
import unitbv.devops.authenticationapi.user.entity.User;
import unitbv.devops.authenticationapi.user.mapper.UserMapper;
import unitbv.devops.authenticationapi.user.repository.UserRepository;
import unitbv.devops.authenticationapi.user.service.UserService;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService {
	private final UserRepository users;
	private final PasswordEncoder encoder;

	public UserServiceImpl(UserRepository users, PasswordEncoder encoder) {
		this.users = users;
		this.encoder = encoder;
	}

	@Override
	public Optional<UserResponse> register(RegisterRequest req) {

	if (users.existsByUsername(req.username()) || users.existsByEmail(req.email())) {
		return Optional.empty();
	}

	User u = User.builder()
	.username(req.username())
	.email(req.email())
	.passwordHash(encoder.encode(req.password()))
	.roles(new HashSet<>(Set.of(Role.USER)))
	.createdAt(Instant.now())
	.enabled(true)
	.build();

	u = users.save(u);

	return Optional.of(UserMapper.toResponse(u));
}

	@Override
	public Optional<UserResponse> login(LoginRequest req) {

	Optional<User> found = users.findByUsername(req.usernameOrEmail());

	if (found.isEmpty()) {
		found = users.findByEmail(req.usernameOrEmail());
	}

	if (found.isEmpty()) {
		return Optional.empty();
	}

	User u = found.get();

	if (!encoder.matches(req.password(), u.getPasswordHash())) {
		return Optional.empty();
	}

	return Optional.of(UserMapper.toResponse(u));
	}
}
```
## 8) Test Everything

Run:

```bash

docker compose up -d

```

Start the Spring Boot app.

Register a user:

```json

POST /api/auth/register

{

"username": "student1",

"email": "student1@example.com",

"password": "mypassword"

}

```

Login:

```json

POST /api/auth/login

{

"usernameOrEmail": "student1",

"password": "mypassword"

}

```

Check the database:

```bash

docker exec -it auth-postgres psql -U authuser -d authdb

select * from users;

select * from user_roles;

```

## 9) Troubleshooting

| Problem | Possible Fix |
|----------|---------------|
| Connection refused | Database not running; run `docker compose up -d` |
| `relation users does not exist` | Add `spring.jpa.hibernate.ddl-auto=update` |
| IntelliJ cannot find `jakarta.persistence` | Missing JPA dependency |
| Duplicate username/email | Your `existsBy...` methods protect against this |

## 10) Homework
 
 Please check the platform for the homework details as well as what you need to do, what to upload and what the deadline is.

