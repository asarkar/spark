package org.abhijitsarkar;

import com.couchbase.client.java.document.RawJsonDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.couchbase.spark.StoreMode.UPSERT;
import static com.couchbase.spark.japi.CouchbaseDocumentRDD.couchbaseDocumentRDD;
import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * @author Abhijit Sarkar
 */
@RequiredArgsConstructor
@Service
public class YelpHttpToCouchbaseDataloadService {
    private static transient final List<String> ALLOWED_FIELDS =
            Arrays.asList("name", "rating", "review_count", "hours", "attributes");

    private final SparkProperties sparkProperties;
    private final CouchbaseProperties couchbaseProperties;

    @Value("${spring.application.name:UNKNOWN}")
    private String appName;

    private SparkSession spark;
    private JavaSparkContext sc;
//    private SQLContext sql;

    @PostConstruct
    void postConstruct() {
        spark = getOrCreateSparkSession();

        sc = new JavaSparkContext(spark.sparkContext());
//        sql = new SQLContext(sc);
    }

    private SparkSession getOrCreateSparkSession() {
        return SparkSession
                .builder()
                .master(sparkProperties.getMaster())
                .appName(appName)
                .config("spark.sql.streaming.schemaInference", "true")
                .config("spark.sql.streaming.checkpointLocation", sparkProperties.getCheckpointDir())
                .config("spark.couchbase.nodes", couchbaseProperties.getNodes())
                .config(String.format("spark.couchbase.bucket.%s", couchbaseProperties.getBucketName()),
                        couchbaseProperties.getBucketPassword())
                .getOrCreate();
    }

//    @PreDestroy
//    void preDestroy() {
//        if (spark != null) {
//            spark.close();
//        }
//    }

    public void load(String dataDirURL) throws StreamingQueryException, InterruptedException, IOException {
        load(dataDirURL, "com.couchbase.spark.sql");
    }

    public void load(String dataDirURL, String format) throws InterruptedException {
        JavaStreamingContext ssc = new JavaStreamingContext(sc, new Duration(1000));
        JavaReceiverInputDStream<String> lines = ssc.receiverStream(new HttpReceiver(dataDirURL));

        ObjectMapper objectMapper = new ObjectMapper();

        lines.foreachRDD(rdd -> {
                    JavaRDD<RawJsonDocument> docRdd = rdd
                            .filter(content -> !isEmpty(content))
                            .map(content -> {
                                String id = "";
                                String modifiedContent = "";
                                try {
                                    ObjectNode node = objectMapper.readValue(content, ObjectNode.class);
                                    if (node.has("id")) {
                                        id = node.get("id").textValue();
                                        modifiedContent = objectMapper.writeValueAsString(node.retain(ALLOWED_FIELDS));
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } finally {
                                    return RawJsonDocument.create(id, modifiedContent);
                                }
                            })
                            .filter(doc -> !isEmpty(doc.id()));
                    couchbaseDocumentRDD(docRdd)
                            .saveToCouchbase(UPSERT);
                }
        );

        ssc.start();
        ssc.awaitTerminationOrTimeout(sparkProperties.getTerminationTimeoutMillis());
    }

//        lines.foreachRDD(rdd -> {
//            try {
//                couchbaseWriter(spark.read().json(rdd)
//                        .filter((Row row) -> row.length() > 0)
//                        .select("id", "name", "rating", "review_count", "hours", "attributes")
//                        .write()
//                        .option("idField", "id")
//                        .format(format))
//                        .couchbase();

//                sql.read().json(rdd)
//                        .select("id", "name", "rating", "review_count", "hours", "attributes")
//                        .write()
//                        .option("idField", "id")
//                        .format(format)
//                        .save();
//            } catch (Exception e) {
//                // Time to time throws AnalysisException: cannot resolve '`id`' given input columns: [];
//                e.printStackTrace();
//            }
//        });

//        ssc.start();
//        ssc.awaitTerminationOrTimeout(sparkProperties.getTerminationTimeoutMillis());
//    }

//        StructType userSchema = new StructType()
//                .add("name", "string")
//                .add("age", "integer");
//        SQLContext sql = new SQLContext(sc);
//        Dataset<Row> lines = spark.readStream()
//                .json(dataDirURL);
//
//        lines
//                .select(
//                        new Column("id"),
//                        new Column("name"),
//                        new Column("rating"),
//                        new Column("review_count"),
//                        new Column("hours"),
//                        new Column("attributes"))
//                .writeStream()
//                .option("idField", "id")
//                .format(format)
//                .start()
//                .awaitTermination(sparkProperties.getTerminationTimeoutMillis());
//    }
}
