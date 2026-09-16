FROM eclipse-temurin:25-jdk-ubi10-minimal AS build

RUN microdnf install -y curl tar gzip unzip && microdnf clean all

ENV GRADLE_USER_HOME=/home/gradle/.gradle

WORKDIR /home/gradle/src

COPY --chown=1000:1000 gradlew *.gradle.kts gradle.properties ./
COPY --chown=1000:1000 gradle/ gradle/
COPY --chown=1000:1000 koffset-exporter/*.gradle.kts koffset-exporter/

RUN mkdir -p $GRADLE_USER_HOME && chown -R 1000:1000 /home/gradle

USER 1000
RUN ./gradlew :koffset-exporter:dependencies --no-daemon

USER ROOT
COPY --chown=1000:1000 . .

USER 1000
RUN ./gradlew :koffset-exporter:assemble --no-daemon -x test

FROM eclipse-temurin:25-jre-ubi10-minimal

RUN microdnf install -y curl && microdnf clean all \
    && groupadd -r appgroup && useradd -r -g appgroup appuser

USER appuser

WORKDIR /app

COPY --from=build --chown=appuser:appgroup /home/gradle/src/koffset-exporter/build/distributions/koffset-exporter-*.tar /app.tar
COPY --from=build --chown=appuser:appgroup /home/gradle/src/koffset-exporter/docker/entrypoint.sh /entrypoint.sh

EXPOSE 8080/tcp

ENV JAVA_OPTS="-XX:+UseG1GC --enable-native-access=ALL-UNNAMED"

ENTRYPOINT ["/entrypoint.sh"]
CMD []