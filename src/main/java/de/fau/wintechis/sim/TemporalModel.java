package de.fau.wintechis.sim;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.sail.SailConnectionListener;

import java.util.HashSet;
import java.util.Set;

public class TemporalModel extends LinkedHashModel implements SailConnectionListener {

    private Integer count = null;

    private Resource currentOperation;

    private Set<IRI> reifiedStatements = new HashSet<>();

    TemporalModel() {
        timeIncremented();
    }

    public void timeIncremented() {
        if (count == null) {
            count = 0;
        } else {
            count++;
        }

        currentOperation = generate(count);
        Literal t = Vocabulary.VALUE_FACTORY.createLiteral(count);
        this.add(currentOperation, Vocabulary.AT_TIME, t);
    }

    @Override
    public void statementAdded(Statement st) {
        IRI iri = canonicalize(st);
        this.add(currentOperation, Vocabulary.INSERTED, iri);
    }

    @Override
    public void statementRemoved(Statement st) {
        IRI iri = canonicalize(st);
        this.add(currentOperation, Vocabulary.DELETED, iri);
    }

    private Resource generate(Integer timestamp) {
        return Vocabulary.VALUE_FACTORY.createBNode();
    }

    private IRI canonicalize(Statement st) {
        String hash = Integer.toString(st.hashCode());
        IRI iri = SimpleValueFactory.getInstance().createIRI(Vocabulary.NS, hash);

        if (!reifiedStatements.contains(iri)) {
            this.add(iri, RDF.SUBJECT, st.getSubject());
            this.add(iri, RDF.PREDICATE, st.getPredicate());
            this.add(iri, RDF.OBJECT, st.getObject());
        }

        return iri;
    }

}
