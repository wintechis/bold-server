package de.fau.wintechis.sparql;

import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionBase2;

/**
 * SPARQL function to provide the Cumulative Distribution Function (CDF) value
 * of an exponential distribution with parameter lambda.
 *
 * CDF(x) = 1 - e^{-lambda * x}
 */
public class ComputeExponentialCDFFunction extends FunctionBase2 {

    @Override
    public NodeValue exec(NodeValue lambda, NodeValue value) {
        double cdf = 1 - Math.exp(- lambda.getDouble() * value.getDouble());
        return NodeValue.makeDecimal(cdf);
    }

}
