FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY demo/ /app/demo/
WORKDIR /app/demo

RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar -x test

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/demo/build/libs/*.jar app.jar

EXPOSE 8008
ENTRYPOINT ["java","-jar","/app/app.jar"]
