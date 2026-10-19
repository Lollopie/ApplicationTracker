FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /builder
ARG JAR_FILE=target/*.jar

COPY ${JAR_FILE} application.jar

RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:21-jre-alpine

WORKDIR /application

COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./

RUN addgroup -g 1432 springgroup && adduser -H -s /sbin/nologin -u 1432 -G springgroup springuser -D

USER springuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-XX:MaxRAMPercentage=75", "application.jar"]