package io.lumify.palantir.dataImport;

import io.lumify.core.exception.LumifyException;
import io.lumify.palantir.dataImport.model.PtLinkType;
import io.lumify.palantir.dataImport.model.PtObjectObject;

public class PtObjectObjectImporter extends PtRowImporterBase<PtObjectObject> {
    protected PtObjectObjectImporter(final DataImporter dataImporter) {
        super(dataImporter, PtObjectObject.class);
    }

    @Override
    protected void beforeProcessRows() {
        super.beforeProcessRows();
        getDataImporter().getGraph().flush();
    }

    @Override
    protected void processRow(PtObjectObject row) throws Exception {
        String sourceVertexId = getObjectVertexId(row.getParentObjectId());
        String destVertexId = getObjectVertexId(row.getChildObjectId());

        PtLinkType ptLinkType = getDataImporter().getLinkTypes().get(row.getType());
        if (ptLinkType == null) {
            throw new LumifyException("Could not find link type: " + row.getType());
        }
        String linkTypeUri = getLinkTypeUri(ptLinkType.getUri());

        String edgeId = getEdgeId(row);
        getDataImporter().getGraph().addEdge(edgeId, sourceVertexId, destVertexId, linkTypeUri, getDataImporter().getVisibility(), getDataImporter().getAuthorizations());
    }

    protected String getEdgeId(PtObjectObject ptObjectObject) {
        return getDataImporter().getIdPrefix() + ptObjectObject.getLinkId();
    }

    protected String getLinkTypeUri(String uri) {
        return getDataImporter().getOwlPrefix() + uri;
    }

    @Override
    protected String getSql() {
        // order by parent_object_id to improve cache hits
        return "select * from {namespace}.PT_OBJECT_OBJECT ORDER BY parent_object_id";
    }
}
