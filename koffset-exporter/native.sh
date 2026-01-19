#!/bin/sh
set -e

cd "$(dirname "$0")"
gradle assemble > /dev/null

. ./.classpath.sh

MAIN="io.kineticedge.koffset.Main"

JAVA_HOME=$GRAALVM_HOME
JAVA_OPTS="-agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image_1"

KAFKA_BOOSTRAP_SERVERS=localhost:9090
KAFKA_SECURITY_PROTOCOL=sasl_plaintext
KAFKA_SASL_MECHANISM=oauthbearer


KAFKA_SECURITY_PROTOCOL=SASL_PLAINTEXT
KAFKA_SASL_MECHANISM=OAUTHBEARER
KAFAL_SASL_OAUTHBEARER_TOKEN_ENDPOINT_URL=http://localhost:9999/does-not-exist


java $JAVA_OPTS -cp "${CP}" $MAIN "$@"
