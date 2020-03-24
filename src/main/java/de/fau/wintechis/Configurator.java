package de.fau.wintechis;

import de.fau.wintechis.sim.SimulationEngine;

import java.io.IOException;

public class Configurator {

    public static void main(String[] args) throws IOException {
        SimulationEngine engine = new SimulationEngine();

        // pre-configured building data
        engine.loadData("IBM_B3.trig");
        // TODO configure raw IBM Turtle data with engine.executeUpdate()?

        // simple task 1: turn all lights off
//        engine.executeUpdate("ts1-init.rq");
//        engine.registerQuery("ts1.rq");

        // simple task 2: toggle all lights
        engine.executeUpdate("ts2-init.rq");
        engine.registerQuery("ts2.rq");

        // general simulation of occupants over one day
//        engine.executeUpdate("occupants.ttl");
//        engine.registerUpdate("occupant-actions.rq");
//        engine.registerUpdate("building-reactions.rq");
//        engine.registerQuery("occupancy.rq");

        // 720 iterations = 12h (~ one day, starting from 8am)
//        engine.run(100, 720);

        // TODO change to higher number of iterations (10 set only for quick testing)
        engine.run(100, 10);
    }

}
