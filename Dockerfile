## DEBT: Optimize image size
FROM openjdk:17-alpine

COPY build/libs/akka-mock-oneJar.jar /app.jar
COPY examples/ ./examples/
ENV PORT=8080
ENV FOLDER="./examples/"
EXPOSE $PORT

ENTRYPOINT ["java", "-jar", "/app.jar", "--server.port=${PORT}", "--folder=${FOLDER}"]