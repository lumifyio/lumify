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
    protected void processRow(DataToSequenceFile dataToSequenceFile, T row, SequenceFile.Writer outputFile) throws IOException {
        super.processRow(dataToSequenceFile, row, outputFile);
        writeOntologyXmlFile(dataToSequenceFile, row.getUri(), row.getConfig());
    }
}
