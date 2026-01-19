#!/bin/bash

docker buildx build -f ./koffset-exporter/docker/Dockerfile.native -t koffset-exporter-native:latest .


