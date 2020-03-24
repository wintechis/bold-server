package de.fau.wintechis.sparql;

import de.fau.wintechis.sim.Vocabulary;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

/**
 * This custom function is a workaround to a RDF4J bug: zero-parameter functions are called only once when retrieving
 * solutions while according to SPARQL 1.1. they should be called for every solution mapping.
 */
public class RandomFunction implements Function {

    @Override
    public String getURI() {
        return Vocabulary.NS + "rand";
    }

    @Override
    public Value evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
        return Vocabulary.VALUE_FACTORY.createLiteral(Math.random());
    }
}
