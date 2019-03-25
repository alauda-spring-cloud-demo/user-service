#!/bin/bash

set -ex;

exec /usr/bin/java ${JAVA_OPTS} \
     	-Djava.security.egd=file:/dev/./urandom \
     	-javaagent:/pinpoint-agent/pinpoint-bootstrap-1.8.0.jar \
     	-Dpinpoint.agentId=${APP_NAME} \
     	-Dpinpoint.applicationName=${APP_NAME} \
     	-jar /app/app.jar