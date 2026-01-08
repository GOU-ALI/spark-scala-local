FROM apache/spark:3.5.0

USER root
WORKDIR /app

# Copy JAR
COPY target/spark-scala-local-1.0-SNAPSHOT.jar /app/app.jar

# Copy Configs and Data (for testing)
# In real prod, this might be mounted volume or S3
COPY src/main/resources /app/resources

# Default command: show help
CMD ["/opt/spark/bin/spark-submit", "--class", "com.example.platform.app.PlatformApp", "/app/app.jar"]
