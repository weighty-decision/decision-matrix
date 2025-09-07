# decision-matrix
Weighted decision matrix that supports multiple users. 
When calculating each option's weighted score, it uses the average of each user's score.
It stores the data in a PostgreSQL database. 

# This tool in early development mode!
I'm just building the 1.0 version now, so it's not ready for production use yet.

## Setup

### Prerequisites
- Java 21 or higher
- PostgreSQL database

### Database Configuration
The application uses PostgreSQL for data storage. Configure your database connection using environment variables:

#### Basic Configuration
```bash
export DB_HOST=localhost          # Default: localhost
export DB_PORT=5432              # Default: 5432
export DB_NAME=decision_matrix   # Default: decision_matrix
export DB_USER=your_username     # Default: decision_matrix
export DB_PASSWORD=your_password # Default: decision_matrix_password
```

#### Advanced PostgreSQL Configuration
For additional PostgreSQL connection parameters (SSL, timeouts, etc.), use:

```bash
export DB_CONNECTION_PARAMS="sslmode=require"
# or multiple parameters:
export DB_CONNECTION_PARAMS="sslmode=require&connectTimeout=10&socketTimeout=30"
```

Common connection parameters:
- `sslmode=require` - Force SSL connection
- `sslmode=disable` - Disable SSL
- `connectTimeout=10` - Connection timeout in seconds
- `socketTimeout=30` - Socket timeout in seconds
- `prepareThreshold=0` - Disable prepared statement caching

### Running the Application
```bash
./gradlew run
```

### Authentication Setup
See [CLAUDE.md](CLAUDE.md) for authentication configuration options including development mode and OAuth setup.

## Screenshots
### Home
![Index](docs/resources/index.png)
### /decisions/{id}/edit/
![Edit](docs/resources/edit.png)
### /decisions/{id}/my-scores
![Score](docs/resources/score.png)
### /decisions/{id}/results
![Results](docs/resources/results.png)

## Development
See [DEVELOPMENT.md](DEVELOPMENT.md) for development prerequisites.  
See [CLAUDE.md](CLAUDE.md) for development guidelines, including how to configure authentication for development.
