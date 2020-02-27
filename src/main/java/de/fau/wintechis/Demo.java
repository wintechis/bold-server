package de.fau.wintechis;

import de.fau.wintechis.sim.SimulationEngine;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

public class Demo {

    public static void main(String[] args) {
        SimulationEngine engine = new SimulationEngine();

        engine.registerUpdate(getResource("/occupancy.rq"));
        engine.registerQuery(getResource("/report.rq"));
        engine.loadData("init.ttl");

        engine.run(100, 720);
    }

    private static String getResource(String resourceName) {
        InputStream is = Demo.class.getResourceAsStream(resourceName);
        StringWriter w = new StringWriter();

        int buf = -1;
        try {
            while ((buf = is.read()) > -1) w.write(buf);
        } catch (IOException e) {
            System.err.println("Cannot fetch resource: " + resourceName);
        }

        return w.toString();
    }

}
