# Summary
Reads a Gzip JSON file from an HTTP endpoint, does basic transformation and writes to console (default) or Couchbase.

## To run
1. Build
   ```
   $ bin/activator clean docker:publishLocal
   ```

2a. Destination stdout
    ```
    $ docker run --rm -it <image_id> <url>
    ```
   
2b. Destination Couchbase

    ```
    $ docker run -d --name couchbase -p 8091-8094:8091-8094 -p 11210:11210 couchbase/server
    ```
    Go to localhost:8091 and set up Couchbase.
    ```
    $ docker run --rm -e CB_NODES=<cb_nodes> -it <image_id> <url> couchbase
    ```