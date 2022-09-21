package org.bold.sim;

import org.bold.http.GraphHandler;
import org.bold.http.GraphStoreHandler;
import org.bold.http.LDPHandler;
import org.bold.http.WebSocketHandler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fau.rw.ti.LDPInferencer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * High-level handler providing an interface to agents for managing simulations.
 * After configuration is done, the simulation handler waits for a command to start the simulation (PUT /sim). When a
 * simulation runs, processing is delegated to {@link GraphStoreHandler}.
 */
public class SimulationHandler extends AbstractHandler {

    public static final String SIMULATION_RESOURCE_TARGET = "/sim";

    public static final String PUBLIC_RESOURCE_FOLDER = "doc"; // TODO make it relative to distribution, not working directory

    private String resultFile;

    private final Logger log = LoggerFactory.getLogger(SimulationHandler.class);

    private final Server server;

    private final ResourceHandler staticHandler;

    private final GraphHandler graphHandler;

    private final WebSocketHandler webSocketHandler;

    private final SimulationEngine engine;

    public SimulationHandler(int port, String protocol, boolean webSocket, String resultFile) throws Exception {
        this.resultFile = resultFile;
        
        server = new Server(InetSocketAddress.createUnresolved("127.0.1.1", port));
        server.setHandler(this);
        server.start();

        // TODO isn't there a handler collection to do the job instead?
        staticHandler = new ResourceHandler();
        staticHandler.setBaseResource(Resource.newResource(PUBLIC_RESOURCE_FOLDER));
        staticHandler.setDirectoriesListed(false);
        staticHandler.setWelcomeFiles(new String[] { "index.html" });
        staticHandler.doStart();

        MemoryStore store = new MemoryStore();
        SailRepository  repo;
        switch(protocol) {
            case "ldp":
                repo = new SailRepository(new LDPInferencer(store));
                break;
            default:
                repo = new SailRepository(store);
                break;
        }

        if(webSocket) {
            webSocketHandler = new WebSocketHandler(port + 1, repo);
            webSocketHandler.start();
        } else {
            webSocketHandler = null;
        }

        UpdateHistory history = new UpdateHistory(); // TODO finer-grained reporting: distinct histories
        SailRepositoryConnection engineConnection = repo.getConnection();
        SailRepositoryConnection handlerConnection = repo.getConnection();
        ((NotifyingSailConnection) engineConnection.getSailConnection()).addConnectionListener(history);
        ((NotifyingSailConnection) handlerConnection.getSailConnection()).addConnectionListener(history);
        if(webSocket) {
            ((NotifyingSailConnection) engineConnection.getSailConnection()).addConnectionListener(webSocketHandler);
            ((NotifyingSailConnection) handlerConnection.getSailConnection()).addConnectionListener(webSocketHandler);
        }

        InteractionHistory interactions = new InteractionHistory();

        engine = new SimulationEngine(server.getURI().toString(), engineConnection, history, interactions, resultFile);

        // TODO have a handler thread pool (see org.eclipse.jetty.util.thread.QueuedThreadPool)
        // TODO manage RepositoryConnections for all individual threads
        // note: server's base URI is set only after server starts
        switch(protocol) {
            case "ldp":
                graphHandler = new LDPHandler(server.getURI(), handlerConnection);
                break;
            default: 
                graphHandler = new GraphStoreHandler(server.getURI(), handlerConnection);
                break;
        }
        graphHandler.addGraphListener(interactions);

        log.info("Server started on port {}. Waiting for command on resource {}...", port, SIMULATION_RESOURCE_TARGET);
    }

    public SimulationEngine getSimulationEngine() {
        return engine;
    }

    public void terminate() throws Exception {
        server.stop();
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        staticHandler.handle(target, baseRequest, request, response);
        if (baseRequest.isHandled()) return; // static resource was found

        switch (engine.getCurrentState()) {
            case EMPTY_STORE:
                // only recognizes PUT /sim
                // TODO check validity of RDF payload with shape
                if (request.getMethod().equals("PUT") && target.equals(SIMULATION_RESOURCE_TARGET)) {
                    graphHandler.handle(target, baseRequest, request, response);
                    engine.callTransition();
                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    baseRequest.setHandled(true);
                }
                break;

            case RUNNING:
                graphHandler.handle(target, baseRequest, request, response);
                break;

            default:
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                baseRequest.setHandled(true);
                break;
        }
    }

}
