package de.fau.wintechis.fmu;

import no.ntnu.ihb.fmi4j.Fmi4jVariableUtils;
import no.ntnu.ihb.fmi4j.SlaveInstance;
import no.ntnu.ihb.fmi4j.VariableRead;
import no.ntnu.ihb.fmi4j.modeldescription.variables.TypedScalarVariable;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;

public class FMU2RDF {

    public static final IRI QUANTITY_VALUE = Vocabulary.VALUE_FACTORY.createIRI("http://qudt.org/2.1/schema/qudt/quantityValue");

    public static Model getModelVariables(SlaveInstance fmu, String base) {
        Model ds = new LinkedHashModel();

        for (TypedScalarVariable v : fmu.getModelVariables().getVariables()) {
            IRI id = Vocabulary.VALUE_FACTORY.createIRI(base, v.getName());

            ds.add(id, RDF.TYPE, Vocabulary.asIRI(v.getType()), id);
            ds.add(id, Vocabulary.VARIABILITY, Vocabulary.asIRI(v.getVariability()), id);
            ds.add(id, Vocabulary.CAUSALITY, Vocabulary.asIRI(v.getCausality()), id);
        }

        return ds;
    }

    public static Model getState(SlaveInstance fmu, String base) {
        Model ds = new LinkedHashModel();

        for (TypedScalarVariable v : fmu.getModelVariables().getVariables()) {
            IRI id = Vocabulary.VALUE_FACTORY.createIRI(base, v.getName());

            Value val = getValue(v, fmu, base);
            if (val != null) ds.add(id, QUANTITY_VALUE, val, id);
        }

        return ds;
    }

    private static Value getValue(TypedScalarVariable v, SlaveInstance fmu, String base) {
        VariableRead vr = Fmi4jVariableUtils.read(v, fmu);

        switch (v.getType()) {
            case BOOLEAN: return Vocabulary.VALUE_FACTORY.createLiteral((Boolean) vr.getValue());
            case REAL: return Vocabulary.VALUE_FACTORY.createLiteral((Double) vr.getValue());
            case INTEGER: return Vocabulary.VALUE_FACTORY.createLiteral((Integer) vr.getValue());
            case STRING: return Vocabulary.VALUE_FACTORY.createLiteral((String) vr.getValue());
            case ENUMERATION: return Vocabulary.VALUE_FACTORY.createIRI(base + vr.getValue());
            default: return null;
        }
    }

}
