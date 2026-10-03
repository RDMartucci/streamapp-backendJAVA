# Construye el cliente para que el servidor pueda entregarlo como una única app.
FROM node:22-alpine AS frontend-build
WORKDIR /frontend
COPY streamapp-Frontend/package.json streamapp-Frontend/package-lock.json ./
RUN npm ci
COPY streamapp-Frontend/ ./
RUN npm run build

FROM maven:3.9.9-eclipse-temurin-21 AS backend-build
WORKDIR /build
COPY streamapp-backend/pom.xml ./
RUN mvn --batch-mode dependency:go-offline
COPY streamapp-backend/src ./src
COPY --from=frontend-build /frontend/dist ./src/main/resources/static
RUN mvn --batch-mode package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S streamapp && adduser -S streamapp -G streamapp \
    && mkdir -p /media \
    && chown -R streamapp:streamapp /app /media
COPY --from=backend-build --chown=streamapp:streamapp /build/target/streamapp-backend-0.0.1-SNAPSHOT.jar ./streamapp.jar
USER streamapp
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/streamapp.jar"]
