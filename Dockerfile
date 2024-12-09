# docker build -t uaa .
# docker run -p 8082:8082 uaa

# Sử dụng image Maven để build project
FROM maven:3.9.4-eclipse-temurin-21 AS builder

# Đặt thư mục làm việc trong container
WORKDIR /app

# Sao chép file cấu hình Maven và các file nguồn vào container
COPY pom.xml ./
COPY src ./src

# Build project bằng Maven
RUN mvn clean package -DskipTests

# Sử dụng image Java 21 nhẹ để chạy ứng dụng
FROM eclipse-temurin:21-jre

# Đặt thư mục làm việc
WORKDIR /app

# Sao chép file JAR từ builder vào container này
COPY --from=builder /app/target/*.jar app.jar

# Chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]