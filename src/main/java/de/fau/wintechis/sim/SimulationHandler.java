package de.fau.wintechis.sim;

import de.fau.wintechis.gsp.GraphStoreHandler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SimulationHandler extends AbstractHandler {

    public static final String SIMULATION_RESOURCE_TARGET = "/sim";

    private enum HandlerState {
        READY,
        RUNNING,
        REPLAYING
    }

    private final Logger log = LoggerFactory.getLogger(SimulationHandler.class);

    private HandlerState currentState = HandlerState.READY;

    private final Server server;

    private final GraphStoreHandler handler;

    private final SimulationEngine engine;

    public SimulationHandler(SimulationEngine ng, int port) throws Exception {
        engine = ng;

        server = new Server(port);
        server.setHandler(this);
        server.start();

        log.info("Server started on port {}. Waiting for command on resource {}...", port, SIMULATION_RESOURCE_TARGET);

        // note: server's base URI is set only after server starts
        // TODO tight interdependency between handlers/engine...
        handler = new GraphStoreHandler(server.getURI(), engine.getConnection().getRepository());
    }

    public String getBaseURI() {
        return server.getURI().toString();
    }

    public GraphStoreHandler getGraphStoreHandler() {
        return handler;
    }

    void callTransition() {
        switch (currentState) {
            case READY:
                log.info("Linked Data interface over dataset exposed to agents.");
                currentState = HandlerState.RUNNING;
                break;

            case RUNNING:
                log.info("Linked Data interface closed (replaying).");
                currentState = HandlerState.REPLAYING;
                break;

            case REPLAYING:
                log.info("Reinitialized server. Waiting for command on resource {}...", SIMULATION_RESOURCE_TARGET);
                currentState = HandlerState.READY;
                break;

            default:
                throw new IllegalSimulationStateException();
        }
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        switch (currentState) {
            case READY:
                // only recognizes PUT /sim
                // TODO check validity of RDF payload with shape
                if (request.getMethod().equals("PUT") && target.equals(SIMULATION_RESOURCE_TARGET)) {
                    handler.handle(target, baseRequest, request, response);
                    engine.callTransition();
                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    baseRequest.setHandled(true);
                }
                break;

            case RUNNING:
                handler.handle(target, baseRequest, request, response);
                break;

            case REPLAYING:
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                baseRequest.setHandled(true);
                break;
        }
    }

}
