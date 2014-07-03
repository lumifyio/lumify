package io.lumify.tikaMimeType;

import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.ingest.graphProperty.MimeTypeGraphPropertyWorker;

import java.io.InputStream;

public class TikaMimeTypeGraphPropertyWorker extends MimeTypeGraphPropertyWorker {
    private TikaMimeTypeMapper mimeTypeMapper;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        mimeTypeMapper = new TikaMimeTypeMapper();
    }

    public String getMimeType(InputStream in, String fileName) throws Exception {
        String mimeType = mimeTypeMapper.guessMimeType(in, fileName);
        if (mimeType == null) {
            return null;
        }
        return mimeType;
    }
}
