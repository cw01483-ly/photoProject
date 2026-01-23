FROM eclipse-temurin:21-jre
WORKDIR /app

# 실행할 jar를 명시적으로 지정 (plain.jar 선택 위험 제거)
COPY demo/build/libs/demo-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8008
ENTRYPOINT ["java","-jar","/app/app.jar"]