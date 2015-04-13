package io.lumify.palantir.service;

import io.lumify.palantir.DataToSequenceFile;
import io.lumify.palantir.model.PtOntologyType;
import org.apache.hadoop.io.SequenceFile;

import java.io.IOException;

public abstract class OntologyTypeExporterBase<T extends PtOntologyType> extends ExporterBase<T> {
    protected OntologyTypeExporterBase(Class<T> ptClass) {
        super(ptClass);
    }

    @Override
    protected void processRow(Exporter.ExporterSource exporterSource, T row, SequenceFile.Writer outputFile) throws IOException {
        super.processRow(exporterSource, row, outputFile);
        writeOntologyXmlFile(exporterSource, row.getUri(), row.getConfig());
    }
}
