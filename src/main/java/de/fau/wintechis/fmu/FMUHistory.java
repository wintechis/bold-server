package de.fau.wintechis.fmu;

import de.fau.wintechis.sim.History;
import no.ntnu.ihb.fmi4j.Fmi4jVariableUtils;
import no.ntnu.ihb.fmi4j.SlaveInstance;
import no.ntnu.ihb.fmi4j.VariableRead;
import no.ntnu.ihb.fmi4j.importer.fmi2.Fmu;
import no.ntnu.ihb.fmi4j.modeldescription.variables.RealVariable;
import no.ntnu.ihb.fmi4j.modeldescription.variables.TypedScalarVariable;
import no.ntnu.ihb.fmi4j.modeldescription.variables.VariableType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class FMUHistory implements History {

    public static void main(String[] args) throws IOException {
        // source: https://github.com/NTNU-IHB/FMI4j/blob/f28ca207b8374835d5b81416bf26cde880286870/test-fmus/fmus/2.0/cs/20sim/4.6.4.8004/ControlledTemperature/ControlledTemperature.fmu?raw=true
        Fmu fmu = Fmu.from(new File("ControlledTemperature.fmu"));
        SlaveInstance slave = fmu.asCoSimulationFmu().newInstance();

        slave.simpleSetup();

        FileWriter w = new FileWriter(new File("simu.dat"));

        w.write("# ");
        for (TypedScalarVariable<?> v : slave.getModelVariables()) {
            w.write(v.getName() + "\t");
        }
        w.write("\n");

        double stop = 20;
        double stepSize = 1.0/100;
        while(slave.getSimulationTime() <= stop) {
            for (TypedScalarVariable v : slave.getModelVariables().getVariables()) {
                if (v.getType().equals(VariableType.REAL)) {
                    VariableRead vr = Fmi4jVariableUtils.read(v.asRealVariable(), slave);
                    w.write(vr.getValue() + "\t");
                }
            }
            w.write("\n");

            if (!slave.doStep(stepSize)) {
                break;
            }
        }

        w.close();

        slave.terminate(); //or close, try with resources is also supported
        fmu.close();
    }

    @Override
    public void timeIncremented(Long updateTime) {
        // TODO
    }
}
