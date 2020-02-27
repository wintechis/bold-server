package de.fau.wintechis.sim;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class SimulationEngine {

    private final static String UPDATE_TIME =
        "PREFIX : <" + Vocabulary.NS + ">\n" +
        "DELETE { ?sim :currentTime ?time } INSERT { ?sim :currentTime ?time_p }\n" +
        "WHERE { ?sim :currentTime ?time BIND (?time + 1 AS ?time_p) }";

    private final Timer timer;

    private final List<String> updates;

    private final List<String> queries;

    private final Map<String, FileWriter> writers;

    private final RDFConnection connection;

    public SimulationEngine() {
        this.timer = new Timer();

        this.updates = new ArrayList<>();
        // time must be updated first, before any other resource
        this.updates.add(UPDATE_TIME);

        this.queries = new ArrayList<>();
        this.writers = new HashMap<>();

        Model m = ModelFactory.createDefaultModel();
        // simulation time is an arbitrary integer starting from 0
        // (makes it easier to formulate time-dependent updates in SPARQL)
        m.createResource(Vocabulary.NS + "sim").addLiteral(Vocabulary.CURRENT_TIME, 0);

        Dataset dataset = DatasetFactory.create(m);
        connection = RDFConnectionFactory.connect(dataset);

        Vocabulary.registerFunctions();
    }

    public void registerUpdate(String sparulString) {
        this.updates.add(sparulString);
    }

    public void registerQuery(String sparqlString) {
        this.queries.add(sparqlString);
    }

    public void loadData(String filename) {
        connection.load(filename);
    }

    public void run(Integer timeSlot, Integer iterations) {
        for (String q : queries) {
            try {
                writers.put(q, new FileWriter(q.hashCode() + ".dat"));
            } catch (IOException e) {
                e.printStackTrace();
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
            for (String q : queries) {
                connection.querySelect(q, (qs) -> {
                    String row = "";

                    Iterator<String> it = qs.varNames();
                    while (it.hasNext()) {
                        if (!row.isEmpty()) row += "\t";
                        row += qs.getLiteral(it.next()).getLexicalForm();
                    }
                    row += "\n";

                    try {
                        writers.get(q).append(row);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }

            if (count++ > maxIterations) {
                for (FileWriter w : writers.values()) {
                    try {
                        w.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                timer.cancel();
            } else {
                for (String u : updates) {
                    connection.update(u);
                }
            }
        }
    }

}
