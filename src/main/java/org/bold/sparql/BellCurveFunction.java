package org.bold.sparql;

import org.bold.sim.Vocabulary;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

import java.lang.Math;
import java.util.Random;

/**
 * Returns a normally distributed value with mu = 0 and sigma = 1
 */
public class BellCurveFunction implements Function {

    @Override
    public String getURI() {
        return Vocabulary.NS + "bellCurve";
    }

    @Override
    public Value evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
        Literal x = (Literal) args[0];
        Literal mu = (Literal) args[1];
        Literal sigma = (Literal) args[2];
        double result = Math.exp(-Math.pow((x.doubleValue() - mu.doubleValue()), 2) / (2 * Math.pow(sigma.doubleValue(), 2)));
        return Vocabulary.VALUE_FACTORY.createLiteral(result);
    }

}
