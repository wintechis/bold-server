package de.fau.wintechis.gsp;

import org.eclipse.rdf4j.model.IRI;

public interface GraphStoreListener {

    void graphRetrieved(IRI graphName);

    void graphUpdated(IRI graphName);

    void graphDeleted(IRI graphName);

    void graphExtended(IRI graphName);

}
