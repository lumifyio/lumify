package io.lumify.core.model.artifactThumbnails;

import com.altamiracorp.bigtable.model.Column;
import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Row;
import com.beust.jcommander.internal.Nullable;
import com.google.inject.Inject;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.user.User;
import org.securegraph.Vertex;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public class BigTableArtifactThumbnailRepository extends ArtifactThumbnailRepository {
    @Inject
    public BigTableArtifactThumbnailRepository(@Nullable final ModelSession modelSession, final OntologyRepository ontologyRepository) {
        super(modelSession, ontologyRepository);
    }

    @Override
    public BigTableArtifactThumbnail fromRow(Row row) {
        BigTableArtifactThumbnail artifactThumbnail = new BigTableArtifactThumbnail(row.getRowKey());
        Collection<ColumnFamily> families = row.getColumnFamilies();
        for (ColumnFamily columnFamily : families) {
            String columnFamilyName = columnFamily.getColumnFamilyName();
            if (columnFamilyName.equals(BigTableArtifactThumbnailMetadata.NAME)) {
                Collection<Column> columns = columnFamily.getColumns();
                artifactThumbnail.addColumnFamily(new BigTableArtifactThumbnailMetadata().addColumns(columns));
            } else {
                artifactThumbnail.addColumnFamily(columnFamily);
            }
        }
        return artifactThumbnail;
    }

    @Override
    public Row toRow(BigTableArtifactThumbnail artifactThumbnail) {
        return artifactThumbnail;
    }

    @Override
    public String getTableName() {
        return BigTableArtifactThumbnail.TABLE_NAME;
    }

    @Override
    public ArtifactThumbnail getThumbnail(Object artifactVertexId, String thumbnailType, int width, int height, User user) {
        BigTableArtifactThumbnailRowKey rowKey = new BigTableArtifactThumbnailRowKey(artifactVertexId, thumbnailType, width, height);
        BigTableArtifactThumbnail thumbnail = findByRowKey(rowKey.toString(), user.getModelUserContext());
        if (thumbnail != null) {
            BigTableArtifactThumbnailMetadata metadata = thumbnail.getMetadata();
            return new ArtifactThumbnail(metadata.getData(), metadata.getType(), metadata.getFormat());
        }
        return null;
    }

    @Override
    public byte[] getThumbnailData(Object artifactVertexId, String thumbnailType, int width, int height, User user) {
        ArtifactThumbnail artifactThumbnail = getThumbnail(artifactVertexId, thumbnailType, width, height, user);
        if (artifactThumbnail == null) {
            return null;
        }
        return artifactThumbnail.getThumbnailData();
    }

    @Override
    public ArtifactThumbnail createThumbnail(Vertex artifactVertex, String thumbnailType, InputStream in, int[] boundaryDims, User user) throws IOException {
        ArtifactThumbnail thumbnail = super.generateThumbnail(artifactVertex, in, boundaryDims);
        saveThumbnail(artifactVertex, thumbnailType, boundaryDims, thumbnail.getThumbnailData(), thumbnail.getType(), thumbnail.getFormat());
        return thumbnail;
    }

    private void saveThumbnail(Object artifactVertexId, String thumbnailType, int[] boundaryDims, byte[] bytes, int type, String format) {
        BigTableArtifactThumbnailRowKey artifactThumbnailRowKey = new BigTableArtifactThumbnailRowKey(artifactVertexId, thumbnailType, boundaryDims[0], boundaryDims[1]);
        BigTableArtifactThumbnail artifactThumbnail = new BigTableArtifactThumbnail(artifactThumbnailRowKey);
        artifactThumbnail.getMetadata().setData(bytes);
        artifactThumbnail.getMetadata().setType(type);
        artifactThumbnail.getMetadata().setFormat(format);
        save(artifactThumbnail);
    }
}
