package de.fau.wintechis;

import de.fau.wintechis.io.FileUtils;
import de.fau.wintechis.sim.SimulationEngine;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Configurator {

    private final static String SERVER_HTTP_PORT_KEY = "boldng.server.httpPort";

    private final static String SERVER_HTTP_PORT_DEFAULT = "8080";

    private final static String INIT_DATASET_KEY = "boldng.init.dataset";

    private final static String INIT_UPDATE_KEY = "boldng.init.update";

    private final static String RUNTIME_UPDATE_KEY = "boldng.runtime.update";

    private final static String RUNTIME_QUERY_KEY = "boldng.runtime.query";

    public static void main(String[] args) throws IOException {
        // TODO provide as CLI argument
        String propertiesFilename = "ts1.properties";
        //String propertiesFilename = "ts2.properties";
        //String propertiesFilename = "sim.properties";

        Properties config = new Properties();
        config.load(new FileInputStream(("sim.properties")));

        int port = Integer.parseInt(config.getProperty(SERVER_HTTP_PORT_KEY, SERVER_HTTP_PORT_DEFAULT));
        SimulationEngine engine = new SimulationEngine(port);

        for (String f : FileUtils.listFiles(config.getProperty(INIT_DATASET_KEY))) {
            engine.loadData(f);
        }

        for (String f : FileUtils.listFiles(config.getProperty(INIT_UPDATE_KEY))) {
            engine.executeUpdate(f);
        }

        for (String f : FileUtils.listFiles(config.getProperty(RUNTIME_UPDATE_KEY))) {
            engine.registerUpdate(f);
        }

        for (String f : FileUtils.listFiles(config.getProperty(RUNTIME_QUERY_KEY))) {
            engine.registerQuery(f);
        }
    }

}
