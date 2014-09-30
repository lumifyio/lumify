package io.lumify.web.clientapi.codegen;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;
import io.lumify.web.clientapi.codegen.model.ArtifactImportResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

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

    public ArtifactImportResponse importFiles(FileForImport... files) throws ApiException, IOException {
        Object postBody = null;
        // verify required params are set
        if (files == null || files.length == 0) {
            throw new ApiException(400, "missing required params");
        }
        try {
            // create path and map variables
            String path = "/artifact/import".replaceAll("\\{format\\}", "json");

            // query params
            Map<String, String> queryParams = new HashMap<String, String>();
            Map<String, String> headerParams = new HashMap<String, String>();
            Map<String, String> formParams = new HashMap<String, String>();

            String[] contentTypes = {
                    "multipart/form-data"};

            String contentType = contentTypes.length > 0 ? contentTypes[0] : "application/json";

            FormDataMultiPart mp = new FormDataMultiPart();
            for (FileForImport fileForImport : files) {
                mp.field("visibilitySource", fileForImport.getVisibilitySource(), MediaType.MULTIPART_FORM_DATA_TYPE);
                FormDataContentDisposition dispo = FormDataContentDisposition
                        .name("file")
                        .fileName(fileForImport.getFileName())
                        .size(fileForImport.getFile().length())
                        .build();
                FormDataBodyPart bodyPart = new FormDataBodyPart(dispo, fileForImport.getFile(), MediaType.MULTIPART_FORM_DATA_TYPE);
                mp.bodyPart(bodyPart);
            }
            postBody = mp;

            try {
                String response = apiInvoker.invokeAPI(basePath, path, "POST", queryParams, postBody, headerParams, formParams, contentType);
                if (response != null) {
                    return (ArtifactImportResponse) ApiInvoker.deserialize(response, "", ArtifactImportResponse.class);
                } else {
                    return null;
                }
            } catch (ApiException ex) {
                if (ex.getCode() == 404) {
                    return null;
                } else {
                    throw ex;
                }
            }
        } finally {
            for (FileForImport fileForImport : files) {
                fileForImport.deleteTempFiles();
            }
        }
    }

    public InputStream getRaw(String graphVertexId) throws IOException, ApiException {
        return getRaw(graphVertexId, true, false, null);
    }

    public InputStream getRawForPlayback(String graphVertexId, String type) throws IOException, ApiException {
        return getRaw(graphVertexId, false, true, type);
    }

    private InputStream getRaw(String graphVertexId, boolean download, boolean playback, String type) throws ApiException, IOException {
        Map<String, String> queryParams = new HashMap<String, String>();
        Map<String, String> headerParams = new HashMap<String, String>();
        queryParams.put("graphVertexId", graphVertexId);
        if (download) {
            queryParams.put("download", "true");
        }
        if (playback) {
            queryParams.put("playback", "true");
        }
        if (type != null) {
            queryParams.put("type", type);
        }
        return apiInvoker.getBinary(basePath, "/artifact/raw", queryParams, headerParams);
    }

    public InputStream getThumbnail(String graphVertexId, Integer width) throws ApiException, IOException {
        Map<String, String> queryParams = new HashMap<String, String>();
        Map<String, String> headerParams = new HashMap<String, String>();
        queryParams.put("graphVertexId", graphVertexId);
        if (width != null) {
            queryParams.put("width", width.toString());
        }
        return apiInvoker.getBinary(basePath, "/artifact/thumbnail", queryParams, headerParams);
    }

    public InputStream getPosterFrame(String graphVertexId, Integer width) throws ApiException, IOException {
        Map<String, String> queryParams = new HashMap<String, String>();
        Map<String, String> headerParams = new HashMap<String, String>();
        queryParams.put("graphVertexId", graphVertexId);
        if (width != null) {
            queryParams.put("width", width.toString());
        }
        return apiInvoker.getBinary(basePath, "/artifact/poster-frame", queryParams, headerParams);
    }

    public InputStream getVideoPreview(String graphVertexId, Integer width) throws ApiException, IOException {
        Map<String, String> queryParams = new HashMap<String, String>();
        Map<String, String> headerParams = new HashMap<String, String>();
        queryParams.put("graphVertexId", graphVertexId);
        if (width != null) {
            queryParams.put("width", width.toString());
        }
        return apiInvoker.getBinary(basePath, "/artifact/video-preview", queryParams, headerParams);
    }

    public static class FileForImport {
        private final String visibilitySource;
        private final String fileName;
        private final InputStream data;
        private File file;

        public FileForImport(String visibilitySource, String fileName, InputStream data) {
            this.visibilitySource = visibilitySource;
            this.fileName = fileName;
            this.data = data;
        }

        public String getVisibilitySource() {
            return visibilitySource;
        }

        public String getFileName() {
            return fileName;
        }

        public File getFile() throws IOException {
            if (file == null) {
                File tempDir = FileUtils.getTempDirectory();
                file = new File(tempDir, getFileName());
                FileOutputStream out = new FileOutputStream(file);
                try {
                    IOUtils.copy(data, out);
                } finally {
                    out.close();
                }
            }
            return file;
        }

        public void deleteTempFiles() {
            if (file != null) {
                file.delete();
                file.getParentFile().delete();
            }
        }
    }
}
