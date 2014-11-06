package io.lumify.palantir.dataImport;

import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.palantir.dataImport.model.PtObject;
import io.lumify.palantir.dataImport.model.PtObjectType;
import org.securegraph.VertexBuilder;

public class PtObjectImporter extends PtRowImporterBase<PtObject> {

    protected PtObjectImporter(DataImporter dataImporter) {
        super(dataImporter, PtObject.class);
    }

    @Override
    protected void processRow(PtObject row) {
        PtObjectType ptObjectType = getDataImporter().getObjectTypes().get(row.getType());
        if (ptObjectType == null) {
            throw new LumifyException("Could not find object type: " + row.getType());
        }
        String conceptTypeUri = getConceptTypeUri(ptObjectType.getUri());

        VertexBuilder v = getDataImporter().getGraph().prepareVertex(getObjectId(row), getDataImporter().getVisibility());
        LumifyProperties.CONCEPT_TYPE.setProperty(v, conceptTypeUri, getDataImporter().getVisibility());
        v.save(getDataImporter().getAuthorizations());
    }

    protected String getObjectId(PtObject ptObject) {
        return getDataImporter().getIdPrefix() + ptObject.getObjectId();
    }

    protected String getConceptTypeUri(String uri) {
        return getDataImporter().getOwlPrefix() + uri;
    }

    @Override
    protected String getSql() {
        return "select * from {namespace}.PT_OBJECT";
    }
}
