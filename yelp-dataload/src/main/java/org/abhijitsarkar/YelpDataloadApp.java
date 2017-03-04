package org.abhijitsarkar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Abhijit Sarkar
 */
@SpringBootApplication
public class YelpDataloadApp implements CommandLineRunner {
    @Autowired
    private YelpDataloadService service;

    @Value("${yelp.dataDirURL}")
    String dataDirURL;

    public static void main(String[] args) {
        SpringApplication.run(YelpDataloadApp.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        service.load(dataDirURL);
    }
}
