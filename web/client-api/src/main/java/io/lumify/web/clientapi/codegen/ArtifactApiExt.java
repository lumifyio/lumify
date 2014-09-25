package io.lumify.web.clientapi.codegen;

import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.codegen.ArtifactApi;
import io.lumify.web.clientapi.codegen.model.ArtifactImportResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ArtifactApiExt extends ArtifactApi {
    public ArtifactImportResponse importFile(String visibilitySource, String fileName, InputStream data) throws ApiException, IOException {
        File tempDir = FileUtils.getTempDirectory();
        try {
            File file = new File(tempDir, fileName);
            try {
                FileOutputStream out = new FileOutputStream(file);
                try {
                    IOUtils.copy(data, out);
                } finally {
                    out.close();
                }
                return importFile(visibilitySource, file);
            } finally {
                file.delete();
            }
        } finally {
            tempDir.delete();
        }
    }
}
