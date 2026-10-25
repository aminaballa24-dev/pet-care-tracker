FROM eclipse-temurin:21.0.12_8-jdk AS build
WORKDIR /app

COPY . .

RUN chmod +x mvnw && ./mvnw clean package -DskipTests


FROM eclipse-temurin:21.0.12_8-jre
WORKDIR /app

RUN useradd --system --create-home appuser

COPY --from=build /app/target/*.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]