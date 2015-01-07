package io.lumify.palantir.dataImport;

import io.lumify.palantir.dataImport.model.PtObjectType;

public class PtObjectTypeImporter extends PtRowImporterBase<PtObjectType> {

    protected PtObjectTypeImporter(DataImporter dataImporter) {
        super(dataImporter, PtObjectType.class);
    }

    @Override
    protected void processRow(PtObjectType row) {
        getDataImporter().getObjectTypes().put(row.getType(), row);
        getDataImporter().writeOntologyXmlFile(row.getUri(), row.getConfig());
    }

    @Override
    protected String getSql() {
        return "select * from {namespace}.PT_OBJECT_TYPE";
    }
}
