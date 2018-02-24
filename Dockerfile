# Builder image
FROM maven:3.5-jdk-8 as builder
ENV MAVEN_OPTS=-Dmaven.repo.local=/srv/.m2/
WORKDIR /srv/app/
COPY pom.xml .
RUN mvn -B -C -T 1C org.apache.maven.plugins:maven-dependency-plugin:3.0.2:go-offline
COPY . .
RUN mvn -B -o -T 1C package -DskipTests

# Executor image
FROM openjdk:8-jre-alpine
ARG SERVICE_PORT
COPY --from=builder /srv/app/target/*.jar \
/srv/app/app.jar
WORKDIR /srv/app/
EXPOSE $SERVICE_PORT
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
