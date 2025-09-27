FROM eclipse-temurin:21-jre

# Prerequisite: call `./gradlew distTar` before running docker to generate the app.tar file
ADD build/distributions/app.tar /opt/
WORKDIR /opt/app
CMD ["./bin/app"]
