package de.fau.wintechis.gsp;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;

/**
 * Basic implementation of the SPARQL Graph Store protocol, giving
 * RESTful access to named graphs in an RDF dataset.
 */
public class GraphStoreHandler extends AbstractHandler {

    private final RDFConnection connection;

    public GraphStoreHandler(Dataset dataset) {
        connection = RDFConnectionFactory.connect(dataset);
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String graphName = getServer().getURI().resolve(target).toString();

        System.out.println(graphName);

        // TODO proper conneg (with translation to/from Model instances)
        Lang accept = Lang.TURTLE;
        Lang contentType = Lang.TURTLE;

        Model requestModel, responseModel;
        switch (baseRequest.getMethod()) {
            case "GET":
                responseModel = connection.fetch(graphName);
                response.setHeader("Content-Type", contentType.getContentType().getContentTypeStr());
                RDFDataMgr.write(response.getOutputStream(), responseModel, contentType);
                response.setStatus(200);
                // TODO return 404 if no such graph
                break;

            case "PUT":
                requestModel = ModelFactory.createDefaultModel();
                RDFDataMgr.read(requestModel, request.getInputStream(), accept);
                connection.put(graphName, requestModel);
                response.setStatus(204);
                // TODO or 201 if graph created
                break;

            case "POST":
                requestModel = ModelFactory.createDefaultModel();
                RDFDataMgr.read(requestModel, request.getInputStream(), accept);
                connection.load(graphName, requestModel);
                response.setStatus(204);
                // TODO or 201 if graph created
                break;

            case "DELETE":
                connection.delete(graphName);
                response.setStatus(204);
                // TODO return 404 if no such graph
                break;

            default:
                response.sendError(405);
                break;
        }

        baseRequest.setHandled(true);
    }

}
