package de.fau.wintechis.gsp;

import de.fau.wintechis.sim.Vocabulary;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.Rio;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;

/**
 * Basic implementation of the SPARQL Graph Store protocol, giving
 * RESTful access to named graphs in an RDF dataset.
 */
public class GraphStoreHandler extends AbstractHandler {

    /**
     * Note: a better implementation would take the running server's base instead but it comes with several complications.
     */
    public static final String BASE_URI_STRING = "http://ti.rw.fau.de/resources/";

    private static final URI BASE_URI = URI.create(BASE_URI_STRING);

    private final RepositoryConnection connection;

    public GraphStoreHandler(Repository repo) {
        connection = repo.getConnection();
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        IRI graphName = Vocabulary.VALUE_FACTORY.createIRI(BASE_URI.resolve(target).toString()); // direct addressing

        // TODO proper conneg (with translation to/from Model instances)
        RDFFormat accept = RDFFormat.TURTLE;
        RDFFormat contentType = RDFFormat.TURTLE;

        switch (baseRequest.getMethod()) {
            case "GET":
                response.setHeader("Content-Type", contentType.getDefaultMIMEType());
                RDFHandler writer = Rio.createWriter(accept, response.getOutputStream());
                connection.export(writer, graphName);
                response.setStatus(200);
                // TODO return 404 if no such graph
                break;

            case "PUT":
                // TODO do both operations in a single transaction
                connection.clear(graphName);
                connection.add(request.getInputStream(), baseRequest.getRequestURI(), contentType, graphName);
                response.setStatus(204);
                // TODO or 201 if graph created
                break;

            case "POST":
                connection.add(request.getInputStream(), baseRequest.getRequestURI(), contentType, graphName);
                response.setStatus(204);
                // TODO or 201 if graph created
                break;

            case "DELETE":
                connection.clear(graphName);
                response.setStatus(204);
                // TODO return 404 if no such graph
                break;

            default:
                response.setStatus(405);
                break;
        }

        baseRequest.setHandled(true);
    }

}
