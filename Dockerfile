# Builder image
FROM maven:3.5-jdk-8 as builder
WORKDIR /home/root/build/
COPY pom.xml /home/root/build/
RUN mvn package -DskipTests --fail-never
COPY . /home/root/build/
RUN mvn -o package -DskipTests

# Executor image
FROM openjdk:8-jre-alpine
ARG SERVICE_PORT
COPY --from=builder /home/root/build/target/*.jar \
/srv/wikitolearn-pwa-gateway/app.jar
WORKDIR /srv/wikitolearn-pwa-gateway/
EXPOSE $SERVICE_PORT
ENTRYPOINT ["java", "-jar", "app.jar"]
