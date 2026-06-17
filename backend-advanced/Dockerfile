FROM maven:3.9.9-eclipse-temurin-25 AS build
WORKDIR /workspace

COPY pom.xml /workspace/
COPY backend-advanced/pom.xml /workspace/backend-advanced/pom.xml
COPY backend-advanced/src /workspace/backend-advanced/src

RUN mvn -pl backend-advanced -am clean package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/backend-advanced/target/docusign-backend-advanced-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
