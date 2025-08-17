FROM maven:3.9.11-eclipse-temurin-21-alpine AS builder

WORKDIR /build

COPY pom.xml .
COPY td.jar ./

RUN mvn install:install-file \
    -Dfile=td.jar \
    -DgroupId=org.drinkless \
    -DartifactId=tdlib \
    -Dversion=1.0 \
    -Dpackaging=jar

COPY src/ ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update && \
    apt-get install -y libc++1 && \
    rm -rf /var/lib/apt/lists/*

COPY db/ ./db/
COPY libtdjni.so /usr/lib/
COPY --from=builder /build/target/cryptobot-catch-*-jar-with-dependencies.jar ./cryptobot-catch.jar


ENTRYPOINT ["java", \
 "-server", "-Xms384m",  \
 "-jar", "cryptobot-catch.jar"]