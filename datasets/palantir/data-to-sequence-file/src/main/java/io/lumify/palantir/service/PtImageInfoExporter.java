package io.lumify.palantir.service;

import io.lumify.palantir.DataToSequenceFile;
import io.lumify.palantir.model.PtImageInfo;
import org.apache.hadoop.io.SequenceFile;

import java.io.IOException;

public class PtImageInfoExporter extends ExporterBase<PtImageInfo> {
    private StringBuilder xml;

    public PtImageInfoExporter() {
        super(PtImageInfo.class);
    }

    @Override
    protected void beforeProcessRows(Exporter.ExporterSource exporterSource) {
        super.beforeProcessRows(exporterSource);
        xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" ?>\n");
        xml.append("<image_infos>\n");
    }

    @Override
    protected void processRow(Exporter.ExporterSource exporterSource, PtImageInfo row, SequenceFile.Writer outputFile) throws IOException {
        super.processRow(exporterSource, row, outputFile);
        xml.append("  <image_info_config>\n");
        xml.append("    <name>").append(row.getName()).append("</name>\n");
        xml.append("    <uri>").append(row.getUri()).append("</uri>\n");
        xml.append("    <description>").append(row.getDescription()).append("</description>\n");
        xml.append("    <path>").append(row.getPath()).append("</path>\n");
        xml.append("  </image_info_config>\n");
    }

    @Override
    protected void afterProcessRows(Exporter.ExporterSource exporterSource) {
        super.afterProcessRows(exporterSource);
        xml.append("</image_infos>\n");

        writeOntologyXmlFile(exporterSource, "pt_image_info", xml.toString());
    }

    @Override
    protected String getSql() {
        return "SELECT * FROM {namespace}.PT_IMAGE_INFO ORDER BY id";
    }
}
