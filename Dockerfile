# Builder image
FROM maven:3.5-jdk-8-alpine as builder
WORKDIR /srv/app/
COPY pom.xml .
RUN mvn -B -C -T 1C org.apache.maven.plugins:maven-dependency-plugin:3.0.2:go-offline
COPY . .
RUN mvn -B -o -T 1C package -DskipTests

# Executor image
FROM openjdk:8-jre-alpine
RUN apk --update --no-cache add curl
ARG SERVICE_PORT
COPY --from=builder /srv/app/target/*.jar \
/srv/app/app.jar
COPY ./src/main/resources/app-keystore.jks /srv/app/app-keystore.jks
COPY ./src/main/resources/app-truststore.jks /srv/app/app-truststore.jks
WORKDIR /srv/app/
EXPOSE $SERVICE_PORT
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
HEALTHCHECK --interval=1m --timeout=3s CMD curl -f http://localhost:${SERVICE_PORT}/_meta/status || exit 1