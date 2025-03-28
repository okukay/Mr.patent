FROM openjdk:17-jdk-slim

WORKDIR /app

# 빌드된 JAR 파일만 복사
COPY ./build/libs/*.jar app.jar

# 환경 변수 설정
ENV SPRING_PROFILES_ACTIVE=prod

# 포트 노출
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]