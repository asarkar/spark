package org.abhijitsarkar;

import org.apache.spark.streaming.receiver.Receiver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.spark.storage.StorageLevel.MEMORY_AND_DISK;

/**
 * @author Abhijit Sarkar
 */
public class HttpReceiver extends Receiver<String> {
    private final String url;

    public HttpReceiver(String url) {
        super(MEMORY_AND_DISK());
        this.url = url;
    }

    @Override
    public void onStart() {
        new Thread(() -> receive()).start();
    }

    private void receive() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setAllowUserInteraction(false);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestMethod("GET");
            conn.setReadTimeout(60 * 1000);

            InputStream gzipStream = new GZIPInputStream(conn.getInputStream());
            Reader decoder = new InputStreamReader(gzipStream, UTF_8);
            BufferedReader reader = new BufferedReader(decoder);

            String json = null;
            while (!isStopped() && (json = reader.readLine()) != null) {
                store(json);
            }
            reader.close();
            conn.disconnect();
        } catch (IOException e) {
            stop(e.getMessage(), e);
        }
    }

    @Override
    public void onStop() {

    }
}
