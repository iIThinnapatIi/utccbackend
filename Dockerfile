# ใช้ Java 17 JDK
FROM eclipse-temurin:17-jdk

# โฟลเดอร์ทำงานใน container
WORKDIR /app

# คัดลอก Maven wrapper และ pom.xml
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# ทำให้ mvnw รันได้ และดาวน์โหลด dependency ล่วงหน้า
RUN chmod +x mvnw && ./mvnw dependency:go-offline

# คัดลอกโค้ด src ทั้งหมด
COPY src ./src

# คำสั่งตอน container เริ่มรัน
CMD ["./mvnw", "spring-boot:run"]
