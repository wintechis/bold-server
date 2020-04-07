package de.fau.wintechis;

import de.fau.wintechis.io.FileUtils;
import de.fau.wintechis.sim.SimulationEngine;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Configurator {

    private final static String SERVER_HTTP_PORT_KEY = "bold.server.httpPort";

    private final static String SERVER_HTTP_PORT_DEFAULT = "8080";

    private final static String INIT_DATASET_KEY = "bold.init.dataset";

    private final static String INIT_UPDATE_KEY = "bold.init.update";

    private final static String RUNTIME_UPDATE_KEY = "bold.runtime.update";

    private final static String RUNTIME_QUERY_KEY = "bold.runtime.query";

    public static void main(String[] args) throws IOException {
        // TODO more advanced CLI
        String task = args.length > 0 ? args[0] : "sim";

        Properties config = new Properties();
        config.load(new FileInputStream((task + ".properties")));

        int port = Integer.parseInt(config.getProperty(SERVER_HTTP_PORT_KEY, SERVER_HTTP_PORT_DEFAULT));
        SimulationEngine engine = new SimulationEngine(port);

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

        engine.registrationDone();
    }

}
