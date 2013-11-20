package com.altamiracorp.lumify.core.fs;

import com.altamiracorp.lumify.core.model.SaveFileResults;
import com.altamiracorp.lumify.core.user.User;

import java.io.InputStream;

public abstract class FileSystemSession {

    public abstract SaveFileResults saveFile (InputStream in);

    public abstract InputStream loadFile (String path);

    public abstract long getFileLength (String path);
}
