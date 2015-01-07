package io.lumify.palantir.dataImport;

import io.lumify.palantir.dataImport.model.PtNodeDisplayType;

public class PtNodeDisplayTypeImporter extends PtRowImporterBase<PtNodeDisplayType> {

    protected PtNodeDisplayTypeImporter(DataImporter dataImporter) {
        super(dataImporter, PtNodeDisplayType.class);
    }

    @Override
    protected void processRow(PtNodeDisplayType row) {
        getDataImporter().getNodeDisplayTypes().put(row.getId(), row);
        getDataImporter().writeOntologyXmlFile(row.getUri(), row.getConfig());
    }

    @Override
    protected String getSql() {
        return "select * from {namespace}.PT_NODE_DISPLAY_TYPE";
    }
}
