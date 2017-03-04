package org.abhijitsarkar;

import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static org.springframework.util.StringUtils.isEmpty;

/**
 * @author Abhijit Sarkar
 */
@ConfigurationProperties("couchbase")
@Component
@Setter
public class CouchbaseProperties {
    private String nodes;
    private String adminUsername;
    private String adminPassword;
    private String bucketName;
    private String bucketPassword;

    public String getNodes() {
        return isEmpty(nodes) ? "localhost" : nodes;
    }

    public String getAdminUsername() {
        return isEmpty(adminUsername) ? "admin" : adminUsername;
    }

    public String getAdminPassword() {
        return isEmpty(adminPassword) ? "" : adminPassword;
    }

    public String getBucketName() {
        return isEmpty(bucketName) ? "yelp" : bucketName;
    }

    public String getBucketPassword() {
        return isEmpty(bucketPassword) ? "" : bucketPassword;
    }
}
