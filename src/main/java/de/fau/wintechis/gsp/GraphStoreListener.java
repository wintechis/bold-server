package de.fau.wintechis.gsp;

import org.eclipse.rdf4j.model.IRI;

public interface GraphStoreListener {

    void graphRetrieved(IRI graphName, Long opTime);

    void graphUpdated(IRI graphName, Long opTime);

    void graphDeleted(IRI graphName, Long opTime);

    void graphExtended(IRI graphName, Long opTime);

}
