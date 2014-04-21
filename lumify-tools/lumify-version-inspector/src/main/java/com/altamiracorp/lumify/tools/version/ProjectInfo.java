package com.altamiracorp.lumify.tools.version;

import java.util.Map;

/**
 * Metadata about a Lumify project.
 */
public final class ProjectInfo {
    /**
     * The project name key.
     */
    private static final String PROJECT_NAME = "project.name";

    /**
     * The project group ID key.
     */
    private static final String PROJECT_GROUP_ID = "project.groupId";

    /**
     * The project artifact ID key.
     */
    private static final String PROJECT_ARTIFACT_ID = "project.artifactId";

    /**
     * The project version key.
     */
    private static final String PROJECT_VERSION = "project.version";

    /**
     * The project SCM revision key.
     */
    private static final String PROJECT_SCM_REVISION = "project.scm.revision";

    /**
     * The source of this project.
     */
    private final String source;

    /**
     * The name.
     */
    private final String name;

    /**
     * The group ID.
     */
    private final String groupId;

    /**
     * The artifact ID.
     */
    private final String artifactId;

    /**
     * The version.
     */
    private final String version;

    /**
     * The SCM revision.
     */
    private final String scmRevision;

    /**
     * The build info.
     */
    private final BuildInfo buildInfo;

    /**
     * Create a new ProjectInfo.
     * @param src the source of this ProjectInfo, e.g. lumify-core-1.0-SNAPSHOT.jar
     * @param props the properties map
     */
    public ProjectInfo(final String src, final Map<String, String> props) {
        source = src;
        name = props.get(PROJECT_NAME);
        groupId = props.get(PROJECT_GROUP_ID);
        artifactId = props.get(PROJECT_ARTIFACT_ID);
        version = props.get(PROJECT_VERSION);
        scmRevision = props.get(PROJECT_SCM_REVISION);
        buildInfo = new BuildInfo(props);
    }

    public String getName() {
        return name != null && !name.trim().isEmpty() ? name : artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getScmRevision() {
        return scmRevision;
    }

    public BuildInfo getBuildInfo() {
        return buildInfo;
    }

    public String getSource() {
        return source;
    }

    public String getCoordinates() {
        return String.format("%s:%s:%s", groupId, artifactId, version);
    }

    @Override
    public String toString() {
        return String.format("%s (%s) [%s]", name, getCoordinates(), scmRevision);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 11 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 11 * hash + (this.groupId != null ? this.groupId.hashCode() : 0);
        hash = 11 * hash + (this.artifactId != null ? this.artifactId.hashCode() : 0);
        hash = 11 * hash + (this.version != null ? this.version.hashCode() : 0);
        hash = 11 * hash + (this.scmRevision != null ? this.scmRevision.hashCode() : 0);
        hash = 11 * hash + this.buildInfo.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ProjectInfo other = (ProjectInfo) obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        if ((this.groupId == null) ? (other.groupId != null) : !this.groupId.equals(other.groupId)) {
            return false;
        }
        if ((this.artifactId == null) ? (other.artifactId != null) : !this.artifactId.equals(other.artifactId)) {
            return false;
        }
        if ((this.version == null) ? (other.version != null) : !this.version.equals(other.version)) {
            return false;
        }
        if ((this.scmRevision == null) ? (other.scmRevision != null) : !this.scmRevision.equals(other.scmRevision)) {
            return false;
        }
        if (!this.buildInfo.equals(other.buildInfo)) {
            return false;
        }
        return true;
    }
}
