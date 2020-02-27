package de.fau.wintechis.sim;

import de.fau.wintechis.sparql.ComputeExponentialCDFFunction;
import de.fau.wintechis.sparql.ComputeNormalCDFFunction;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.function.FunctionRegistry;

public class Vocabulary {

    public static final String NS = "http://ti.rw.fau.de/sim#";

    public static final Property CURRENT_TIME = ResourceFactory.createProperty(NS + "currentTime");

    public static void registerFunctions() {
        FunctionRegistry.get().put(NS + "cdf-exp", ComputeExponentialCDFFunction.class);
        FunctionRegistry.get().put(NS + "cdf-normal", ComputeNormalCDFFunction.class);
    }

}
