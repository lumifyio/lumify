package io.lumify.core.ingest;

import org.securegraph.VertexBuilder;
import org.securegraph.Visibility;

import java.io.File;

public abstract class FileImportSupportingFileHandler {
    public abstract boolean isSupportingFile(File f);

    public abstract AddSupportingFilesResult addSupportingFiles(VertexBuilder vertexBuilder, File f, Visibility visibility) throws Exception;

    public abstract static class AddSupportingFilesResult {
        public abstract void close() throws Exception;
    }
}
