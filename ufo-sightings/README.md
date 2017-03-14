# Summary
The producer gets UFO sightings data (yup!) from [The National UFO Reporting Center Online Database](http://www.nuforc.org/webreports.html) 
(there's such a thing), does some basic clean up, and inserts into Kafka. Each year is processed on a separate thread,
with a delay introduced between consecutive years to simulate streaming. Months in a year are processed in parallel.

The consumer reads from Kafka and calculates analytics like number of sightings per state, per month, per year, 
per shape and holds the data in memory.

After the producer and consumer are stopped, the analytics is printed to console  and the app is terminated.

> See `application.conf` for configuration options.

To run locally, `docker-compose up`.  Press `CTRL + C` to exit and `docker-compose rm -f` to clean up, 
See my [Kafka](https://github.com/asarkar/docker/tree/master/kafka) repository for useful Kafka commands.

To build the Docker images locally instead of using the ones from my Docker Cloud repo, 
`bin/activator clean docker:publishLocal`