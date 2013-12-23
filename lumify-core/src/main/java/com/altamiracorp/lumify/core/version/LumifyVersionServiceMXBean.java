package com.altamiracorp.lumify.core.version;

public interface LumifyVersionServiceMXBean {
    Long getUnixBuildTime();

    String getVersion();

    String getScmBuildNumber();
}
