FROM maven:3.9.13-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -ntp -DskipTests verify

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S teamflow && adduser -S -G teamflow teamflow
WORKDIR /app
COPY --from=build /build/target/teamflow-server-*.jar /app/app.jar
USER teamflow
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
