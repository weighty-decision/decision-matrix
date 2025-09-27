## Prerequisites
- Java 21 or newer
    - If you use homebrew and don't already have a JDK installed: `brew install --cask temurin21`
- Docker. e.g. `brew install colima` or `brew install docker`

## Running it locally
The app uses a Postgres database that is supplied by Docker.
```shell
docker-compose up -d
./gradlew build
```

This will start the app, listening on port 8080.
On startup, if the database does not exist, it will be created. 

The UI is located at http://localhost:8080/
