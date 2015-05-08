package io.lumify.core.ingest;

import java.util.Collections;
import java.util.List;
import org.securegraph.VertexBuilder;
import org.securegraph.Visibility;

import java.io.File;

public abstract class FileImportSupportingFileHandler {
    public abstract boolean isSupportingFile(File f);

    public abstract AddSupportingFilesResult addSupportingFiles(VertexBuilder vertexBuilder, File f, Visibility visibility) throws Exception;

    public abstract static class AddSupportingFilesResult {
        /**
         * Subclasses may override this method to provide a list of properties
         * that have been updated by this supporting file handler and should
         * be added to the work queue for the created vertex. The default
         * implementation returns an empty list.
         * @return the list of properties to add to the work queue for the processed vertex; should never return null
         */
        public List<String> getPropertiesToQueue() {
            return Collections.EMPTY_LIST;
        }

        public abstract void close() throws Exception;
    }
}
