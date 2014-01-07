package com.altamiracorp.lumify.core.fs;

import com.altamiracorp.lumify.core.model.SaveFileResults;

import java.io.InputStream;

public interface FileSystemSession {
    void saveFile(String path, InputStream in);
    
    SaveFileResults saveFile (InputStream in);

    InputStream loadFile (String path);

    long getFileLength (String path);
}
