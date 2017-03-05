package org.abhijitsarkar;

import lombok.RequiredArgsConstructor;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * @author Abhijit Sarkar
 */
@RequiredArgsConstructor
@Service
public class YelpS3ToCouchbaseDataloadService {
    private final SparkProperties sparkProperties;
    private final CouchbaseProperties couchbaseProperties;

    @Value("${spring.application.name:UNKNOWN}")
    private String appName;

    private SparkSession spark;
//    private JavaSparkContext sc;

    @PostConstruct
    void postConstruct() {
        spark = SparkSession
                .builder()
                .master(sparkProperties.getMaster())
                .appName(appName)
                .config("spark.sql.streaming.schemaInference", "true")
                .config("spark.sql.streaming.checkpointLocation", sparkProperties.getCheckpointDir())
                .config("spark.couchbase.nodes", couchbaseProperties.getNodes())
                .config(String.format("spark.couchbase.bucket.%s", couchbaseProperties.getBucketName()),
                        couchbaseProperties.getBucketPassword())
                .getOrCreate();

//        sc = new JavaSparkContext(spark.sparkContext());
    }

    @PreDestroy
    void preDestroy() {
        spark.close();
    }

    public void load(String dataDirURL) throws StreamingQueryException {
        load(dataDirURL, "com.couchbase.spark.sql");
    }

    public void load(String dataDirURL, String format) throws StreamingQueryException {
//        StructType userSchema = new StructType()
//                .add("name", "string")
//                .add("age", "integer");
//        SQLContext sql = new SQLContext(sc);
        Dataset<Row> lines = spark.readStream()
                .json(dataDirURL);

        lines
                .select(
                        new Column("id"),
                        new Column("name"),
                        new Column("rating"),
                        new Column("review_count"),
                        new Column("hours"),
                        new Column("attributes"))
                .writeStream()
                .option("idField", "id")
                .format(format)
                .start()
                .awaitTermination(sparkProperties.getTerminationTimeoutMillis());
    }
}
