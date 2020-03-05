package de.fau.wintechis;

import de.fau.wintechis.sim.SimulationEngine;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

public class Demo {

    public static void main(String[] args) throws IOException {
        SimulationEngine engine = new SimulationEngine();

        engine.registerUpdate("occupancy.rq");
        engine.registerQuery("report.rq");

        engine.loadData("init.trig");

        engine.run(100, 720);
    }

}
