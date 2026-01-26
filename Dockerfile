# 多阶段构建 - 构建阶段
FROM maven:3.9.6-eclipse-temurin-17 AS builder

# 设置工作目录
WORKDIR /app

# 设置时区
ENV TZ=Asia/Shanghai

# 复制 Maven 配置文件
COPY pom.xml .
# 创建正确的模块目录结构
RUN mkdir -p listen-common listen-wx listen-admin
COPY listen-common/pom.xml listen-common/
COPY listen-wx/pom.xml listen-wx/
COPY listen-admin/pom.xml listen-admin/

# 下载依赖（先下载依赖以利用 Docker 缓存）
RUN mvn dependency:go-offline -B

# 复制源代码
COPY listen-common/src listen-common/src
COPY listen-wx/src listen-wx/src
COPY listen-admin/src listen-admin/src

# 构建应用 - 使用正确的模块名称
RUN mvn clean package -pl listen-admin -am -DskipTests -B

# 运行阶段 - 使用更轻量的基础镜像
FROM eclipse-temurin:17-jre-alpine

# 设置工作目录
WORKDIR /app

# 设置时区
ENV TZ=Asia/Shanghai

# Alpine使用apk而不是apt-get
RUN apk add --no-cache curl

# 设置环境变量
ENV SPRING_PROFILES_ACTIVE=prod

# 复制构建好的jar文件
COPY --from=builder /app/listen-admin/target/admin-*.jar app.jar

# 暴露端口
EXPOSE 8081

# 健康检查
#HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
#    CMD curl -f http://localhost:8081/actuator/health || exit 1

# 启动应用 - 使用jar文件启动
CMD ["java", "-jar", "app.jar"]
