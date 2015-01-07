package io.lumify.palantir.dataImport;

import io.lumify.palantir.dataImport.model.PtImageInfo;

public class PtImageInfoImporter extends PtRowImporterBase<PtImageInfo> {
    private StringBuilder xml;

    protected PtImageInfoImporter(DataImporter dataImporter) {
        super(dataImporter, PtImageInfo.class);
    }

    @Override
    protected void beforeProcessRows() {
        super.beforeProcessRows();
        xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" ?>\n");
        xml.append("<image_infos>\n");
    }

    @Override
    protected void processRow(PtImageInfo row) {
        xml.append("  <image_info_config>\n");
        xml.append("    <name>").append(row.getName()).append("</name>\n");
        xml.append("    <uri>").append(row.getUri()).append("</uri>\n");
        xml.append("    <description>").append(row.getDescription()).append("</description>\n");
        xml.append("    <path>").append(row.getPath()).append("</path>\n");
        xml.append("  </image_info_config>\n");
    }

    @Override
    protected void afterProcessRows() {
        super.afterProcessRows();
        xml.append("</image_infos>\n");

        getDataImporter().writeFile("pt_image_info.xml", xml.toString());
    }

    @Override
    protected String getSql() {
        return "select * from {namespace}.PT_IMAGE_INFO";
    }
}
