package de.fau.wintechis.sim;

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

    private final List<Update> updates;

    private final List<TupleQuery> queries;

    private final Map<TupleQuery, FileWriter> writers;

    private final RepositoryConnection connection;

    private final Server server;

    public SimulationEngine() {
        this.timer = new Timer();

        this.updates = new ArrayList<>();
        this.queries = new ArrayList<>();
        this.writers = new HashMap<>();

        Repository repo = new SailRepository(new MemoryStore());
        connection = repo.getConnection();

        ValueFactory vf = Vocabulary.VALUE_FACTORY;
        Resource sim = vf.createIRI(Vocabulary.NS, "sim");
        // simulation time is an arbitrary integer starting from 0
        // (makes it easier to formulate time-dependent updates in SPARQL)
        connection.add(sim, Vocabulary.CURRENT_TIME, vf.createLiteral(0));

        // time must be updated first, before any other resource
        this.updates.add(connection.prepareUpdate(UPDATE_TIME));

        server = new Server(8080); // TODO as env or class constructor argument
        server.setHandler(new GraphStoreHandler(repo));

        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace(); // TODO clean error handling
        }

        Vocabulary.registerFunctions();
    }

    public void registerUpdate(String sparulString) {
        Update u = connection.prepareUpdate(sparulString);
        this.updates.add(u);
    }

    public void registerQuery(String sparqlString) {
        TupleQuery q = connection.prepareTupleQuery(sparqlString);
        this.queries.add(q);
    }

    /**
     * First tries to open the file from the file system. If it does not exist, interpret it as a resource file.
     * The default base URI is the local server's.
     *
     * @param filename
     * @throws IOException
     */
    public void loadData(String filename) throws IOException {
        String base = server.getURI().toString();
        RDFFormat format = Rio.getParserFormatForFileName(filename).orElseThrow(() -> new IOException());

        File f = new File(filename);
        URL url = SimulationEngine.class.getClassLoader().getResource(filename);
        InputStream is = f.exists() ? new FileInputStream(f) : url.openStream();

        connection.add(is, base, format);
    }

    public void run(Integer timeSlot, Integer iterations) {
        for (TupleQuery q : queries) {
            try {
                writers.put(q, new FileWriter(q.hashCode() + ".dat"));
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
            for (TupleQuery q : queries) {
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
                for (Update u : updates) {
                    u.execute();
                }
            }
        }
    }

}
