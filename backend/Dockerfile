ARG MAVEN_BASE_IMAGE=docker.m.daocloud.io/library/maven:3.9.9-eclipse-temurin-17
ARG JRE_BASE_IMAGE=docker.m.daocloud.io/library/eclipse-temurin:17-jre
ARG MAVEN_MIRROR_URL=https://maven.aliyun.com/repository/public
ARG MAVEN_MIRROR_ID=aliyun-public

FROM ${MAVEN_BASE_IMAGE} AS build
ARG MAVEN_MIRROR_URL
ARG MAVEN_MIRROR_ID

WORKDIR /workspace

RUN mkdir -p /root/.m2
RUN if [ -n "${MAVEN_MIRROR_URL}" ]; then \
      printf '%s\n' \
        '<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"' \
        '          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"' \
        '          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0 https://maven.apache.org/xsd/settings-1.2.0.xsd">' \
        '  <mirrors>' \
        '    <mirror>' \
        "      <id>${MAVEN_MIRROR_ID}</id>" \
        '      <name>Build Mirror</name>' \
        "      <url>${MAVEN_MIRROR_URL}</url>" \
        '      <mirrorOf>*</mirrorOf>' \
        '    </mirror>' \
        '  </mirrors>' \
        '</settings>' \
        > /root/.m2/settings.xml; \
    fi

COPY pom.xml .

RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -ntp -U \
      -Dmaven.wagon.http.retryHandler.count=5 \
      -Dmaven.wagon.http.retryHandler.requestSentEnabled=true \
      -Dmaven.wagon.http.pool=false \
      -Dhttp.keepAlive=false \
      dependency:go-offline

COPY src ./src

RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -ntp \
      -Dmaven.wagon.http.retryHandler.count=5 \
      -Dmaven.wagon.http.retryHandler.requestSentEnabled=true \
      -Dmaven.wagon.http.pool=false \
      -Dhttp.keepAlive=false \
      -DskipTests \
      -Dmaven.test.skip=true \
      clean package

FROM ${JRE_BASE_IMAGE}

WORKDIR /app

COPY --from=build /workspace/target/backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8000

CMD ["java", "-jar", "app.jar"]
