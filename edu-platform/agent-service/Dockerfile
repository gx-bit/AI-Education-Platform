FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/agent-service-1.0.0.jar app.jar
EXPOSE 8086
ENV JAVA_OPTS="-Xms192m -Xmx384m -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
