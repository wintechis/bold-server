package de.fau.wintechis;

import de.fau.wintechis.sim.SimulationEngine;

import java.io.IOException;

public class Demo {

    public static void main(String[] args) throws IOException {
        SimulationEngine engine = new SimulationEngine();

        engine.registerUpdate("occupant-actions.rq");
        engine.registerUpdate("building-reactions.rq");
        engine.registerQuery("ts1.rq");
        //engine.registerQuery("report.rq");

        engine.loadData("IBM_B3.trig");
        engine.loadData("occupants.ttl");

        engine.run(100, 720);
    }

}
