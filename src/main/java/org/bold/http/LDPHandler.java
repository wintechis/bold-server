package org.bold.http;

import org.bold.io.RDFValueFormats;
import org.bold.sim.Vocabulary;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Basic implementation of the SPARQL Graph Store protocol, giving
 * RESTful access to named graphs in an RDF dataset.
 *
 * TODO ASK queries (~ RDF shapes) to control resource handler I/O
 */
public class LDPHandler extends AbstractHandler implements GraphHandler {

    public static final RDFFormat DEFAULT_RDF_FORMAT = RDFFormat.TURTLE;

    private final URI baseURI;

    private final RepositoryConnection connection;

    private final Set<GraphListener> listeners = new HashSet<>();

    public LDPHandler(URI base, RepositoryConnection con) {
        baseURI = base;
        connection = con;
    }

    public void addGraphListener(GraphListener listener) {
        listeners.add(listener);
    }

    public void removeGraphListener(GraphListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        IRI graphName = Vocabulary.VALUE_FACTORY.createIRI(baseURI.resolve(target).toString()); // direct addressing

        boolean created = !exists(graphName);

        IRI containerType = getContainerType(graphName);
                        
        if(containerType != null) {
            response.setHeader("Link", containerType.stringValue() + "; rel=\"type\"");
        }

        // TODO use a ServletFilter instead, for processing Accept/Content-Type

        String acceptString = request.getHeader("Accept");
        RDFFormat accept = getFormatForMediaType(acceptString);

        if (accept == null) {
            response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
            baseRequest.setHandled(true);
            return;
        }

        String contentTypeString = request.getHeader("Content-Type");
        RDFFormat contentType = getFormatForMediaType(contentTypeString);

        if (contentType == null) {
            response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
            baseRequest.setHandled(true);
            return;
        }

        long before, after;

        try {
            switch (baseRequest.getMethod()) {
                case "GET":
                    if (!created) {
                        //response.setHeader("Content-Type", accept.getDefaultMIMEType());
                        response.setHeader("Content-Type", "text/turtle");

                        RDFWriter writer = Rio.createWriter(RDFFormat.TURTLE, response.getOutputStream());

                        before = System.currentTimeMillis();
                        connection.exportStatements(null, null, null, true, writer, graphName);
                        after = System.currentTimeMillis();

                        response.setStatus(HttpServletResponse.SC_OK);

                        for (GraphListener l : listeners) {
                            l.graphRetrieved(graphName, after - before);
                        }
                    } else {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    }
                    break;

                case "PUT":
                    Model model = Rio.parse(request.getInputStream(), baseRequest.getRequestURI(), contentType);
                    if(containerType != null) {
                        Set<Statement> containsSet = new HashSet<>();
                        connection.getStatements(null, LDP.CONTAINS, null, graphName).forEach(containsSet::add);

                        //Check if containment triples are untouched
                        boolean conflict = false;
                        for(Statement s : model.getStatements(null, LDP.CONTAINS, null)) {
                            Statement graphS = Vocabulary.VALUE_FACTORY.createStatement(s.getSubject(), s.getPredicate(), s.getObject(), graphName);
                            if(!containsSet.contains(graphS)) {
                                conflict = true;
                                break;
                            }
                            containsSet.remove(graphS);
                        }
                        if(conflict || containsSet.size() > 0) {
                            response.setStatus(HttpServletResponse.SC_CONFLICT);
                            break;
                        }
                    }
                    // TODO test transaction isolation (and rollback if necessary)
                    before = System.currentTimeMillis();
                    connection.begin();
                    connection.clear(graphName);
                    connection.add(model, graphName);
                    connection.commit();
                    after = System.currentTimeMillis();

                    response.setStatus(created ? HttpServletResponse.SC_CREATED : HttpServletResponse.SC_NO_CONTENT);

                    for (GraphListener l : listeners) {
                        l.graphReplaced(graphName, after - before);
                    }
                    break;

                case "POST":
                    if(containerType != null) {
                        IRI containedResource = Vocabulary.VALUE_FACTORY.createIRI(graphName.stringValue() + "/", UUID.randomUUID().toString());

                        before = System.currentTimeMillis();
                        connection.add(request.getInputStream(), containedResource.stringValue(), contentType, containedResource);
                        after = System.currentTimeMillis();
                        for (GraphListener l : listeners) {
                            l.graphExtended(graphName, after - before);
                        }

                        before = System.currentTimeMillis();
                        connection.add(graphName, LDP.CONTAINS, containedResource, graphName);
                        after = System.currentTimeMillis();
                        for (GraphListener l : listeners) {
                            l.graphExtended(graphName, after - before);
                        }

                        response.setStatus(HttpServletResponse.SC_CREATED);
                        response.setHeader("Location", containedResource.stringValue());
                    } else {
                        before = System.currentTimeMillis();
                        connection.add(request.getInputStream(), baseRequest.getRequestURI(), contentType, graphName);
                        after = System.currentTimeMillis();

                        response.setStatus(created ? HttpServletResponse.SC_CREATED : HttpServletResponse.SC_NO_CONTENT);

                        for (GraphListener l : listeners) {
                            l.graphExtended(graphName, after - before);
                        }
                    }
                    break;

                case "DELETE":
                    if (!created) {
                        //Collect containment triples for every context
                        Map<Resource,List<Statement>> containers = connection.getStatements(null, LDP.CONTAINS, graphName).stream().collect(Collectors.groupingBy(Statement::getContext));
                        for(Resource container : containers.keySet()) {
                            before = System.currentTimeMillis();
                            connection.remove(containers.get(container));
                            after = System.currentTimeMillis();
                            for (GraphListener l : listeners) {
                                l.graphReplaced(graphName, after - before);
                            }
                        }

                        before = System.currentTimeMillis();
                        connection.clear(graphName);
                        after = System.currentTimeMillis();

                        response.setStatus(HttpServletResponse.SC_NO_CONTENT);

                        for (GraphListener l : listeners) {
                            l.graphDeleted(graphName, after - before);
                        }
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

    private IRI getContainerType(IRI graphName) {
        return (IRI) connection.getStatements(graphName, RDF.TYPE, null, false, graphName).stream().map(
            s -> s.getObject()
        ).filter(
            o -> o.equals(LDP.BASIC_CONTAINER) || o.equals(LDP.DIRECT_CONTAINER) || o.equals(LDP.INDIRECT_CONTAINER)
        ).findAny().orElse(null);
    }

    private RDFFormat getFormatForMediaType(String mediaType) {
        if (mediaType == null || mediaType.equals("*/*")) return DEFAULT_RDF_FORMAT;

        // TODO parse conneg header if more complex value

        Optional<RDFFormat> opt = Rio.getParserFormatForMIMEType(mediaType);
        if (opt.isPresent()) return opt.get();

        return RDFValueFormats.getFormatForMediaType(mediaType);
    }

}
