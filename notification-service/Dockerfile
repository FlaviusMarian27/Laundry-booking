# ---- Build stage: compile the jar with Maven ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy only the Maven files first, so dependencies are cached between builds
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

# Then copy the source code and build
COPY src src
RUN ./mvnw -q -B package -DskipTests

# ---- Run stage: only the JRE and the jar ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as a non-root user
RUN useradd --system --uid 1001 appuser
USER appuser

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
