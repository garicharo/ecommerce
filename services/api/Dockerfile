FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache wget \
    && adduser -D -u 1000 shop
COPY --from=build /src/target/*.jar app.jar
USER shop
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
