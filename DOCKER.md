# Docker Configuration

This document describes how to build and run the Decision Matrix application in Docker.

## Building the Docker Image

1. Build the application distribution:
   ```bash
   ./gradlew distTar
   ```

2. Build the Docker image:
   ```bash
   docker build -t decision-matrix .
   ```

## Environment Variables

The application can be configured using the following environment variables:

### HTTP Server Configuration
- **`DM_HTTP_SERVER_PORT`** - HTTP server port (default: `8080`)

### Authentication Configuration
- **`DM_DEV_MODE`** - Enable development mode (bypasses OAuth) (default: `false`)
- **`DM_DEV_USER_ID`** - Default user ID when in dev mode (default: `"dev-user"`)

### OAuth Configuration
Required when `DM_DEV_MODE=false`:
- **`DM_OAUTH_ISSUER_URL`** - OAuth 2.0/OpenID Connect issuer URL (required)
- **`DM_OAUTH_CLIENT_ID`** - OAuth client ID (required)
- **`DM_OAUTH_CLIENT_SECRET`** - OAuth client secret (required)
- **`DM_OAUTH_REDIRECT_URI`** - OAuth redirect URI (required)
- **`DM_OAUTH_SCOPES`** - OAuth scopes as comma-separated list (default: `"openid,profile,email"`)

### Mock OAuth Server (for testing)
- **`DM_MOCK_OAUTH_SERVER`** - Enable embedded mock OAuth server (default: `false`)

### Database Configuration
- **`DB_HOST`** - PostgreSQL database host (default: `"localhost"`)
- **`DB_PORT`** - PostgreSQL database port (default: `"5432"`)
- **`DB_NAME`** - PostgreSQL database name (default: `"decision_matrix"`)
- **`DB_USER`** - PostgreSQL username (default: `"decision_matrix"`)
- **`DB_PASSWORD`** - PostgreSQL password (default: `"decision_matrix_password"`)
- **`DB_CONNECTION_PARAMS`** - Additional PostgreSQL connection parameters (default: `""`)

## Example Usage

### Development Mode (no authentication)
```bash
docker run -p 8080:8080 \
  -e DM_DEV_MODE=true \
  -e DM_DEV_USER_ID=my-test-user \
  -e DB_HOST=host.docker.internal \
  -e DB_PASSWORD=my-secure-password \
  decision-matrix
```

### Production Mode with OAuth
```bash
docker run -p 8080:8080 \
  -e DM_DEV_MODE=false \
  -e DM_OAUTH_ISSUER_URL=https://your-oauth-provider.com \
  -e DM_OAUTH_CLIENT_ID=your-client-id \
  -e DM_OAUTH_CLIENT_SECRET=your-client-secret \
  -e DM_OAUTH_REDIRECT_URI=https://your-domain.com/auth/callback \
  -e DB_HOST=your-postgres-host \
  -e DB_USER=your-db-user \
  -e DB_PASSWORD=your-db-password \
  decision-matrix
```

### Testing with Mock OAuth Server
```bash
docker run -p 8080:8080 \
  -e DM_DEV_MODE=false \
  -e DM_MOCK_OAUTH_SERVER=true \
  -e DM_OAUTH_ISSUER_URL=http://localhost:8081 \
  -e DM_OAUTH_CLIENT_ID=test-client \
  -e DM_OAUTH_CLIENT_SECRET=test-secret \
  -e DM_OAUTH_REDIRECT_URI=http://localhost:8080/auth/callback \
  -e DB_HOST=host.docker.internal \
  decision-matrix
```

## Notes

- The application requires a PostgreSQL database to be available
- When running locally with Docker, use `host.docker.internal` as the database host to connect to services running on the host machine
- The mock OAuth server is useful for testing OAuth flows without external dependencies
- In production, ensure all sensitive environment variables (especially `DB_PASSWORD` and `DM_OAUTH_CLIENT_SECRET`) are properly secured