# spark
My Spark projects

## Playing in the Spark shell
Mount UFO sightings data
```
$ docker run --rm -v sightings.log:/media/sightings.log:ro -it asarkar/spark:2.1.0 spark-shell
scala> val df = spark.read.json("/media/sightings.log")
scala> df.show
scala> df.groupBy("state").agg(count(df("city"))).show
```
