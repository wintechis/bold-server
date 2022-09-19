package org.bold.sparql;

import org.bold.sim.Vocabulary;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

import java.lang.Math;
import java.util.Random;

/**
 * Returns a normally distributed value with mu = 0 and sigma = 1
 */
public class NormalRandomFunction implements Function {

    private final Random prng = new Random(6987736584800324l); // TODO as sim parameter

    public void setSeed(long seed) {
        prng.setSeed(seed);
    }

    @Override
    public String getURI() {
        return Vocabulary.NS + "normalRand";
    }

    @Override
    public Value evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
        double u1 = prng.nextDouble();
        double u2 = prng.nextDouble();
        double z1 = Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
        return Vocabulary.VALUE_FACTORY.createLiteral(z1);
    }

}
