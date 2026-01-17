#!/bin/bash

docker buildx build -f ./koffset-exporter/docker/Dockerfile.native -t kafka-exporter-native:latest .


