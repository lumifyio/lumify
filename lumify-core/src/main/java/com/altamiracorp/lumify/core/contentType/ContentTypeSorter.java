package com.altamiracorp.lumify.core.contentType;

import org.apache.commons.compress.archivers.ArchiveEntry;

import java.io.InputStream;

public interface ContentTypeSorter {
    String getQueueNameFromMimeType(String mimeType);

    String getQueueNameFromArchiveEntry(ArchiveEntry entry, InputStream inputStream);
}
