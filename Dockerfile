FROM spring-cloud-demo/java

COPY target/*.jar /app/app.jar

CMD /bin/bash -c 'java $JAVA_OPTS -jar /app/app.jar'