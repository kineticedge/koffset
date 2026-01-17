FROM eclipse-temurin:25-jdk-ubi10-minimal AS build

RUN microdnf install -y curl tar gzip unzip && microdnf clean all

ENV GRADLE_VERSION=9.2.1
RUN curl -sL https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -o gradle.zip \
    && unzip gradle.zip -d /opt \
    && ln -s /opt/gradle-${GRADLE_VERSION}/bin/gradle /usr/bin/gradle \
    && rm gradle.zip

COPY --chown=1000:1000 . /home/gradle/src

WORKDIR /home/gradle/src

RUN gradle :koffset-exporter:build -x test --no-daemon


FROM eclipse-temurin:25-jre-ubi10-minimal

RUN microdnf install -y curl && microdnf clean all \
    && groupadd -r appgroup && useradd -r -g appgroup appuser

USER appuser

WORKDIR /app

# for production Docker build process, artifacts are pulled in from the previous build phase
COPY --from=build --chown=appuser:appgroup /home/gradle/src/koffset-exporter/docker/entrypoint.sh /entrypoint.sh
COPY --from=build --chown=appuser:appgroup /home/gradle/src/koffset-exporter/build/distributions/koffset-exporter-*.tar /app.tar

EXPOSE 8899

ENV JAVA_OPTS="-XX:+UseG1GC --enable-native-access=ALL-UNNAMED"

ENTRYPOINT ["/entrypoint.sh"]
CMD []