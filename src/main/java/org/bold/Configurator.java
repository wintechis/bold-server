package org.bold;

import org.bold.io.FileUtils;
import org.bold.sim.SimulationEngine;
import org.bold.sim.SimulationHandler;
import org.eclipse.rdf4j.model.vocabulary.SP;

import java.io.FileInputStream;
import java.util.Properties;

public class Configurator {

    public static long randomSeed;

    private final static String SERVER_HTTP_PORT_KEY = "bold.server.httpPort";

    private final static String SERVER_HTTP_PORT_DEFAULT = "8080";

    private final static String SERVER_PROTOCOL = "bold.server.protocol";

    private final static String SERVER_PROTOCOL_DEFAULT = "gsp";

    private final static String SERVER_WEBSOCKET = "bold.server.webSocket";

    private final static String SERVER_WEBSOCKET_DEFAULT = "false";

    private final static String INIT_DATASET_KEY = "bold.init.dataset";

    private final static String INIT_UPDATE_KEY = "bold.init.update";

    private final static String RUNTIME_UPDATE_KEY = "bold.runtime.update";

    private final static String RUNTIME_QUERY_KEY = "bold.runtime.query";

    private final static String REPLAY_DUMP_KEY = "bold.replay.dump";

    private final static String SPARQL_RANDOM_SEED = "bold.sparql.randomSeed";

    private final static String SPARQL_RANDOM_SEED_DEFAULT = "1";

    private final static String SPARQL_RESULT_FILE = "bold.sparql.resultFile";

    private final static String SPARQL_RESULT_FILE_DEFAULT = "result.csv";

    public static void main(String[] args) throws Exception {
        // TODO more advanced CLI
        String task = args.length > 0 ? args[0] : "sim";

        Properties config = new Properties();
        config.load(new FileInputStream((task + ".properties")));

        randomSeed = Long.parseLong(config.getProperty(SPARQL_RANDOM_SEED, SPARQL_RANDOM_SEED_DEFAULT));

        int port = Integer.parseInt(config.getProperty(SERVER_HTTP_PORT_KEY, SERVER_HTTP_PORT_DEFAULT));
        String protocol = config.getProperty(SERVER_PROTOCOL, SERVER_PROTOCOL_DEFAULT);
        String webSocket = config.getProperty(SERVER_WEBSOCKET, SERVER_WEBSOCKET_DEFAULT);
        String resultFile = config.getProperty(SPARQL_RESULT_FILE, SPARQL_RESULT_FILE_DEFAULT);
        SimulationHandler handler = new SimulationHandler(port, protocol, Boolean.parseBoolean(webSocket), resultFile);

        SimulationEngine engine = handler.getSimulationEngine();

        for (String f : FileUtils.listFiles(config.getProperty(INIT_DATASET_KEY))) {
            engine.registerDataset(f);
        }

        for (String f : FileUtils.listFiles(config.getProperty(INIT_UPDATE_KEY))) {
            engine.registerSingleUpdate(f);
        }

        for (String f : FileUtils.listFiles(config.getProperty(RUNTIME_UPDATE_KEY))) {
            engine.registerContinuousUpdate(f);
        }

        for (String f : FileUtils.listFiles(config.getProperty(RUNTIME_QUERY_KEY))) {
            engine.registerQuery(f);
        }

        String filenamePattern = config.getProperty(REPLAY_DUMP_KEY);
        engine.setDumpPattern(filenamePattern);

        engine.registrationDone();
    }

}
