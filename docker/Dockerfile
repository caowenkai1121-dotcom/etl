# ============================================
# ETL同步系统 - 单阶段构建Dockerfile
# 使用本地已编译的JAR文件
# ============================================

FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="ETL Team <etl@example.com>"
LABEL description="ETL数据同步调度系统"
LABEL version="2.0.0"

WORKDIR /app

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 复制本地已编译的jar包
COPY etl-api/target/etl-api-1.0.0.jar app.jar

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost:8080/api/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", "-Xms512m", "-Xmx1024m", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:docker}", \
  "-jar", "app.jar"]
