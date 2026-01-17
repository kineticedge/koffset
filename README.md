# koffset
Kafka Consumer Offset Monitoring


## Features

- **Java** - written in Java leveraging the Apache kafka-clients Admin API
- **minimal dependencies** - using kafka-clients for admin client, for HTTP server, and slf4j/logback for logging. Easy to update and minimizes CVE concerns
- **Java 25** - leverage latest Java features
- **non-blocking** - lag collection is done asyncronously than metric scraping
- **collection-adjusting** - adjust the collector thread to run "just before" the primary scraping request
- **admin client** - 100% admin client only with minimal API calls and no Kafka Consumer APIs used
- **minimal memory footprint** - small footprint 
  - native (graalvm compiled) image also provided - currently experimental (missing all security.protocol configurations).
  - at least 1/2 the memory footprint of other similar JVM based implementation
- **fast start times** - starts in a few seconds
  - both JVM based and native containers start in a few seconds
  - Admin API calls used to get historial context to provide more accurate timestamp lag on restart.
- **configurable** - all settings are configurable via environment variables
