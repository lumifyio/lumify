package io.lumify.palantir.service;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import io.lumify.palantir.DataToSequenceFile;
import io.lumify.palantir.model.PtOntologyResource;
import org.apache.commons.codec.binary.Base64;
import org.apache.hadoop.io.SequenceFile;

import java.io.IOException;

public class PtOntologyResourceExporter extends ExporterBase<PtOntologyResource> {
    public PtOntologyResourceExporter() {
        super(PtOntologyResource.class);
    }

    @Override
    protected void processRow(Exporter.ExporterSource exporterSource, PtOntologyResource row, SequenceFile.Writer outputFile) throws IOException {
        super.processRow(exporterSource, row, outputFile);

        if (row.isDeleted()) {
            return;
        }

        String contentsBase64 = Base64.encodeBase64String(row.getContents());
        contentsBase64 = Joiner.on('\n').join(Splitter.fixedLength(76).split(contentsBase64));

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" ?>\n");
        xml.append("<ontology_resource_config>\n");
        xml.append("  <type>").append(row.getType()).append("</type>\n");
        xml.append("  <path>").append(row.getPath()).append("</path>\n");
        xml.append("  <deleted>").append(row.isDeleted()).append("</deleted>\n");
        xml.append("  <contents>").append(contentsBase64).append("</contents>\n");
        xml.append("</ontology_resource_config>\n");

        writeFile(exporterSource, DataToSequenceFile.ONTOLOGY_XML_DIR_NAME + "/image/OntologyResource" + row.getId() + ".xml", xml.toString());
    }

    @Override
    protected String getSql() {
        return "SELECT * FROM {namespace}.PT_ONTOLOGY_RESOURCE ORDER BY id";
    }
}
