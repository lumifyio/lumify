package com.altamiracorp.lumify.ucd;

import com.altamiracorp.lumify.core.user.ModelAuthorizations;
import com.altamiracorp.bigtable.model.accumulo.AccumuloBaseInputFormat;
import com.altamiracorp.lumify.model.AccumuloModelAuthorizations;
import com.altamiracorp.lumify.core.model.artifact.Artifact;
import com.altamiracorp.lumify.core.model.artifact.ArtifactBuilder;
import org.apache.accumulo.core.client.mapreduce.AccumuloInputFormat;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.hadoop.mapreduce.Job;

public class AccumuloArtifactInputFormat extends AccumuloBaseInputFormat<Artifact, ArtifactBuilder> {
    private ArtifactBuilder artifactBuilder = new ArtifactBuilder();

    public static void init(Job job, String username, String password, ModelAuthorizations modelAuthorizations, String zookeeperInstanceName, String zookeeperServerNames) {
        Authorizations authorizations = ((AccumuloModelAuthorizations) modelAuthorizations).getAuthorizations();
        AccumuloInputFormat.setZooKeeperInstance(job.getConfiguration(), zookeeperInstanceName, zookeeperServerNames);
        AccumuloInputFormat.setInputInfo(job.getConfiguration(), username, password.getBytes(), Artifact.TABLE_NAME, authorizations);
    }

    @Override
    public ArtifactBuilder getBuilder() {
        return artifactBuilder;
    }
}
