# ---- build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- run stage ----
FROM eclipse-temurin:21-jre
ENV TZ=Asia/Kolkata \
    JAVA_TOOL_OPTIONS="-Duser.timezone=Asia/Kolkata -XX:MaxRAMPercentage=70"
RUN useradd --system --uid 10001 --no-create-home app
WORKDIR /app
COPY --from=build /src/target/*.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080' || exit 1
ENTRYPOINT ["java","-jar","/app/app.jar"]
