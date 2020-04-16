package de.fau.wintechis.sim;

import de.fau.wintechis.gsp.GraphStoreHandler;
import de.fau.wintechis.gsp.GraphStoreListener;
import de.fau.wintechis.io.FileUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class SimulationEngine {

    private enum EngineState {
        CREATED,
        CONFIGURED,
        EMPTY_STORE,
        READY,
        RUNNING,
        REPLAYING,
        DIRTY_STORE
    }

    private final Integer timeSlotDuration = 100; // TODO as config parameter

    private EngineState currentState = EngineState.CREATED;

    private final Server server;

    private final Model dataset = new LinkedHashModel();

    private final Map<String, Update> singleUpdates = new HashMap<>();

    private final Map<String, Update> continuousUpdates = new HashMap<>();

    private final Map<String, TupleQuery> queries = new HashMap<>();

    private final Map<TupleQuery, Writer> writers = new HashMap<>();

    private final RepositoryConnection connection;

    private final UpdateHistory history = new UpdateHistory();

    private Timer timer;

    private BooleanQuery simRunningQuery = null; // TODO clean assignment

    public SimulationEngine(int port) {
        // RDF store initialization
        Vocabulary.registerFunctions();
        MemoryStore store = new MemoryStore();
        Repository repo = new SailRepository(store);
        connection = repo.getConnection();

        // GSP interface initialization
        GraphStoreHandler handler = new GraphStoreHandler(repo);
        server = new Server(port);
        server.setHandler(handler);

        try {
            server.start();

            SimListener runner = new SimListener();
            handler.addGraphStoreListener(runner);

            // sim resource must be updated first, before any other resource
            registerSingleUpdate("sim-init.rq");
            registerContinuousUpdate("sim.rq");

            // simulation ends when no iteration is left in sim resource
            String buf = FileUtils.asString(FileUtils.getFileOrResource("sim-running.rq"));
            simRunningQuery = connection.prepareBooleanQuery(QueryLanguage.SPARQL, buf, getBaseIRI(server));

            callTransition();
        } catch (Exception e) {
            e.printStackTrace(); // TODO clean error handling
        }
    }

    public SimulationEngine registerContinuousUpdate(String filename) throws IOException {
        String buf = FileUtils.asString((FileUtils.getFileOrResource(filename)));
        registerContinuousUpdate(filename, buf);

        return this;
    }

    public SimulationEngine registerContinuousUpdate(String name, String sparulString) throws IOException {
        Update u = connection.prepareUpdate(QueryLanguage.SPARQL, sparulString, getBaseIRI(server));
        continuousUpdates.put(name, u);

        return this;
    }

    public SimulationEngine registerQuery(String filename) throws IOException {
        String buf = FileUtils.asString((FileUtils.getFileOrResource(filename)));
        registerQuery(filename, buf);

        return this;
    }

    public SimulationEngine registerQuery(String name, String sparqlString) throws IOException {
        TupleQuery q = connection.prepareTupleQuery(QueryLanguage.SPARQL, sparqlString, getBaseIRI(server));
        queries.put(name, q);

        return this;
    }

    public SimulationEngine registerDataset(String filename) throws IOException {
        RDFFormat format = Rio.getParserFormatForFileName(filename).orElseThrow(() -> new IOException());
        Model ds = Rio.parse(FileUtils.getFileOrResource(filename), getBaseIRI(server), format);
        dataset.addAll(ds);

        return this;
    }

    public SimulationEngine registerSingleUpdate(String filename) throws IOException {
        String buf = FileUtils.asString((FileUtils.getFileOrResource(filename)));
        registerSingleUpdate(filename, buf);

        return this;
    }

    public SimulationEngine registerSingleUpdate(String name, String sparulString) throws IOException {
        Update u = connection.prepareUpdate(QueryLanguage.SPARQL, sparulString, getBaseIRI(server));
        singleUpdates.put(name, u);

        return this;
    }

    public void registrationDone() {
        callTransition();
    }

    public void terminate() throws Exception {
        server.stop();
    }

    /**
     * For test purposes.
     *
     * @return the engine's repository connection
     */
    RepositoryConnection getConnection() {
        return connection;
    }

    /**
     * This class listens to changes to the repository coming from agents and
     * runs a calls for a transition if the 'sim' resource is being updated/deleted.
     */
    private class SimListener implements GraphStoreListener {

        private final IRI sim;

        public SimListener() {
            sim = Vocabulary.VALUE_FACTORY.createIRI(server.getURI().resolve("sim").toString());
        }

        @Override
        public void graphRetrieved(IRI graphName) {
            // does nothing
        }

        @Override
        public void graphUpdated(IRI graphName) {
            if (graphName.equals(sim)) {
                callTransition();
            }
        }

        @Override
        public void graphExtended(IRI graphName) {
            graphUpdated(graphName);
        }

        @Override
        public void graphDeleted(IRI graphName) {
            graphUpdated(graphName);
        }

    }

    private void callTransition() {
        switch (currentState) {
            case CREATED:
                // TODO move constructor statements to separate function?
                currentState = EngineState.CONFIGURED;
                break;

            case CONFIGURED:
                // configuration done by successive calls to class methods
                currentState = EngineState.EMPTY_STORE;
                break;

            case EMPTY_STORE:
                init();
                currentState = EngineState.READY;
                callTransition();
                break;

            case READY:
                run();
                currentState = EngineState.RUNNING;
                break;

            case RUNNING:
                Boolean simRunning = simRunningQuery.evaluate();
                if (simRunning) {
                    update();
                } else {
                    replay();
                    currentState = EngineState.REPLAYING;
                    callTransition();
                }
                break;

            case REPLAYING:
                close();
                currentState = EngineState.DIRTY_STORE;
                callTransition();
                break;

            case DIRTY_STORE:
                clean();
                currentState = EngineState.CONFIGURED;
                callTransition();
                break;

            default:
                throw new IllegalEngineStateException();
        }
    }

    private void init() {
        getNotifyingSailConnection(connection).addConnectionListener(history);

        connection.add(dataset);
        for (Update u : singleUpdates.values()) u.execute();
    }

    private void run() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                callTransition();
            }
        };

        timer = new Timer();
        timer.scheduleAtFixedRate(task, 0, timeSlotDuration);
    }

    private void update() {
        history.timeIncremented();
        for (Update u : continuousUpdates.values()) u.execute();
    }

    private void replay() {
        timer.cancel();
        timer.purge();
        getNotifyingSailConnection(connection).removeConnectionListener(history);

        for (Map.Entry<String, TupleQuery> kv : queries.entrySet()) {
            try {
                String name = kv.getKey().replaceFirst("(\\.rq|\\.sparql)?$", ".tsv");
                writers.put(kv.getValue(),  new FileWriter(name));
            } catch (IOException e) {
                e.printStackTrace(); // TODO clean error handling
            }
        }

        connection.clear();

        // replays updates and submits queries at each timestamp
        for (UpdateHistory.Update cs : history) {
            connection.remove(cs.getDeletions());
            connection.add(cs.getInsertions());

            for (TupleQuery q : queries.values()) {
                // TODO put this code in a TupleQueryResultHandler
                // vars are ordered for deterministic rendering
                Set<String> vars = new TreeSet<>();
                for (BindingSet mu : q.evaluate()) {
                    String row = "";

                    if (vars.isEmpty()) {
                        vars.addAll(mu.getBindingNames());
                    }

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
        }
    }

    private void close() {
        for (Writer w : writers.values()) {
            try {
                w.close();
            } catch (IOException e) {
                e.printStackTrace(); // TODO clean error handling
            }
        }
    }

    private void clean() {
        writers.clear();
        history.clear();
        connection.clear();
    }

    private static NotifyingSailConnection getNotifyingSailConnection(RepositoryConnection con) {
        SailConnection sailCon = ((SailRepositoryConnection) con).getSailConnection();
        return (NotifyingSailConnection) sailCon;
    }

    private static String getBaseIRI(Server server) {
        return server.getURI().toString();
    }

}
