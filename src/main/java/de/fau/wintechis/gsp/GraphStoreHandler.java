package de.fau.wintechis.gsp;

import de.fau.wintechis.sim.Vocabulary;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFParseException;
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

    public static final RDFFormat DEFAULT_RDF_FORMAT = RDFFormat.TURTLE;

    private final RepositoryConnection connection;

    public GraphStoreHandler(Repository repo) {
        connection = repo.getConnection();
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        URI base = getServer().getURI();
        IRI graphName = Vocabulary.VALUE_FACTORY.createIRI(base.resolve(target).toString()); // direct addressing

        boolean created = !exists(graphName);

        // TODO send 406 Not Acceptable and 415 Unsupported Media Type if header present
        RDFFormat accept = Rio.getParserFormatForMIMEType(request.getHeader("Accept")).orElse(DEFAULT_RDF_FORMAT);
        RDFFormat contentType = Rio.getParserFormatForMIMEType(request.getHeader("Content-Type")).orElse(DEFAULT_RDF_FORMAT);

        try {
            switch (baseRequest.getMethod()) {
                case "GET":
                    if (!created) {
                        response.setHeader("Content-Type", accept.getDefaultMIMEType());
                        RDFHandler writer = Rio.createWriter(accept, response.getOutputStream());
                        connection.export(writer, graphName);
                        response.setStatus(HttpServletResponse.SC_OK);
                    } else {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    }
                    break;

                case "PUT":
                    // TODO do both operations in a single transaction
                    connection.clear(graphName);
                    connection.add(request.getInputStream(), baseRequest.getRequestURI(), contentType, graphName);
                    response.setStatus(created ? HttpServletResponse.SC_CREATED : HttpServletResponse.SC_NO_CONTENT);
                    break;

                case "POST":
                    connection.add(request.getInputStream(), baseRequest.getRequestURI(), contentType, graphName);
                    response.setStatus(created ? HttpServletResponse.SC_CREATED : HttpServletResponse.SC_NO_CONTENT);
                    break;

                case "DELETE":
                    if (!created) {
                        connection.clear(graphName);
                        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    } else {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    }
                    break;

                default:
                    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    break;
            }
        } catch (RDFParseException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } // other exceptions caught by jetty and 500 Internal Server Error returned

        baseRequest.setHandled(true);
    }

    private boolean exists(IRI graphName) {
        return connection.hasStatement(null, null, null, false, graphName);
    }

}
