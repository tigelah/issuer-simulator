FROM eclipse-temurin:21-jre
WORKDIR /app
ARG JAR_FILE=target/issuer-simulator-*.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8090
ENTRYPOINT ["java","-jar","/app/app.jar"]
