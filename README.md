# Benchmark Server

To run the server:

```shell script
gradle install
cd build/install/bbench-server
bin/bbench-server
```

Default server configuration is stored in `sim.properties`.
The different tasks of the benchmark also have their own configuration file.
The first argument of the server command is a task name, e.g. `bin/bbench-server ts1` will load `ts1.properties`.

To start/stop a simulation run, send the following HTTP requests to the server:

 - `PUT /sim` with payload `data/sim.ttl` (or any RDF graph giving a number of iterations to run)
 - `DELETE /sim` (or any request that deletes that number of iterations)

See also `run.sh` (to execute after the server has started on port 8080).

At the end of a simulation run, the solution for each registered query is stored in a TSV file
(same location as the corresponding query file). E.g., the result for `query/ts1.rq` will be stored in `query/ts1.tsv`.
Note that any consequent run will overwrite all TSV files.