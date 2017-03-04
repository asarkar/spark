package org.abhijitsarkar;

import com.google.common.io.Files;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static org.springframework.util.StringUtils.isEmpty;

/**
 * @author Abhijit Sarkar
 */
@Component
@ConfigurationProperties("spark")
@Setter
// http://spark.apache.org/docs/latest/configuration.html
public class SparkProperties {
    private String master;
    private String checkpointDir;
    private long terminationTimeoutMillis;

    public String getMaster() {
        return isEmpty(master) ? "local[*]" : master;
    }

    public String getCheckpointDir() {
        return isEmpty(checkpointDir) ?
                Files.createTempDir().getAbsolutePath() : checkpointDir;
    }

    public long getTerminationTimeoutMillis() {
        return terminationTimeoutMillis == 0 ? 60000l : terminationTimeoutMillis;
    }
}
