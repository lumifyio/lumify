package com.altamiracorp.lumify.storm;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.tuple.Tuple;
import com.altamiracorp.lumify.core.bootstrap.InjectHelper;
import com.altamiracorp.lumify.core.ingest.*;
import com.altamiracorp.lumify.core.model.detectedObjects.DetectedObjectRepository;
import com.altamiracorp.lumify.core.model.videoFrames.VideoFrameRepository;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.core.util.ThreadedInputStreamProcess;
import com.altamiracorp.lumify.core.util.ThreadedTeeInputStreamWorker;
import com.altamiracorp.lumify.storm.file.FileMetadata;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.Visibility;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public abstract class BaseArtifactProcessingBolt extends BaseFileProcessingBolt {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(BaseArtifactProcessingBolt.class);

    private ThreadedInputStreamProcess<ArtifactExtractedInfo, AdditionalArtifactWorkData> threadedInputStreamProcess;
    private VideoFrameRepository videoFrameRepository;
    private DetectedObjectRepository detectedObjectRepository;

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        super.prepare(stormConf, context, collector);
        try {
            mkdir("/tmp");
            mkdir("/lumify");
            mkdir("/lumify/artifacts");
            mkdir("/lumify/artifacts/text");
            mkdir("/lumify/artifacts/raw");
        } catch (IOException e) {
            collector.reportError(e);
        }

        List<ThreadedTeeInputStreamWorker<ArtifactExtractedInfo, AdditionalArtifactWorkData>> workers = Lists.newArrayList();

        ServiceLoader services = getServiceLoader();
        for (Object service : services) {
            LOGGER.info("Adding service %s to %s", service.getClass().getName(), getClass().getName());
            InjectHelper.inject(service);
            TextExtractionWorkerPrepareData data = new TextExtractionWorkerPrepareData(stormConf, getUser(), getHdfsFileSystem(), InjectHelper.getInjector());
            try {
                ((TextExtractionWorker) service).prepare(data);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to prepare " + service.getClass(), ex);
            }
            workers.add((ThreadedTeeInputStreamWorker<ArtifactExtractedInfo, AdditionalArtifactWorkData>) service);
        }

        setThreadedInputStreamProcess(new ThreadedInputStreamProcess<ArtifactExtractedInfo, AdditionalArtifactWorkData>(getThreadPrefix(), workers));
    }

    protected abstract String getThreadPrefix();

    protected abstract ServiceLoader getServiceLoader();

    @Override
    public void cleanup() {
        threadedInputStreamProcess.stop();
        super.cleanup();
    }

    protected Vertex processFile(Tuple input) throws Exception {
        FileMetadata fileMetadata = getFileMetadata(input);
        File archiveTempDir = null;
        InputStream in;
        int rawSize;
        LOGGER.info("Processing file: %s (mimeType: %s)", fileMetadata.getFileName(), fileMetadata.getMimeType());

        String fileName = fileMetadata.getFileNameWithoutDateSuffix();
        ArtifactExtractedInfo artifactExtractedInfo = new ArtifactExtractedInfo();
        if (fileMetadata.getRaw() != null) {
            in = fileMetadata.getRaw();
        } else if (isArchive(fileName)) {
            archiveTempDir = extractArchive(fileMetadata);
            File primaryFile = getPrimaryFileFromArchive(archiveTempDir);
            in = openFile(primaryFile.getAbsolutePath());
            fileMetadata.setPrimaryFileFromArchive(primaryFile);
            fileMetadata.setMimeType(getContentTypeExtractor().extract(new FileInputStream(primaryFile), FilenameUtils.getExtension(primaryFile.getAbsoluteFile().toString())));
        } else {
            in = openFile(fileMetadata.getFileName());
        }
        if (fileMetadata.getTitle() != null) {
            artifactExtractedInfo.setTitle(fileMetadata.getTitle());
        }
        if (fileMetadata.getSource() != null) {
            artifactExtractedInfo.setSource(fileMetadata.getSource());
        }
        artifactExtractedInfo.setFileExtension(FilenameUtils.getExtension(fileMetadata.getFileName()));
        artifactExtractedInfo.setMimeType(fileMetadata.getMimeType());
        rawSize = in.available();
        if(rawSize <= ArtifactExtractedInfo.MAX_SIZE_OF_INLINE_FILE) {
            in = new ByteArrayInputStream(IOUtils.toByteArray(in));
        }

        runWorkers(in, fileMetadata, artifactExtractedInfo, archiveTempDir);

        if (rawSize > ArtifactExtractedInfo.MAX_SIZE_OF_INLINE_FILE) {
            String newRawArtifactHdfsPath = moveRawFile(fileMetadata.getFileName(), artifactExtractedInfo.getRowKey(), fileMetadata.getRaw());
            artifactExtractedInfo.setRawHdfsPath(newRawArtifactHdfsPath);
        } else {
            in.reset();
            artifactExtractedInfo.setRaw(IOUtils.toByteArray(in));
        }

        if (artifactExtractedInfo.getMp4HdfsFilePath() != null) {
            String newTextPath = moveTempMp4File(artifactExtractedInfo.getMp4HdfsFilePath(), artifactExtractedInfo.getRowKey());
            artifactExtractedInfo.setMp4HdfsFilePath(newTextPath);
        }
        if (artifactExtractedInfo.getWebMHdfsFilePath() != null) {
            String newTextPath = moveTempWebMFile(artifactExtractedInfo.getWebMHdfsFilePath(), artifactExtractedInfo.getRowKey());
            artifactExtractedInfo.setWebMHdfsFilePath(newTextPath);
        }
        if (artifactExtractedInfo.getAudioHdfsPath() != null) {
            String newTextPath = moveTempAudioFile(artifactExtractedInfo.getAudioHdfsPath(), artifactExtractedInfo.getRowKey());
            artifactExtractedInfo.setAudioHdfsPath(newTextPath);
        }
        if (artifactExtractedInfo.getPosterFrameHdfsPath() != null) {
            String newTextPath = moveTempPosterFrameFile(artifactExtractedInfo.getPosterFrameHdfsPath(), artifactExtractedInfo.getRowKey());
            artifactExtractedInfo.setPosterFrameHdfsPath(newTextPath);
        }
        if (artifactExtractedInfo.getProcess() == null) {
            artifactExtractedInfo.setProcess("");
        }

        Vertex graphVertex = saveArtifact(artifactExtractedInfo);

        if (artifactExtractedInfo.getVideoFrames() != null) {
            saveVideoFrames(graphVertex.getId(), artifactExtractedInfo.getVideoFrames());
        }

        if (artifactExtractedInfo.getDetectedObjects() != null) {
            saveDetectedObjects (graphVertex.getId(), artifactExtractedInfo.getDetectedObjects());
        }

        if (archiveTempDir != null) {
            FileUtils.deleteDirectory(archiveTempDir);
            LOGGER.debug("Deleted temporary directory holding archive content");
        }

        graph.flush();

        LOGGER.debug("Created graph vertex [%s] for %s", graphVertex.getId(), artifactExtractedInfo.getTitle());
        return graphVertex;
    }

    protected File getPrimaryFileFromArchive(File archiveTempDir) {
        throw new RuntimeException("Not implemented for class " + getClass());
    }

    private void saveVideoFrames(Object artifactVertexId, List<ArtifactExtractedInfo.VideoFrame> videoFrames) throws IOException {
        for (ArtifactExtractedInfo.VideoFrame videoFrame : videoFrames) {
            saveVideoFrame(artifactVertexId, videoFrame);
        }
    }

    private void saveVideoFrame(Object artifactVertexId, ArtifactExtractedInfo.VideoFrame videoFrame) throws IOException {
        InputStream in = getHdfsFileSystem().open(new Path(videoFrame.getHdfsPath()));
        try {
            videoFrameRepository.saveVideoFrame(artifactVertexId, in, videoFrame.getFrameStartTime(), getUser());
        } finally {
            in.close();
        }
        getHdfsFileSystem().delete(new Path(videoFrame.getHdfsPath()), false);
    }

    private void saveDetectedObjects (Object artifactVertexId, List<ArtifactDetectedObject> detectedObjects) {
        for (ArtifactDetectedObject detectedObject : detectedObjects) {
            saveDetectedObject(artifactVertexId, detectedObject);
        }
    }

    private void saveDetectedObject (Object artifactVertexId, ArtifactDetectedObject detectedObject) {
        detectedObjectRepository.saveDetectedObject(artifactVertexId, detectedObject.getId(), detectedObject.getConcept(),
                detectedObject.getX1(), detectedObject.getY1(), detectedObject.getX2(), detectedObject.getY2(), false, new Visibility(""));
    }

    protected void runWorkers(InputStream in, FileMetadata fileMetadata, ArtifactExtractedInfo artifactExtractedInfo, File archiveTempDir) throws Exception {
        AdditionalArtifactWorkData additionalDocumentWorkData = new AdditionalArtifactWorkData();
        try {
            additionalDocumentWorkData.setFileName(fileMetadata.getFileName());
            additionalDocumentWorkData.setMimeType(fileMetadata.getMimeType());
            additionalDocumentWorkData.setHdfsFileSystem(getHdfsFileSystem());
            additionalDocumentWorkData.setArchiveTempDir(archiveTempDir);
            if (isLocalFileRequired()) {
                if (fileMetadata.getPrimaryFileFromArchive() != null) {
                    additionalDocumentWorkData.setLocalFileName(fileMetadata.getPrimaryFileFromArchive().getAbsolutePath());
                } else {
                    File localFile = copyFileToLocalFile(in);
                    in = new FileInputStream(localFile);
                    additionalDocumentWorkData.setLocalFileName(localFile.getAbsolutePath());
                }
            }
            List<ThreadedTeeInputStreamWorker.WorkResult<ArtifactExtractedInfo>> results = threadedInputStreamProcess.doWork(in, additionalDocumentWorkData);
            mergeResults(artifactExtractedInfo, results);
        } finally {
            in.close();
            if (additionalDocumentWorkData.getLocalFileName() != null) {
                //noinspection ResultOfMethodCallIgnored
                new File(additionalDocumentWorkData.getLocalFileName()).delete();
            }
        }
    }

    private File copyFileToLocalFile(InputStream in) throws IOException {
        File localFile = File.createTempFile("fileProcessing", "");
        LOGGER.debug("Copying file locally for processing: %s", localFile);
        OutputStream localFileOut = new FileOutputStream(localFile);
        try {
            long numberOfBytesCopied = IOUtils.copyLarge(in, localFileOut);
            LOGGER.debug("Copied %d to file %s", numberOfBytesCopied, localFile);
        } finally {
            localFileOut.close();
            in.close();
        }
        return localFile;
    }

    protected boolean isLocalFileRequired() {
        return false;
    }

    protected void mergeResults(ArtifactExtractedInfo artifactExtractedInfo, List<ThreadedTeeInputStreamWorker.WorkResult<ArtifactExtractedInfo>> results) throws Exception {
        for (ThreadedTeeInputStreamWorker.WorkResult<ArtifactExtractedInfo> result : results) {
            if (result.getError() != null) {
                throw result.getError();
            }
            artifactExtractedInfo.mergeFrom(result.getResult());
        }
    }

    protected String moveRawFile(String fileName, String rowKey, InputStream raw) throws IOException {
        String rawArtifactHdfsPath = "/lumify/artifacts/raw/" + rowKey;
        if (getHdfsFileSystem().exists(new Path(rawArtifactHdfsPath))) {
            getHdfsFileSystem().delete(new Path(fileName), false);
        } else {
            if (raw != null) {
                FSDataOutputStream rawFile = getHdfsFileSystem().create(new Path(rawArtifactHdfsPath));
                try {
                    rawFile.write(IOUtils.toByteArray(raw));
                } finally {
                    rawFile.close();
                }
            } else {
                moveFile(fileName, rawArtifactHdfsPath);
            }
        }
        return rawArtifactHdfsPath;
    }

    protected String moveTempTextFile(String fileName, String rowKey) throws IOException {
        return moveTempFile("/lumify/artifacts/text/", fileName, rowKey);
    }

    protected String moveTempWebMFile(String fileName, String rowKey) throws IOException {
        return moveTempFile("/lumify/artifacts/video/webm/", fileName, rowKey);
    }

    protected String moveTempAudioFile(String fileName, String rowKey) throws IOException {
        return moveTempFile("/lumify/artifacts/video/audio/", fileName, rowKey);
    }

    protected String moveTempMp4File(String fileName, String rowKey) throws IOException {
        return moveTempFile("/lumify/artifacts/video/mp4/", fileName, rowKey);
    }

    protected String moveTempPosterFrameFile(String fileName, String rowKey) throws IOException {
        return moveTempFile("/lumify/artifacts/video/posterFrame/", fileName, rowKey);
    }

    private String moveTempFile(String path, String fileName, String rowKey) throws IOException {
        String newPath = path + rowKey;
        LOGGER.info("Moving file %s -> %s", fileName, newPath);
        getHdfsFileSystem().delete(new Path(newPath), false);
        getHdfsFileSystem().rename(new Path(fileName), new Path(newPath));
        return newPath;
    }

    protected void setThreadedInputStreamProcess(ThreadedInputStreamProcess<ArtifactExtractedInfo, AdditionalArtifactWorkData> threadedInputStreamProcess) {
        this.threadedInputStreamProcess = threadedInputStreamProcess;
    }

    @Override
    public void safeExecute(Tuple input) throws Exception {
        Vertex graphVertex = processFile(input);
        onAfterGraphVertexCreated(graphVertex);
    }

    protected void onAfterGraphVertexCreated(Vertex graphVertex) {
        workQueueRepository.pushText(graphVertex.getId().toString());
    }

    @Inject
    public void setVideoFrameRepository(VideoFrameRepository videoFrameRepository) {
        this.videoFrameRepository = videoFrameRepository;
    }

    @Inject
    public void setDetectedObjectRepository(DetectedObjectRepository detectedObjectRepository) { this.detectedObjectRepository = detectedObjectRepository; }
}
