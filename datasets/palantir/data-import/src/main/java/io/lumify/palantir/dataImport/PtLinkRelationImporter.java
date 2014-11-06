package io.lumify.palantir.dataImport;

import io.lumify.palantir.dataImport.model.PtLinkRelation;

public class PtLinkRelationImporter extends PtRowImporterBase<PtLinkRelation> {
    private StringBuilder xml;

    protected PtLinkRelationImporter(DataImporter dataImporter) {
        super(dataImporter, PtLinkRelation.class);
    }

    @Override
    protected void beforeProcessRows() {
        super.beforeProcessRows();

        xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" ?>\n");
        xml.append("<link_relations>\n");
    }

    @Override
    protected void processRow(PtLinkRelation row) {
        xml.append("  <link_relation_config>\n");
        xml.append("    <tableType1>").append(row.getTableTypeId1()).append("</tableType1>\n");
        xml.append("    <uri1>").append(row.getUri1()).append("</uri1>\n");
        xml.append("    <tableType2>").append(row.getTableTypeId2()).append("</tableType2>\n");
        xml.append("    <uri2>").append(row.getUri2()).append("</uri2>\n");
        xml.append("    <linkUri>").append(row.getLinkUri()).append("</linkUri>\n");
        xml.append("    <linkStatus>").append(row.getLinkStatus()).append("</linkStatus>\n");
        xml.append("    <hidden>").append(row.isHidden()).append("</hidden>\n");
        xml.append("  </link_relation_config>\n");
    }

    @Override
    protected void afterProcessRows() {
        super.afterProcessRows();
        xml.append("</link_relations>\n");

        getDataImporter().writeFile("pt_link_relation.xml", xml.toString());
    }

    @Override
    protected String getSql() {
        return "select * from {namespace}.PT_LINK_RELATION";
    }
}
