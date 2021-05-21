package org.bold.http;

import org.eclipse.rdf4j.model.IRI;

public interface GraphListener {

    void graphRetrieved(IRI graphName, Long opTime);

    void graphReplaced(IRI graphName, Long opTime);

    void graphDeleted(IRI graphName, Long opTime);

    void graphExtended(IRI graphName, Long opTime);

}
