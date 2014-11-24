package io.lumify.palantir.dataImport;

import io.lumify.palantir.dataImport.model.PtPropertyType;

public class PtPropertyTypeImporter extends PtRowImporterBase<PtPropertyType> {
    protected PtPropertyTypeImporter(DataImporter dataImporter) {
        super(dataImporter, PtPropertyType.class);
    }

    @Override
    protected void processRow(PtPropertyType row) {
        getDataImporter().getPropertyTypes().put(row.getType(), row);
        getDataImporter().writeOntologyXmlFile(row.getUri(), row.getConfig());
    }

    @Override
    protected String getSql() {
        return "select * from {namespace}.PT_PROPERTY_TYPE";
    }
}
