# chỉ ra image nào mà ta đang kế thừa (sẵn có)
FROM openjdk:17-jdk-alpine

# config thư mục làm việc + trong Linux kernel của container này có 1 directory
# là /app, thay vì ta nhảy vào cd cái directory này thì ta khai báo Working directory
# tức là khi container bật lên 1 cái thì nó sẽ tự đọng nhảy vào thư mục này
WORKDIR /app

# copy từ host sang container và cái "/" ở dưới là cái WORKDIR
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# trong image này ta run lệnh dưới này khi image được tạo ra
# câu lệnh dưới dùng để download dependency giúp nó nằm luôn trong image này
RUN ./mvnw dependency:go-offline

COPY src ./src
# sau khi cài hết các library thì nó chạy
CMD ["./mvnw", "spring-boot:run"]

## chỉ ra image nào mà ta đang kế thừa (sẵn có)
#FROM openjdk:17-jdk-alpine
#
## config thư mục làm việc + trong Linux kernel của container này có 1 directory
## là /app, thay vì ta nhảy vào cd cái directory này thì ta khai báo Working directory
## tức là khi container bật lên 1 cái thì nó sẽ tự đọng nhảy vào thư mục này
#WORKDIR /app
#
## copy từ host sang container và cái "/" ở dưới là cái WORKDIR
#COPY .mvn/ .mvn
#COPY mvnw pom.xml ./
#
## trong image này ta run lệnh dưới này khi image được tạo ra
## câu lệnh dưới dùng để download dependency giúp nó nằm luôn trong image này
#RUN ./mvnw dependency:go-offline
#
#COPY src ./src
## sau khi cài hết các library thì nó chạy
#CMD ["./mvnw", "spring-boot:run"]
