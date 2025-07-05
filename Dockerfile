FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# 1. First copy only the files needed for dependency resolution
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

# 2. Make sure mvnw is executable and has Unix line endings
RUN apk add --no-cache dos2unix && \
    dos2unix mvnw && \
    chmod +x mvnw

# 3. Download dependencies
RUN ./mvnw dependency:go-offline -B

# 4. Copy the rest of the source code
COPY src src

# 5. Build the application
RUN ./mvnw clean package -DskipTests

# 6. Run the application
ENTRYPOINT ["java", "-jar", "target/your-app-name.jar"]