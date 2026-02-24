ARG MAVEN_BASE_IMAGE=docker.m.daocloud.io/library/maven:3.9.9-eclipse-temurin-17
ARG JRE_BASE_IMAGE=docker.m.daocloud.io/library/eclipse-temurin:17-jre

FROM ${MAVEN_BASE_IMAGE} AS build

WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn -B clean package -DskipTests

FROM ${JRE_BASE_IMAGE}

WORKDIR /app

COPY --from=build /workspace/target/backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8000

CMD ["java", "-jar", "app.jar"]
