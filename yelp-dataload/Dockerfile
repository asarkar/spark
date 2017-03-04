FROM asarkar/spark:2.1.0

COPY spark-env.sh "$SPARK_HOME"/conf/
COPY build/libs/*.jar /app.jar

RUN chmod +x "$SPARK_HOME/conf/spark-env.sh" && \
  sh -c 'touch /app.jar'

ENTRYPOINT ["/opt/spark/bin/spark-submit", "/app.jar"]