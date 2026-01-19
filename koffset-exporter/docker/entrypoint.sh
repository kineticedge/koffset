#!/bin/sh

set -e

(cd /app; tar xfv /app.tar)

PROJECT="$(ls -A /app)"
APPLICATION=$(echo "$PROJECT" | sed -E -e 's/(.*)-(.*)/\1/')
VERSION=$(echo "$PROJECT" | sed -E -e 's/(.*)-(.*)/\2/')

COMMAND="/app/${PROJECT}/bin/${APPLICATION}"

#
# use exec so signals are properly handled
#
exec "${COMMAND}" "$@"
