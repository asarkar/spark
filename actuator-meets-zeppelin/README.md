# Summary
This is an example of analyzing data obtained from Spring Boot Actuator [metrics](https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-metrics.html).

## Run
1. Start Zeppelin as follows:

   ```
   docker run --rm --name=zeppelin \
   -p 8080:8080 -p 8081:8081 \
   -v $PWD/data/:/srv/metrics/:ro \
   -d asarkar/zeppelin:0.7.0
   ```
2. Click on "Import note" from the [home page](http://localhost:8080) and import
   any of the files in the `notes` directory of this project.
3. Click on the newly created note. It'll open and run all the paragraphs automatically.
4. In the 1st paragraph, change the directory name to any of the ones in the `data` directory,
   and press "Run all paragraphs" (the `>`-ish button on the top).
   All data will be recalculated.
5. To change the graph, press any of the graph buttons, and then click "settings".
   Drag and drop columns to the "Keys" and "Values" text areas, or remove existing ones.
6. To temporarily remove a plot from the graph, toggle the respective color coded circle.
