# Project: Multi-user weighted decision matrix tool

This project supports the creation of weighted decision matrices when multiple people are involved.

## General Guidance
- ALWAYS ask for clarification rather than making assumptions.
- If you're having trouble with something, it's ok to stop and ask for help.
- Focus on getting working, tested code before optimizing or adding features.

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

### Authentication
The application uses OAuth for authentication with pluggable provider support.

#### Development Mode
For local development, set `DM_DEV_MODE=true` to bypass OAuth:
```bash
export DM_DEV_MODE=true
export DM_DEV_USER_ID=your-dev-username  # Optional, defaults to "dev-user"
```

You can also specify different users per request using the `?dev_user=<user_id>` query parameter.

#### Production OAuth Setup
For production, configure OAuth environment variables:

```bash
export DM_DEV_MODE=false
export DM_OAUTH_PROVIDER=google  # Currently only "google" is supported
export DM_OAUTH_CLIENT_ID=your-google-client-id
export DM_OAUTH_CLIENT_SECRET=your-google-client-secret
export DM_OAUTH_REDIRECT_URI=https://your-domain.com/auth/callback
```

#### Google OAuth Setup
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the Google+ API
4. Go to "Credentials" → "Create Credentials" → "OAuth 2.0 Client IDs"
5. Set application type to "Web application"
6. Add your redirect URI (e.g., `http://localhost:9000/auth/callback` for local dev)
7. Copy the Client ID and Client Secret to your environment variables

#### Adding New OAuth Providers
To add a new OAuth provider:

1. Implement the `OAuthProvider` interface in `src/main/kotlin/decisionmatrix/auth/providers/`
2. Add provider selection logic in `App.kt`
3. Update the `DM_OAUTH_PROVIDER` environment variable documentation

#### Session Management
- Sessions are stored in memory (suitable for single-instance deployments)
- Session timeout is 24 hours by default
- Sessions are automatically cleaned up on expiry
