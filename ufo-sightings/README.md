# Summary
The producer gets UFO sightings data (yup!) from [The National UFO Reporting Center Online Database](http://www.nuforc.org/webreports.html) 
(there's such a thing), does some basic clean up, and inserts into Kafka. Each year is processed on a separate thread,
with a delay introduced between consecutive years to simulate streaming. Months in a year are processed in parallel.

There are 2 consumers, one Akka, another Spark, that read from Kafka and calculate analytics like number of sightings 
per state, month, year and shape. In the end, the analytics is printed out and the app is terminated.

> See `application.conf` in `src/main/resources` for configuration options.

To run locally, `docker-compose up`.  Press `CTRL + C` to exit and `docker-compose rm -f` to clean up, 
See my [Kafka](https://github.com/asarkar/docker/tree/master/kafka) repository for useful Kafka commands.

To build the Docker images locally instead of using the ones from my Docker Cloud repo, 
`bin/activator clean docker:publishLocal`

# References
[Akka Streams Kafka](http://doc.akka.io/docs/akka-stream-kafka/current/home.html)

[activator-reactive-kafka-scala](https://github.com/softwaremill/activator-reactive-kafka-scala)

[reactive-kafka-scala-example](https://github.com/makersu/reactive-kafka-scala-example#master)

[Spark Streaming + Kafka Integration Guide](https://spark.apache.org/docs/latest/streaming-kafka-integration.html)
