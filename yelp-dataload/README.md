# Summary
Reads a Gzip JSON file from an HTTP endpoint, does basic transformation and spits out each record to console.

## To run
1. Build
   ```
   $ bin/activator clean docker:publishLocal
   ```

2. Run
   ```
   $ docker run --rm -it <image_id> <url>
   ```