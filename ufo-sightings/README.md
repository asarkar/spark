# Summary
The producer gets UFO sightings data (yup!) from [The National UFO Reporting Center Online Database](http://www.nuforc.org/webreports.html) 
(there's such a thing), does some basic clean up, and inserts into Kafka.

The consumer reads from Kafka and prints analytics like number of sightings per state, per month, per year, 
per shape.

### How to run locally

```
docker-compose -d up
```

To clean up
```
docker-compose -af
```