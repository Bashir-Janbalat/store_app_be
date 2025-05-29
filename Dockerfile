FROM openjdk:17-jdk-slim
WORKDIR /app

ARG APP_VERSION
ENV APP_VERSION=${APP_VERSION}

COPY target/store.jar store.jar
ENTRYPOINT ["java", "-jar", "store.jar"]