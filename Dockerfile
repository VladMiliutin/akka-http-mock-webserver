## DEBT: Optimize image size
FROM openjdk:17-alpine

COPY build/libs/akka-mock-oneJar.jar /app.jar
COPY examples/ ./examples/
ENV HTTP_PORT=8080
ENV MOCK_FOLDER="./examples/"

ENTRYPOINT ["java", "-jar", "/app.jar"]