package com.altamiracorp.lumify.core.version;

public interface VersionServiceMXBean {
    Long getUnixBuildTime();

    String getVersion();

    String getScmBuildNumber();
}
