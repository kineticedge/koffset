#!/bin/bash

./gradlew :koffset-exporter:build -x test

docker buildx build -f ./koffset-exporter/docker/Dockerfile.dev -t koffset-exporter-dev:latest .

