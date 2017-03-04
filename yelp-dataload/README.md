# Summary
Reads a JSON file and inserts into Couchbase using [Couchbase Spark Connector](https://github.com/couchbase/couchbase-spark-connector).
The file can come from Amazon S3, local, or HDFS. The example below is for S3, so the credentials are set as environment
variables when creating a container. A Couchbase instance is required to be running. The bucket is created if not present.

An sample file can be found in `src/test/resources`.

## To run

1. Build.
```
$ ./gradlew bootRepackage &amp;&amp; \
docker build -t asarkar/yelp-dataload .
```
2. Start Couchbase
```
$ docker run -d --name couchbase -p 8091-8094:8091-8094 -p 11210:11210 couchbase/server
```

3. Go to localhost:8091 and set up Couchbase.

4. Run the application.
```
$ docker run --rm \
-e CB_NODES=<cb_ip> \
-e YELP_DATADIR_URL=<s3_url> \
-e AWS_ACCESS_KEY=<aws_access_key> \
-e AWS_SECRET=<aws_secret> \
-it asarkar/yelp-dataload
```

# References

[Lessons Learned From Running Spark On Docker](https://www.youtube.com/watch?v=Qei5VAnCpDM)
