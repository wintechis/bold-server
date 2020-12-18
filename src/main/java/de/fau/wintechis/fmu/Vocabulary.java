package de.fau.wintechis.fmu;

import de.fau.wintechis.sparql.*;
import no.ntnu.ihb.fmi4j.modeldescription.variables.Causality;
import no.ntnu.ihb.fmi4j.modeldescription.variables.Variability;
import no.ntnu.ihb.fmi4j.modeldescription.variables.VariableType;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.function.FunctionRegistry;

public class Vocabulary {

    public static final ValueFactory VALUE_FACTORY = SimpleValueFactory.getInstance();

    public static final String NS = "http://ti.rw.fau.de/fmu#";

    public static final IRI SCALAR_VARIABLE = VALUE_FACTORY.createIRI(NS, "ScalarVariable");

    public static final IRI BOOLEAN_VARIABLE = VALUE_FACTORY.createIRI(NS, "BooleanVariable");

    public static final IRI REAL_VARIABLE = VALUE_FACTORY.createIRI(NS, "RealVariable");

    public static final IRI INTEGER_VARIABLE = VALUE_FACTORY.createIRI(NS, "IntegerVariable");

    public static final IRI STRING_VARIABLE = VALUE_FACTORY.createIRI(NS, "StringVariable");

    public static final IRI ENUMERATION_VARIABLE = VALUE_FACTORY.createIRI(NS, "EnumerationVariable");

    public static final IRI VARIABILITY = VALUE_FACTORY.createIRI(NS, "variability");

    public static final IRI CONTINUOUS = VALUE_FACTORY.createIRI(NS, "continuous");

    public static final IRI DISCRETE = VALUE_FACTORY.createIRI(NS, "discrete");

    public static final IRI TUNABLE = VALUE_FACTORY.createIRI(NS, "tunable");

    public static final IRI FIXED = VALUE_FACTORY.createIRI(NS, "fixed");

    public static final IRI CAUSALITY = VALUE_FACTORY.createIRI(NS, "causality");

    public static final IRI LOCAL = VALUE_FACTORY.createIRI(NS, "local");

    public static final IRI PARAMETER = VALUE_FACTORY.createIRI(NS, "parameter");

    public static final IRI OUTPUT = VALUE_FACTORY.createIRI(NS, "output");

    public static IRI asIRI(VariableType type) {
        switch (type) {
            case BOOLEAN: return BOOLEAN_VARIABLE;
            case REAL: return REAL_VARIABLE;
            case INTEGER: return INTEGER_VARIABLE;
            case STRING: return STRING_VARIABLE;
            case ENUMERATION: return ENUMERATION_VARIABLE;
            default: return null;
        }
    }

    public static IRI asIRI(Causality causality) {
        switch (causality) {
            case LOCAL: return LOCAL;
            case PARAMETER: return PARAMETER;
            case OUTPUT: return OUTPUT;
            default: return null; // TODO finish
        }
    }

    public static IRI asIRI(Variability variability) {
        switch (variability) {
            case CONTINUOUS: return CONTINUOUS;
            case DISCRETE: return DISCRETE;
            case CONSTANT: return PARAMETER;
            case TUNABLE: return TUNABLE;
            case FIXED: return FIXED;
            default: return null; // TODO finish
        }
    }

}
