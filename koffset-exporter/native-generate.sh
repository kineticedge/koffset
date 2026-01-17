#!/bin/bash


. ./.classpath.sh


METADATA=src/main/resources/META-INF/native-image_GEN

$GRAALVM_HOME/bin/native-image \
  -cp ${CP} \
  io.kineticedge.koffset.Main -o ./koffset \
  --no-fallback \
  --enable-http \
  --enable-https \
  --allow-incomplete-classpath \
  --report-unsupported-elements-at-runtime \
  --install-exit-handlers \
  --enable-monitoring=jmxserver,jmxclient,heapdump,jvmstat \
  -H:+ReportExceptionStackTraces \
  -H:+EnableAllSecurityServices \
  -H:EnableURLProtocols=http,https \
  -H:AdditionalSecurityProviders=sun.security.jgss.SunProvider \
  -H:ReflectionConfigurationFiles=${METADATA}/reflect-config.json \
  -H:JNIConfigurationFiles=$METADATA/jni-config.json \
  -H:ResourceConfigurationFiles=$METADATA/resource-config.json \
  -H:SerializationConfigurationFiles=$METADATA/serialization-config.json \
  -H:PredefinedClassesConfigurationFiles=$METADATA/predefined-classes-config.json \
  -H:DynamicProxyConfigurationFiles=$METADATA/proxy-config.json \
  -march=compatibility \
  --initialize-at-build-time=org.slf4j.LoggerFactory,org.slf4j.helpers,ch.qos.logback,ch.qos.logback.classic.Logger,org.xml.sax.helpers \
  --verbose

