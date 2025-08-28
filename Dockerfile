FROM maven:3.9.11-eclipse-temurin-24-alpine AS builder

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


FROM ubuntu:24.04

WORKDIR /app

RUN apt-get update && \
    apt-get install -y \
    wget \
    gzip \
    tar \
    libc6 \
    libstdc++6 \
    zlib1g \
    libc++1 \
    libc++-dev \
    libc++abi1 \
    && apt-get clean


RUN wget https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-24.0.2/graalvm-community-jdk-24.0.2_linux-x64_bin.tar.gz && \
    tar -xzf graalvm-community-jdk-24.0.2_linux-x64_bin.tar.gz -C /opt && \
    rm graalvm-community-jdk-24.0.2_linux-x64_bin.tar.gz && \
    mv /opt/graalvm-community-openjdk-24.0.2+11.1 /opt/graalvm && \
    update-alternatives --install /usr/bin/java java /opt/graalvm/bin/java 1000 && \
    update-alternatives --install /usr/bin/javac javac /opt/graalvm/bin/javac 1000

ENV JAVA_HOME="/opt/graalvm"
ENV PATH="/opt/graalvm/bin"

COPY db/ ./db/
COPY libtdjni.so /usr/lib/
COPY --from=builder /build/target/cryptobot-catch-*-jar-with-dependencies.jar ./cryptobot-catch.jar

ENTRYPOINT ["java", \
 "--enable-native-access=ALL-UNNAMED", \
 "-server", \
 "-jar", "cryptobot-catch.jar"]