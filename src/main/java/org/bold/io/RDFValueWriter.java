package org.bold.io;

import org.bold.sim.SimulationEngine;
import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.common.lang.service.FileFormatServiceRegistry;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

/**
 * RDF writer to include in RDF4J's RIO module that implements the original semantics of the rdf:value property.
 *
 * If a resource includes a triple of the form {@code res rdf:value val}, where {@code val} is a typed literal, the
 * RDFValueWriter considers {@code val} to be an alternative representation of the resource and writes it to output.
 * Instances of RDFValueWriter must be passed an IRI for {@code res} as setting.
 *
 * See https://lists.w3.org/Archives/Public/semantic-web/2010Jul/0252.html
 */
public class RDFValueWriter implements RDFWriter {

    public class UnknownSubjectResourceException extends IllegalStateException {

        public UnknownSubjectResourceException() {
            super("The resource to use as subject of the rdf:value triple is unknown. It should be passed as setting");
        }

    }

    private final Logger log = LoggerFactory.getLogger(RDFValueWriter.class);

    private final Writer baseWriter;

    private final RDFFormat format;

    private final IRI datatypeIRI;

    private IRI resourceIRI = null;

    public RDFValueWriter(Writer baseWriter, RDFFormat format) {
        this.baseWriter = baseWriter;
        this.format = format;
        datatypeIRI = format.getStandardURI();
    }

    @Override
    public RDFFormat getRDFFormat() {
        return format;
    }

    @Override
    public RDFWriter setWriterConfig(WriterConfig config) {
        return null;
    }

    @Override
    public WriterConfig getWriterConfig() {
        return null;
    }

    @Override
    public Collection<RioSetting<?>> getSupportedSettings() {
        return null;
    }

    @Override
    public <T> RDFWriter set(RioSetting<T> setting, T value) {
        return null;
    }

    @Override
    public void startRDF() throws RDFHandlerException {
        // ignore
    }

    @Override
    public void endRDF() throws RDFHandlerException {
        try {
            baseWriter.close();
        } catch (IOException e) {
            log.error("Couldn't close stream while writing literal representation", e);
        }
    }

    @Override
    public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
        // ignore
    }

    @Override
    public void handleStatement(Statement st) throws RDFHandlerException {
        if (st.getPredicate().equals(RDF.VALUE)) {
            // TODO and if datatype is the registered RDF format
            // TODO and if subject is as provided (same for content type)
            String representation = st.getObject().stringValue();
            try {
                // TODO if several representations are available, choose one
                baseWriter.write(representation);
            } catch (IOException e) {
                log.error("Couldn't write literal representation to stream", e);
            }
        }
    }

    @Override
    public void handleComment(String comment) throws RDFHandlerException {
        // ignore
    }

}
