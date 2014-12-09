package io.lumify.palantir.dataImport;

import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.palantir.dataImport.model.PtMediaAndValue;
import io.lumify.palantir.dataImport.util.TryInflaterInputStream;
import org.securegraph.Vertex;
import org.securegraph.VertexBuilder;
import org.securegraph.property.StreamingPropertyValue;

import java.io.InputStream;

import static com.google.common.base.Preconditions.checkNotNull;

public class PtMediaAndValueImporter extends PtRowImporterBase<PtMediaAndValue> {
    public PtMediaAndValueImporter(DataImporter dataImporter) {
        super(dataImporter, PtMediaAndValue.class);
    }

    @Override
    protected void beforeProcessRows() {
        super.beforeProcessRows();
        getDataImporter().getGraph().flush();
    }

    @Override
    protected void processRow(PtMediaAndValue row) throws Exception {
        String propertyKey = getDataImporter().getIdPrefix() + row.getId();
        InputStream in = new TryInflaterInputStream(row.getContents());
        try {
            StreamingPropertyValue propertyValue = new StreamingPropertyValue(in, byte[].class);
            propertyValue.store(true);
            propertyValue.searchIndex(false);

            String title = row.getTitle();
            if (title == null) {
                title = "";
            }

            VertexBuilder vertexBuilder = getDataImporter().getGraph().prepareVertex(getMediaId(row), getDataImporter().getVisibility());
            LumifyProperties.RAW.addPropertyValue(vertexBuilder, propertyKey, propertyValue, getDataImporter().getVisibility());
            LumifyProperties.TITLE.setProperty(vertexBuilder, title, getDataImporter().getVisibility());
            Vertex mediaVertex = vertexBuilder.save(getDataImporter().getAuthorizations());

            String sourceVertexId = getObjectVertexId(row.getLinkObjectId());

            String edgeId = getEdgeId(row);
            String edgeLabel = getEdgeLabel(row);
            getDataImporter().getGraph().addEdge(edgeId, sourceVertexId, mediaVertex.getId(), edgeLabel, getDataImporter().getVisibility(), getDataImporter().getAuthorizations());
        } finally {
            in.close();
        }
    }

    private String getEdgeLabel(PtMediaAndValue row) {
        return getDataImporter().getHasMediaConceptTypeIri();
    }

    private String getEdgeId(PtMediaAndValue row) {
        return getDataImporter().getIdPrefix() + "_media_" + row.getLinkObjectId() + "_to_" + row.getId();
    }

    private String getMediaId(PtMediaAndValue row) {
        return getDataImporter().getIdPrefix() + "_media_" + row.getId();
    }

    @Override
    protected String getSql() {
        return "select m.ID" +
                ", m.REALM_ID" +
                ", m.LINK_OBJECT_ID" +
                ", m.DATA_EVENT_ID" +
                ", m.ORIGIN_DATA_EVENT_ID" +
                ", m.DELETED" +
                ", m.MEDIA_VALUE_ID" +
                ", m.CROSS_RESOLUTION_ID" +
                ", m.ACCESS_CONTROL_LIST_ID" +
                ", m.CREATED_BY" +
                ", m.TIME_CREATED" +
                ", m.LAST_MODIFIED_BY" +
                ", m.LAST_MODIFIED" +
                ", m.TITLE" +
                ", m.DESCRIPTION" +
                ", m.LINK_TYPE" +
                ", mv.TYPE" +
                ", mv.CONTENTS" +
                ", mv.CONTENTS_HASH" +
                " FROM {namespace}.PT_MEDIA m, {namespace}.PT_MEDIA_VALUE mv" +
                " WHERE " +
                "  m.MEDIA_VALUE_ID = mv.ID" +
                "  AND m.LINK_OBJECT_ID != -1";
    }
}
