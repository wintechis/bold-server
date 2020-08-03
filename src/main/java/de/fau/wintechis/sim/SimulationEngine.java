package de.fau.wintechis.sim;

import de.fau.wintechis.io.FileUtils;
import org.eclipse.jetty.server.Server;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Stream;

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

    private final Logger log = LoggerFactory.getLogger(SimulationEngine.class);

    private final Integer timeSlotDuration = 100; // TODO as config parameter

    private EngineState currentState = EngineState.CREATED;

    private SimulationHandler handler; // FIXME should be final

    private final Model dataset = new LinkedHashModel();

    private final Map<String, Update> singleUpdates = new LinkedHashMap<>();

    private final Map<String, Update> continuousUpdates = new LinkedHashMap<>();

    private final Map<String, TupleQuery> queries = new HashMap<>();

    private final Map<TupleQuery, Writer> writers = new HashMap<>(); // FIXME ref is not necessary (only used during replay)

    private String dumpPattern = null;

    private String interactionFilename = "interactions.tsv"; // FIXME as config parameter

    private final RepositoryConnection connection;

    private final UpdateHistory updateHistory = new UpdateHistory();

    private final InteractionHistory interactionHistory = new InteractionHistory();

    private Timer timer;

    private BooleanQuery simRunningQuery = null; // TODO clean assignment

    public SimulationEngine(int port) {
        // RDF store initialization
        Vocabulary.registerFunctions();
        MemoryStore store = new MemoryStore();
        Repository repo = new SailRepository(store);
        connection = repo.getConnection();

        try {
            handler = new SimulationHandler(this, port);

            // sim resource must be updated first, before any other resource
            registerSingleUpdate("sim-init.rq");
            registerContinuousUpdate("sim.rq");

            // simulation ends when no iteration is left in sim resource
            String buf = FileUtils.asString(FileUtils.getFileOrResource("sim-running.rq"));
            simRunningQuery = connection.prepareBooleanQuery(QueryLanguage.SPARQL, buf, handler.getBaseURI());

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
        Update u = connection.prepareUpdate(QueryLanguage.SPARQL, sparulString, handler.getBaseURI());
        continuousUpdates.put(name, u);

        return this;
    }

    public SimulationEngine registerQuery(String filename) throws IOException {
        String buf = FileUtils.asString((FileUtils.getFileOrResource(filename)));
        registerQuery(filename, buf);

        return this;
    }

    public SimulationEngine registerQuery(String name, String sparqlString) throws IOException {
        TupleQuery q = connection.prepareTupleQuery(QueryLanguage.SPARQL, sparqlString, handler.getBaseURI());
        queries.put(name, q);

        return this;
    }

    public SimulationEngine registerDataset(String filename) throws IOException {
        RDFFormat format = Rio.getParserFormatForFileName(filename).orElseThrow(() -> new IOException());
        Model ds = Rio.parse(FileUtils.getFileOrResource(filename), handler.getBaseURI(), format);
        dataset.addAll(ds);

        return this;
    }

    public SimulationEngine registerSingleUpdate(String filename) throws IOException {
        String buf = FileUtils.asString((FileUtils.getFileOrResource(filename)));
        registerSingleUpdate(filename, buf);

        return this;
    }

    public SimulationEngine registerSingleUpdate(String name, String sparulString) throws IOException {
        Update u = connection.prepareUpdate(QueryLanguage.SPARQL, sparulString, handler.getBaseURI());
        singleUpdates.put(name, u);

        return this;
    }

    public SimulationEngine setDumpPattern(String filenamePattern) {
        dumpPattern = filenamePattern;

        return this;
    }

    public void registrationDone() {
        callTransition();
    }

    /**
     * For test purposes.
     *
     * @return the engine's repository connection
     */
    RepositoryConnection getConnection() {
        return connection;
    }

    void callTransition() {
        switch (currentState) {
            case CREATED:
                log.info("Simulation engine created.");
                // TODO move constructor statements to separate function?
                currentState = EngineState.CONFIGURED;
                break;

            case CONFIGURED:
                // configuration done by successive calls to class methods
                // TODO use a Configuration object
                log.info("Simulation engine configured. Current configuration: (single updates) {}; (continuous updates) {}; (queries) {}; (dump pattern) {}.", singleUpdates.keySet(), continuousUpdates.keySet(), queries.keySet(), dumpPattern);
                log.info("Waiting for agent's start command...");
                currentState = EngineState.EMPTY_STORE;
                break;

            case EMPTY_STORE:
                log.info("Initializing simulation run...");
                init();
                log.info("Simulation ready: {} resources, {} quads in dataset.", dataset.contexts().size(), dataset.size());
                // TODO log nb of iterations (estimated duration)
                currentState = EngineState.READY;
                callTransition();
                break;

            case READY:
                log.info("Simulation running...");
                run();
                currentState = EngineState.RUNNING;
                handler.callTransition();
                break;

            case RUNNING:
                Boolean simRunning = simRunningQuery.evaluate();
                if (simRunning) {
                    update();
                } else {
                    currentState = EngineState.REPLAYING;
                    handler.callTransition();
                    callTransition();
                }
                break;

            case REPLAYING:
                log.info("Simulation run done. Replaying simulation...");
                replay();
                log.info("Replay done.");
                currentState = EngineState.DIRTY_STORE;
                callTransition();
                break;

            case DIRTY_STORE:
                log.info("Results written to file(s). Cleaning resources...");
                clean();
                currentState = EngineState.CONFIGURED;
                handler.callTransition();
                callTransition();
                break;

            default:
                throw new IllegalSimulationStateException();
        }
    }

    private void init() {
        getNotifyingSailConnection(connection).addConnectionListener(updateHistory);
        getNotifyingSailConnection(handler.getGraphStoreHandler().getConnection()).addConnectionListener(updateHistory);

        handler.getGraphStoreHandler().addGraphStoreListener(interactionHistory);

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
        updateHistory.timeIncremented();
        interactionHistory.timeIncremented();
        for (Update u : continuousUpdates.values()) u.execute();
    }

    private void replay() {
        timer.cancel();
        timer.purge();

        getNotifyingSailConnection(connection).removeConnectionListener(updateHistory);
        getNotifyingSailConnection(handler.getGraphStoreHandler().getConnection()).removeConnectionListener(updateHistory);

        handler.getGraphStoreHandler().removeGraphStoreListener(interactionHistory);

        for (Map.Entry<String, TupleQuery> kv : queries.entrySet()) {
            try {
                String name = kv.getKey().replaceFirst("(\\.rq|\\.sparql)?$", ".tsv");
                writers.put(kv.getValue(),  new FileWriter(name, true));
                // TODO use makePath and put all results in a /results folder
            } catch (IOException e) {
                e.printStackTrace(); // TODO clean error handling
            }
        }

        RDFFormat dumpFormat = null;
        if (dumpPattern != null) {
            FileUtils.makePath(dumpPattern);
            dumpFormat = Rio.getParserFormatForFileName(dumpPattern).orElse(RDFFormat.TRIG);
        }

        connection.clear();

        // replays updates and submits queries at each timestamp
        for (int iteration = 0; iteration < updateHistory.size(); iteration++) {
            try {
                UpdateHistory.UpdateSequence cs = updateHistory.get(iteration);

                log.info("Replaying iteration {}...", iteration);

                int insertions = 0, deletions = 0;
                for (UpdateHistory.Update u : cs) {
                        if (u.getOperation().equals(UpdateHistory.UpdateOperation.INSERT)) {
                            connection.add(u.getStatement());
                            insertions++;
                        } else if (u.getOperation().equals(UpdateHistory.UpdateOperation.DELETE)) {
                            connection.remove(u.getStatement());
                            deletions++;
                        }
                }

                log.info("Done {} insertions, {} deletions.", insertions, deletions);

                if (dumpFormat != null) {
                    String dumpFilename = String.format(dumpPattern, iteration);
                    try {
                        Writer dumpWriter = new FileWriter(dumpFilename);
                        connection.export(Rio.createWriter(dumpFormat, dumpWriter));

                        log.info("Dumped dataset to {}.", dumpFilename);
                    } catch (IOException e) {
                        e.printStackTrace(); // TODO clean error handling
                    }
                }

                for (TupleQuery q : queries.values()) {
                    try {
                        long before = System.currentTimeMillis();

                        Stream<BindingSet> stream = q.evaluate().stream().distinct();
                        String row = String.format("%d\t%d\n", iteration, stream.count());

                        long after = System.currentTimeMillis();

                        writers.get(q).append(row);

                        log.info("Executed queries in {} ms.", after - before);
                    } catch (IOException e) {
                        e.printStackTrace(); // TODO clean error handling
                    }
                }
            } catch (Exception e) {
                // TODO why is there randomly a NullPointerException here?
                e.printStackTrace(); // TODO clean error handling
            }
        }

        for (Writer w : writers.values()) {
            try {
                w.append("\n\n");
                w.close();
            } catch (IOException e) {
                e.printStackTrace(); // TODO clean error handling
            }
        }

        try {
            // FIXME filename as const
            Writer w = new FileWriter(interactionFilename, true);

            for (int iteration = 0; iteration < interactionHistory.size(); iteration++) {
                InteractionHistory.InteractionCounter c = interactionHistory.get(iteration);
                w.append(String.format("%d\t%d\t%d\t%d\n", iteration, c.getRetrievals(), c.getUpdates(), c.getDeletions(), c.getExtensions()));
            }

            w.append("\n\n");
            w.close();

            log.info("Stored interaction counts to {}.", interactionFilename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clean() {
        writers.clear();
        updateHistory.clear();
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
