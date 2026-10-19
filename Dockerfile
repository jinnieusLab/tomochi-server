# 실행 이미지. jar는 밖에서 먼저 만들어요 (./gradlew bootJar).
# 레이어로 풀어 두면 의존성이 그대로일 때 이미지 레이어를 다시 받지 않아요.
FROM eclipse-temurin:21-jre AS extract
WORKDIR /build
COPY build/libs/tomochi-server.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:21-jre
RUN groupadd --system tomochi && useradd --system --gid tomochi tomochi
WORKDIR /app
COPY --from=extract /build/extracted/dependencies/ ./
COPY --from=extract /build/extracted/spring-boot-loader/ ./
COPY --from=extract /build/extracted/snapshot-dependencies/ ./
COPY --from=extract /build/extracted/application/ ./
USER tomochi
EXPOSE 8080
ENV TZ=UTC
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
