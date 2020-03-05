package de.fau.wintechis.sim;

import de.fau.wintechis.Demo;
import de.fau.wintechis.gsp.GraphStoreHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.io.*;
import java.net.URL;
import java.util.*;

public class SimulationEngine {

    private final static String UPDATE_TIME =
        "PREFIX : <" + Vocabulary.NS + ">\n" +
        "DELETE { ?sim :currentTime ?time } INSERT { ?sim :currentTime ?time_p }\n" +
        "WHERE { ?sim :currentTime ?time BIND (?time + 1 AS ?time_p) }";

    private final Timer timer;

    private final Map<String, Update> updates;

    private final Map<String, TupleQuery> queries;

    private final Map<TupleQuery, FileWriter> writers;

    private final RepositoryConnection connection;

    private final Server server;

    public SimulationEngine() {
        this.timer = new Timer();

        this.updates = new HashMap<>();
        this.queries = new HashMap<>();
        this.writers = new HashMap<>();

        Repository repo = new SailRepository(new MemoryStore());
        connection = repo.getConnection();

        ValueFactory vf = Vocabulary.VALUE_FACTORY;
        Resource sim = vf.createIRI(Vocabulary.NS, "sim");
        // simulation time is an arbitrary integer starting from 0
        // (makes it easier to formulate time-dependent updates in SPARQL)
        connection.add(sim, Vocabulary.CURRENT_TIME, vf.createLiteral(0));

        // time must be updated first, before any other resource
        this.updates.put("sim.rq", connection.prepareUpdate(UPDATE_TIME));

        server = new Server(8080); // TODO as env or class constructor argument
        server.setHandler(new GraphStoreHandler(repo));

        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace(); // TODO clean error handling
        }

        Vocabulary.registerFunctions();
    }

    public void registerUpdate(String filename) throws IOException {
        String buf = asString((getFileOrResource(filename)));
        this.registerUpdate(filename, buf);
    }

    public void registerUpdate(String name, String sparulString) throws IOException {
        Update u = connection.prepareUpdate(sparulString);
        this.updates.put(name, u);
    }

    public void registerQuery(String filename) throws IOException {
        String buf = asString((getFileOrResource(filename)));
        this.registerQuery(filename, buf);
    }

    public void registerQuery(String name, String sparqlString) throws IOException {
        TupleQuery q = connection.prepareTupleQuery(sparqlString);
        this.queries.put(name, q);
    }

    public void loadData(String filename) throws IOException {
        String base = server.getURI().toString();
        RDFFormat format = Rio.getParserFormatForFileName(filename).orElseThrow(() -> new IOException());

        connection.add(getFileOrResource(filename), base, format);
    }

    public void run(Integer timeSlot, Integer iterations) {
        for (Map.Entry<String, TupleQuery> kv : queries.entrySet()) {
            try {
                String name = kv.getKey().replaceFirst("(\\.rq|\\.sparql)?$", ".dat");
                writers.put(kv.getValue(), new FileWriter(name));
            } catch (IOException e) {
                e.printStackTrace(); // TODO clean error handling
            }
        }

        TimerTask task = new IterationTask(iterations);
        timer.scheduleAtFixedRate(task, 0, timeSlot);
    }

    private class IterationTask extends TimerTask {

        private Integer count = 0;

        private Integer maxIterations;

        public IterationTask(Integer max) {
            this.maxIterations = max;
        }

        @Override
        public void run() {
            for (TupleQuery q : queries.values()) {
                TupleQueryResult res = q.evaluate();
                List<String> vars = res.getBindingNames();
                for (BindingSet mu : q.evaluate()) {
                    String row = "";

                    for (String v : vars) {
                        if (!row.isEmpty()) row += "\t";
                        row += mu.getValue(v).stringValue();
                    }
                    row += "\n";

                    try {
                        writers.get(q).append(row);
                    } catch (IOException e) {
                        e.printStackTrace(); // TODO clean error handling
                    }
                }
            }

            if (count++ > maxIterations) {
                for (FileWriter w : writers.values()) {
                    try {
                        w.close();
                    } catch (IOException e) {
                        e.printStackTrace(); // TODO clean error handling
                    }
                }

                timer.cancel();
                connection.close();
                try {
                    server.stop();
                } catch (Exception e) {
                    e.printStackTrace(); // TODO clean error handling
                }
            } else {
                for (Update u : updates.values()) {
                    u.execute();
                }
            }
        }
    }

    /**
     * First tries to open the file from the file system. If it does not exist, interpret it as a resource file.
     *
     * @param filename name of the file or resource
     * @return an input stream pointing to the content of the file or resource
     * @throws IOException
     */
    private InputStream getFileOrResource(String filename) throws IOException {
        File f = new File(filename);
        URL url = SimulationEngine.class.getClassLoader().getResource(filename);

        return f.exists() ? new FileInputStream(f) : url.openStream();
    }

    /**
     * Buffers the content of an input stream into a string.
     *
     * @param is the input stream
     * @return the content of the stream buffered into a string
     * @throws IOException
     */
    private String asString(InputStream is) throws IOException {
        StringWriter w = new StringWriter();

        int buf = -1;
        while ((buf = is.read()) > -1) w.write(buf);

        return w.toString();
    }

}
