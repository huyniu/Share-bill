# ---------- 1. Build app web (Expo) ----------
FROM node:22-bookworm-slim AS web
WORKDIR /mobile
COPY mobile/package.json mobile/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY mobile/ ./
RUN npx expo export -p web --output-dir /web

# ---------- 2. Build backend (Spring Boot) ----------
FROM eclipse-temurin:17-jdk AS backend
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src/ src/
COPY --from=web /web/ src/main/resources/static/
# Test đã chạy ở máy tính (./mvnw test); bỏ qua ở đây cho build nhanh
RUN ./mvnw -q -B -DskipTests package && cp target/share-bill-*.jar app.jar

# ---------- 3. Chạy ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 sharebill
COPY --from=backend /app/app.jar app.jar
USER sharebill
# Gói miễn phí của Render có 512 MB RAM
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1"
EXPOSE 8686
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
