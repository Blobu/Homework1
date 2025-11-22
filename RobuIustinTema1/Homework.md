# Homework — Secure Authentication + Dockerization

You will extend your **Lab 5** project to support **JWT authentication**, **refresh tokens**, and **secure Docker deployment**.

---

## Requirements

### Authentication APIs (4pts)

* ⚠️ Access tokens should add a claim for an array of roles

* Extend your `/api/auth/login` to return **two tokens**:

  * an **access token (JWT)**
  * a **refresh token**

Both should be stored in the database (with user and creation time).

* Add a `/api/auth/token` endpoint:

  * Receives a JSON body with `{ "accessToken": "...", "refreshToken": "..." }`
  * Marks both as **blacklisted**
  * Returns a new pair of tokens
  * ⚠️ This endpoint **must not use** the authentication filter (since the tokens may already be expired or revoked)

---

### Authentication Filter (2pts)

* Create a **JWT filter** that:

  * Extracts and validates the JWT from the `Authorization` header
  * Checks if it’s **not blacklisted**
  * If valid, sets the authenticated user in the `SecurityContext`
* Apply this filter to all endpoints except `/api/auth/register`, `/api/auth/login`, and `/api/auth/token`.

---

### Database (1pt)

Add a `Token` entity and repository for storing access/refresh tokens:

* Each token should have:

  * an `id`
  * access token value
  * refresh token value
  * a reference to the user (User --- one to many --- Token)
  * a `blacklisted` flag
  * a `createdAt` timestamp

---

### Dockerization (2pts)

* Create a **multi-stage Dockerfile**: _(1pt)_

  * **Stage 1**: build the app using Maven
  * **Stage 2**: run the app
  * Use an **Alpine-based image** (optional but encouraged)
  * Create and run the app as a **non-root user**

* Add a **new service** to your `docker-compose.yml` that: _(1pt)_

  * Builds and runs this Dockerfile
  * Connects to your existing PostgreSQL container

---

## Guidelines

* You can start directly from your **Lab 5 project**.
* Try to **use LLMs as little as possible**.
* If you get stuck, ask smart questions — verify best practices, not full solutions.

---

### Example LLM Prompt

> I’m adding JWT authentication and refresh tokens to my Spring Boot project.
> I want to know:
>
> 1. How to configure a JWT filter in Spring Boot 3 / Spring Security 6 (no deprecated code)
> 2. How to safely store and blacklist tokens in the database
> 3. What are the minimal test cases I should write for login, refresh, and revocation
>
> Please explain step-by-step how to approach this, with short examples and what I should search to understand each part.

---

## Deliverables

Submit:

* Updated source code
* Working `Dockerfile` and `docker-compose.yml`
* Short screenshots or console logs showing:

  * `/login` returning tokens
  * token added in [jwt.io](https://jwt.io)
  * `/token` issuing new tokens
  * Tokens marked as blacklisted in the database
