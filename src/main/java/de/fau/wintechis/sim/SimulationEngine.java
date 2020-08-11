package de.fau.wintechis.sim;

import de.fau.wintechis.io.FileUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.event.NotifyingRepository;
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

    public enum EngineState {
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

    private final Model dataset = new LinkedHashModel();

    private final Map<String, Update> singleUpdates = new LinkedHashMap<>();

    private final Map<String, Update> continuousUpdates = new LinkedHashMap<>();

    private final Map<String, TupleQuery> queries = new HashMap<>();

    private final Map<TupleQuery, Writer> writers = new HashMap<>(); // FIXME ref is not necessary (only used during replay)

    private String dumpPattern = null;

    private String interactionFilename = "interactions.tsv"; // FIXME as config parameter

    private final RepositoryConnection connection;

    private final RepositoryConnection replayConnection;

    private final UpdateHistory updateHistory;

    private final InteractionHistory interactionHistory;

    private final String baseURI;

    private Timer timer;

    private BooleanQuery simRunningQuery = null; // TODO clean assignment

    public SimulationEngine(String base, RepositoryConnection con, UpdateHistory updates, InteractionHistory interactions) {
        baseURI = base;

        // RDF store initialization
        Vocabulary.registerFunctions();
        connection = con;
        replayConnection = con.getRepository().getConnection();

        updateHistory = updates;
        interactionHistory = interactions;

        try {
            // sim resource must be updated first, before any other resource
            registerSingleUpdate("sim-init.rq");
            registerContinuousUpdate("sim.rq");

            // simulation ends when no iteration is left in sim resource
            String buf = FileUtils.asString(FileUtils.getFileOrResource("sim-running.rq"));
            simRunningQuery = connection.prepareBooleanQuery(QueryLanguage.SPARQL, buf, baseURI);

            callTransition();
        } catch (Exception e) {
            e.printStackTrace(); // TODO clean error handling
        }
    }

    public EngineState getCurrentState() {
        return currentState;
    }

    public SimulationEngine registerContinuousUpdate(String filename) throws IOException {
        String buf = FileUtils.asString((FileUtils.getFileOrResource(filename)));
        registerContinuousUpdate(filename, buf);

        return this;
    }

    public SimulationEngine registerContinuousUpdate(String name, String sparulString) throws IOException {
        Update u = connection.prepareUpdate(QueryLanguage.SPARQL, sparulString, baseURI);
        continuousUpdates.put(name, u);

        return this;
    }

    public SimulationEngine registerQuery(String filename) throws IOException {
        String buf = FileUtils.asString((FileUtils.getFileOrResource(filename)));
        registerQuery(filename, buf);

        return this;
    }

    public SimulationEngine registerQuery(String name, String sparqlString) throws IOException {
        TupleQuery q = replayConnection.prepareTupleQuery(QueryLanguage.SPARQL, sparqlString, baseURI);
        queries.put(name, q);

        return this;
    }

    public SimulationEngine registerDataset(String filename) throws IOException {
        RDFFormat format = Rio.getParserFormatForFileName(filename).orElseThrow(() -> new IOException());
        Model ds = Rio.parse(FileUtils.getFileOrResource(filename), baseURI, format);
        dataset.addAll(ds);

        return this;
    }

    public SimulationEngine registerSingleUpdate(String filename) throws IOException {
        String buf = FileUtils.asString((FileUtils.getFileOrResource(filename)));
        registerSingleUpdate(filename, buf);

        return this;
    }

    public SimulationEngine registerSingleUpdate(String name, String sparulString) throws IOException {
        Update u = connection.prepareUpdate(QueryLanguage.SPARQL, sparulString, baseURI);
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
                // FIXME source state should be called 'configured' (unncessary indirection)
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
                break;

            case RUNNING:
                Boolean simRunning = simRunningQuery.evaluate();
                if (simRunning) {
                    update();
                } else {
                    currentState = EngineState.REPLAYING;
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
                callTransition();
                break;

            default:
                throw new IllegalSimulationStateException();
        }
    }

    private void init() {
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
        // TODO record time elapsed to report potential time drifts
        updateHistory.timeIncremented();
        interactionHistory.timeIncremented();
        for (Update u : continuousUpdates.values()) u.execute();
    }

    private void replay() {
        timer.cancel();
        timer.purge();

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

        replayConnection.clear();

        // replays updates and submits queries at each timestamp
        for (int iteration = 0; iteration < updateHistory.size(); iteration++) {
            try {
                UpdateHistory.UpdateSequence cs = updateHistory.get(iteration);

                log.info("Replaying iteration {}...", iteration);

                int insertions = 0, deletions = 0;
                for (UpdateHistory.Update u : cs) {
                        if (u.getOperation().equals(UpdateHistory.UpdateOperation.INSERT)) {
                            replayConnection.add(u.getStatement());
                            insertions++;
                        } else if (u.getOperation().equals(UpdateHistory.UpdateOperation.DELETE)) {
                            replayConnection.remove(u.getStatement());
                            deletions++;
                        }
                }

                log.info("Done {} insertions, {} deletions.", insertions, deletions);

                if (dumpFormat != null) {
                    String dumpFilename = String.format(dumpPattern, iteration);
                    try {
                        Writer dumpWriter = new FileWriter(dumpFilename);
                        replayConnection.export(Rio.createWriter(dumpFormat, dumpWriter));

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

                        log.info("Executed query in {} ms.", after - before); // TODO sum
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
            Writer w = new FileWriter(interactionFilename, true);

            for (int iteration = 0; iteration < interactionHistory.size(); iteration++) {
                InteractionHistory.InteractionCounter c = interactionHistory.get(iteration);
                w.append(String.format("%d\t%d\t%d\t%d\t%d\n", iteration, c.getRetrievals(), c.getUpdates(), c.getDeletions(), c.getExtensions()));
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
        replayConnection.clear();
    }

}
