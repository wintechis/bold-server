package de.fau.wintechis.sparql;

import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionBase3;

/**
 * SPARQL function to provide the Cumulative Distribution Function (CDF) value
 * of a normal distribution with parameters mu, sigma.
 *
 * (Currently approximated by a linear function...)
 *
 * CDF(x) = (x - mu) / (4 * sigma) + 1/2
 */
public class ComputeNormalCDFFunction extends FunctionBase3 {


    @Override
    public NodeValue exec(NodeValue mu, NodeValue sigma, NodeValue value) {
        double m = mu.getDouble();
        double s = sigma.getDouble();
        double v = value.getDouble();

        if (v < (m - s)) return NodeValue.makeDecimal(0);
        if (v > (m + s)) return NodeValue.makeDecimal(1);

        double cdf = (v - m) / (4 * s) + 0.5;
        return NodeValue.makeDecimal(cdf);
    }
}
