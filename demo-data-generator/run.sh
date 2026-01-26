#!/bin/sh
set -e

cd "$(dirname "$0")"

../gradlew assemble > /dev/null

. ./.classpath.sh

MAIN="io.kineticedge.koffset.ddg.Main"

JAVA_OPTS=""

java $JAVA_OPTS -cp "${CP}" $MAIN "$@"
