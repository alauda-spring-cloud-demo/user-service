FROM dwgao/java:8-alpine-pinpoint1.8.4

COPY target/*.jar /app/app.jar

COPY docker-entrypoint.sh /app/docker-entrypoint.sh

RUN chmod +x /app/docker-entrypoint.sh

ENTRYPOINT ["/app/docker-entrypoint.sh"]