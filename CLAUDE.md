# Project: Multi-user weighted decision matrix tool

This project supports the creation of weighted decision matrices when multiple people are involved.

## General Guidance
- ALWAYS ask for clarification rather than making assumptions.
- If you're having trouble with something, it's ok to stop and ask for help.
- Focus on getting working, tested code before optimizing or adding features.

## Quick Reference Commands
- **Run app in dev mode**: `DM_DEV_MODE=true ./gradlew run`
- **Run tests**: `./gradlew test`
- **Run with mock OAuth**: `DM_DEV_MODE=false DM_MOCK_OAUTH_SERVER=true DM_OAUTH_ISSUER_URL=http://localhost:8081 DM_OAUTH_CLIENT_ID=test-client DM_OAUTH_CLIENT_SECRET=test-secret DM_OAUTH_REDIRECT_URI=http://localhost:9000/auth/callback ./gradlew run`

## Architecture & Technology Stack

**Backend & UI**
- Kotlin for backend logic and server-side UI generation
- kotlinx.html for HTML generation
- htmx for frontend interactivity
- http4k for HTTP server framework

**Data & Build**
- SQLite for database storage
- Gradle with Kotlin DSL for build automation
- Use Gradle version catalogs for dependency management (`libs.versions.toml`)

**Testing**
- Kotest assertions for test assertions
- JUnit as the test runner

## Code Structure
- `src/main/kotlin/decisionmatrix/` - Main application code
  - `auth/` - Authentication & OAuth handling
  - `db/` - Database repositories and models
  - `ui/` - HTML page generation (kotlinx.html)
  - `routes/` - HTTP route handlers
  - `oauth/` - Mock OAuth server for testing
- `Domain.kt` - Core domain models (Decision, Option, Criteria, UserScore)
- `App.kt` - Main application entry point

## Development Principles

### Code Quality
- We prefer simple, clean, maintainable solutions over clever or complex ones, even if the latter are more concise or performant. Readability and maintainability are primary concerns.
- Make the smallest reasonable changes to get to the desired outcome. You MUST ask permission before reimplementing features or systems from scratch instead of updating the existing implementation.
- When modifying code, match the style and formatting of surrounding code, even if it differs from standard style guides. Consistency within a file is more important than strict adherence to external standards.

### Kotlin Code Style
- Never use `!!`. Use `requireNotNull()` or `require()` instead, or find more elegant ways to handle nulls.
- Prefer explicit types when they improve readability.
- Use meaningful variable and function names that clearly express intent.

### Git Commit Messages
Follow this format:
```
Short summary (50 chars or less)

More detailed explanatory text, if necessary. Wrap at 72 characters.
Explain what and why, not how.

- Use bullet points for multiple changes
- Reference any issues this commit addresses

Closes #123
```

**Rules:**
- Use the imperative mood in the subject line ("Add feature" not "Added feature")
- Use the body to explain what and why vs. how
- Use the footer to reference any issues this commit closes
- NEVER USE `--no-verify`, `--no-hooks`, or `--no-pre-commit-hook` when committing code

## Testing Strategy

### Testing Framework & Tools
- Use Kotest matchers for assertions
- Use JUnit as the test runner
- When adding a JUnit test @Test annotation, put @Test on the same line as the function declaration
- Don't use mockk; use interfaces as dependencies, with `NotImplementedError()` as the default implementation in the interface
- When writing tests, use a similar style as the rest of the codebase

### Testing Requirements
- Tests MUST cover the functionality being implemented
- NEVER ignore the output of the system or the tests; logs and messages often contain CRITICAL information
- **TEST OUTPUT MUST BE PRISTINE TO PASS**
- If the logs are supposed to contain errors, capture and test it explicitly

### Test Coverage Policy
**NO EXCEPTIONS POLICY:** Under no circumstances should you mark any test type as "not applicable". Every project, regardless of size or complexity, MUST have:
- Unit tests
- Integration tests
- End-to-end tests

If you believe a test type doesn't apply, you need the human to say exactly: **"I AUTHORIZE YOU TO SKIP WRITING TESTS THIS TIME"**

## Test-Driven Development (TDD)

We practice strict TDD. That means:

### TDD Process
1. **Red:** Write a failing test that defines a desired function or improvement
2. **Green:** Write minimal code to make the test pass
3. **Refactor:** Improve code design while keeping tests green
4. **Repeat:** Continue the cycle for each new feature or bugfix

### TDD Rules
- Write tests before writing the implementation code
- Only write enough code to make the failing test pass
- Run tests frequently to get immediate feedback
- Refactor continuously while ensuring tests still pass
- Each test should focus on a single behavior or requirement

### TDD Benefits We're Aiming For
- Better design through test-first thinking
- Higher confidence in code changes
- Built-in regression protection
- Living documentation through tests

## Project-Specific Context

### Decision Matrix Domain
When working on decision matrix features, remember:
- Multiple users need to input weights and scores
- Calculations should be transparent and auditable
- Results should be easy to understand and export

## Database Schema
SQLite with these main entities:
- **decisions** - Decision matrices
- **options** - Choices being evaluated
- **criteria** - Evaluation factors with weights
- **user_scores** - Individual user ratings per option/criteria pair

### Authentication
The application uses OAuth for authentication with pluggable provider support.

#### Development Mode
For local development, set `DM_DEV_MODE=true` to bypass OAuth:
```bash
export DM_DEV_MODE=true
export DM_DEV_USER_ID=your-dev-username  # Optional, defaults to "dev-user"
```

You can also specify different users per request using the `?dev_user=<user_id>` query parameter.

#### Mock OAuth Server for Testing
For testing real OAuth flows locally without external dependencies, you can use the embedded mock OAuth server:

```bash
export DM_DEV_MODE=false
export DM_MOCK_OAUTH_SERVER=true
export DM_OAUTH_ISSUER_URL=http://localhost:8081
export DM_OAUTH_CLIENT_ID=test-client
export DM_OAUTH_CLIENT_SECRET=test-secret
export DM_OAUTH_REDIRECT_URI=http://localhost:9000/auth/callback
```

The mock OAuth server provides:
- A login page with predefined test users (Alice, Bob, Admin)
- Standard OAuth 2.0/OpenID Connect endpoints
- JWT tokens with proper signatures for testing

**Test Users Available:**
- Alice Test (alice@example.com) - ID: user1
- Bob Test (bob@example.com) - ID: user2  
- Admin User (admin@example.com) - ID: admin

#### Production OAuth Setup
For production, configure OAuth environment variables for any standards-compliant OAuth 2.0/OpenID Connect provider:

```bash
export DM_DEV_MODE=false
export DM_OAUTH_ISSUER_URL=https://your-oauth-provider.com  # The OAuth issuer URL
export DM_OAUTH_CLIENT_ID=your-client-id
export DM_OAUTH_CLIENT_SECRET=your-client-secret
export DM_OAUTH_REDIRECT_URI=https://your-domain.com/auth/callback
export DM_OAUTH_SCOPES=openid,profile,email  # Optional, defaults to "openid,profile,email"
```

The application will automatically discover OAuth endpoints via the `/.well-known/openid-configuration` endpoint at your issuer URL.

#### Standards Compliance
The application uses standards-based OAuth 2.0 with PKCE and OpenID Connect, supporting any compliant provider that exposes a `/.well-known/openid-configuration` endpoint.

#### Session Management
- Sessions are stored in memory (suitable for single-instance deployments)
- Session timeout is 24 hours by default
- Sessions are automatically cleaned up on expiry

## Development Patterns in This Codebase
- Repository pattern with `*Repository` interfaces and `*RepositoryImpl` implementations
- HTML generation using kotlinx.html DSL, not templates
- HTMX for frontend interactivity (see existing `.hx-*` attributes)
- All routes return http4k `Response` objects
- Authentication via `UserContext` extracted from sessions
