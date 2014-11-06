package io.lumify.palantir.dataImport;

import io.lumify.palantir.dataImport.model.PtLinkType;

public class PtLinkTypeImporter extends PtRowImporterBase<PtLinkType> {

    protected PtLinkTypeImporter(DataImporter dataImporter) {
        super(dataImporter, PtLinkType.class);
    }

    @Override
    protected void processRow(PtLinkType row) {
        getDataImporter().getLinkTypes().put(row.getType(), row);
        getDataImporter().writeOntologyXmlFile(row.getUri(), row.getConfig());
    }

    @Override
    protected String getSql() {
        return "select * from {namespace}.PT_LINK_TYPE";
    }
}
