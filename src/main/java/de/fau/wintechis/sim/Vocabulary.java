package de.fau.wintechis.sim;

import de.fau.wintechis.sparql.ComputeExponentialCDFFunction;
import de.fau.wintechis.sparql.ComputeNormalCDFFunction;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.function.FunctionRegistry;

public class Vocabulary {

    public static final ValueFactory VALUE_FACTORY = SimpleValueFactory.getInstance();

    public static final String NS = "http://ti.rw.fau.de/sim#";

    public static final IRI CURRENT_TIME = VALUE_FACTORY.createIRI(NS + "currentTime");

    public static void registerFunctions() {
        FunctionRegistry.getInstance().add(new ComputeExponentialCDFFunction());
        FunctionRegistry.getInstance().add(new ComputeNormalCDFFunction());
    }

}
