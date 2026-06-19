FROM eclipse-temurin:21-jdk-alpine
LABEL authors="znjac"

# 设置工作目录
WORKDIR /app

# 请保证target只有一个jar包
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /app/

# 获取 JAR 文件名并存储到 version.txt
RUN ls /app/*.jar | tee /app/version.txt
RUN cat /app/version.txt

# 重命名 JAR 文件为 app.jar
RUN mv /app/*.jar /app/app.jar

ENTRYPOINT ["sh", "-c", "java -jar /app/app.jar"]
