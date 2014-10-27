package io.lumify.themoviedb;

import io.lumify.core.exception.LumifyException;
import io.lumify.core.mapreduce.LumifyElementMapperBase;
import io.lumify.core.model.properties.LumifyProperties;
import org.apache.hadoop.io.BytesWritable;
import org.securegraph.Vertex;
import org.securegraph.VertexBuilder;
import org.securegraph.Visibility;
import org.securegraph.accumulo.AccumuloAuthorizations;
import org.securegraph.property.StreamingPropertyValue;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ImportImgMRMapper extends LumifyElementMapperBase<SequenceFileKey, BytesWritable> {
    public static final String MULTI_VALUE_KEY = ImportJsonMR.class.getName();
    public static final String SOURCE = "TheMovieDb.org";
    private Visibility visibility;
    private AccumuloAuthorizations authorizations;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        this.visibility = new Visibility("");
        this.authorizations = new AccumuloAuthorizations();
    }

    @Override
    protected void safeMap(SequenceFileKey key, BytesWritable value, Context context) throws Exception {
        String conceptType;
        String sourceVertexId;
        String edgeLabel;

        context.setStatus(key.getRecordType() + ":" + key.getId());

        switch (key.getRecordType()) {
            case PERSON:
                conceptType = TheMovieDbOntology.CONCEPT_TYPE_PROFILE_IMAGE;
                edgeLabel = TheMovieDbOntology.EDGE_LABEL_HAS_PROFILE_IMAGE;
                sourceVertexId = TheMovieDbOntology.getPersonVertexId(key.getId());
                break;
            case MOVIE:
                conceptType = TheMovieDbOntology.CONCEPT_TYPE_POSTER_IMAGE;
                edgeLabel = TheMovieDbOntology.EDGE_LABEL_HAS_POSTER_IMAGE;
                sourceVertexId = TheMovieDbOntology.getMovieVertexId(key.getId());
                break;
            case PRODUCTION_COMPANY:
                conceptType = TheMovieDbOntology.CONCEPT_TYPE_LOGO;
                edgeLabel = TheMovieDbOntology.EDGE_LABEL_HAS_LOGO;
                sourceVertexId = TheMovieDbOntology.getProductionCompanyVertexId(key.getId());
                break;
            default:
                throw new LumifyException("Invalid record type: " + key.getRecordType());
        }

        String edgeId = TheMovieDbOntology.getHasImageEdgeId(key.getId(), key.getImagePath());
        String title = key.getTitle();
        String vertexId = TheMovieDbOntology.getImageVertexId(key.getImagePath());
        VertexBuilder m = prepareVertex(vertexId, visibility);
        LumifyProperties.CONCEPT_TYPE.addPropertyValue(m, MULTI_VALUE_KEY, conceptType, visibility);
        LumifyProperties.SOURCE.addPropertyValue(m, MULTI_VALUE_KEY, SOURCE, visibility);
        StreamingPropertyValue rawValue = new StreamingPropertyValue(new ByteArrayInputStream(value.getBytes()), byte[].class);
        rawValue.store(true);
        rawValue.searchIndex(false);
        LumifyProperties.RAW.addPropertyValue(m, MULTI_VALUE_KEY, rawValue, visibility);
        LumifyProperties.TITLE.addPropertyValue(m, MULTI_VALUE_KEY, "Image of " + title, visibility);
        Vertex profileImageVertex = m.save(authorizations);

        VertexBuilder sourceVertexMutation = prepareVertex(sourceVertexId, visibility);
        LumifyProperties.ENTITY_IMAGE_VERTEX_ID.addPropertyValue(sourceVertexMutation, MULTI_VALUE_KEY, profileImageVertex.getId(), visibility);
        Vertex sourceVertex = sourceVertexMutation.save(authorizations);

        addEdge(edgeId, sourceVertex, profileImageVertex, edgeLabel, visibility, authorizations);

        context.getCounter(TheMovieDbImportCounters.IMAGES_PROCESSED).increment(1);
    }
}
