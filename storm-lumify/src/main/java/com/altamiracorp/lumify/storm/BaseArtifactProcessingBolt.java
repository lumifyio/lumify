package com.altamiracorp.lumify.storm;


import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.tuple.Tuple;
import com.altamiracorp.lumify.core.contentTypeExtraction.ContentTypeExtractor;
import com.altamiracorp.lumify.core.ingest.AdditionalArtifactWorkData;
import com.altamiracorp.lumify.core.ingest.ArtifactExtractedInfo;
import com.altamiracorp.lumify.core.ingest.TextExtractionWorker;
import com.altamiracorp.lumify.core.ingest.TextExtractionWorkerPrepareData;
import com.altamiracorp.lumify.core.model.artifact.Artifact;
import com.altamiracorp.lumify.core.model.artifact.ArtifactRowKey;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.videoFrames.VideoFrameRepository;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.lumify.core.util.ThreadedInputStreamProcess;
import com.altamiracorp.lumify.core.util.ThreadedTeeInputStreamWorker;
import com.altamiracorp.lumify.storm.file.FileMetadata;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.inject.Inject;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.Path;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public abstract class BaseArtifactProcessingBolt extends BaseLumifyBolt {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseArtifactProcessingBolt.class);
    private ThreadedInputStreamProcess<ArtifactExtractedInfo, AdditionalArtifactWorkData> threadedInputStreamProcess;
    private ContentTypeExtractor contentTypeExtractor;
    private VideoFrameRepository videoFrameRepository;
    protected WorkQueueRepository workQueueRepository;

    public BaseArtifactProcessingBolt() {
        LOGGER.toString();
    }

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
            LOGGER.info(String.format("Adding service %s to %s", service.getClass().getName(), getClass().getName()));
            inject(service);
            TextExtractionWorkerPrepareData data = new TextExtractionWorkerPrepareData(stormConf, getUser(), getHdfsFileSystem(), getInjector());
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

    protected GraphVertex processFile(Tuple input) throws Exception {
        FileMetadata fileMetadata = getFileMetadata(input);
        File archiveTempDir = null;
        InputStream in;
        LOGGER.info(String.format("Processing file: %s (mimeType: %s)", fileMetadata.getFileName(), fileMetadata.getMimeType()));

        ArtifactExtractedInfo artifactExtractedInfo = new ArtifactExtractedInfo();
        if (isArchive(fileMetadata.getFileName())) {
            archiveTempDir = extractArchive(fileMetadata);
            File primaryFile = getPrimaryFileFromArchive(archiveTempDir);
            in = getInputStream(primaryFile.getAbsolutePath(), artifactExtractedInfo);
            fileMetadata.setPrimaryFileFromArchive(primaryFile);
            fileMetadata.setMimeType(contentTypeExtractor.extract(new FileInputStream(primaryFile), FilenameUtils.getExtension(primaryFile.getAbsoluteFile().toString())));
        } else {
            in = getInputStream(fileMetadata.getFileName(), artifactExtractedInfo);
        }
        artifactExtractedInfo.setTitle(getFilenameWithoutDateTime(fileMetadata));
        artifactExtractedInfo.setFileExtension(FilenameUtils.getExtension(fileMetadata.getFileName()));
        artifactExtractedInfo.setMimeType(fileMetadata.getMimeType());

        runWorkers(in, fileMetadata, artifactExtractedInfo, archiveTempDir);

        String newRawArtifactHdfsPath = moveRawFile(fileMetadata.getFileName(), artifactExtractedInfo.getRowKey());
        artifactExtractedInfo.setRawHdfsPath(newRawArtifactHdfsPath);

        if (artifactExtractedInfo.getTextRowKey() != null && artifactExtractedInfo.getTextHdfsPath() != null) {
            String newTextPath = moveTempTextFile(artifactExtractedInfo.getTextHdfsPath(), artifactExtractedInfo.getRowKey());
            artifactExtractedInfo.setTextHdfsPath(newTextPath);
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
        if (artifactExtractedInfo.getVideoFrames() != null) {
            saveVideoFrames(new ArtifactRowKey(artifactExtractedInfo.getRowKey()), artifactExtractedInfo.getVideoFrames());
        }

        GraphVertex graphVertex = saveArtifact(artifactExtractedInfo);

        if (archiveTempDir != null) {
            FileUtils.deleteDirectory(archiveTempDir);
            LOGGER.debug("Deleted temporary directory holding archive content");
        }
        LOGGER.debug("Created graph vertex [" + graphVertex.getId() + "] for " + artifactExtractedInfo.getTitle());
        return graphVertex;
    }

    private String getFilenameWithoutDateTime(FileMetadata fileMetadata) {
        String fileName = FilenameUtils.getName(fileMetadata.getFileName());
        int dateTimeSeparator = fileName.lastIndexOf("__");
        if (dateTimeSeparator > 0) {
            fileName = fileName.substring(0, dateTimeSeparator);
        }
        return fileName;
    }

    protected File getPrimaryFileFromArchive(File archiveTempDir) {
        throw new RuntimeException("Not implemented for class " + getClass());
    }

    protected File extractArchive(FileMetadata fileMetadata) throws Exception {
        File tempDir = Files.createTempDir();
        LOGGER.debug("Extracting " + fileMetadata.getFileName() + " to " + tempDir);
        InputStream in = getInputStream(fileMetadata.getFileName(), null);
        try {
            ArchiveInputStream input = new ArchiveStreamFactory().createArchiveInputStream(new BufferedInputStream(in));
            try {
                ArchiveEntry entry;
                while ((entry = input.getNextEntry()) != null) {
                    File outputFile = new File(tempDir, entry.getName());
                    OutputStream out = new FileOutputStream(outputFile);
                    try {
                        long numberOfBytesExtracted = IOUtils.copyLarge(input, out);
                        LOGGER.debug("Extracted (" + numberOfBytesExtracted + " bytes) to " + outputFile.getAbsolutePath());
                    } finally {
                        out.close();
                    }
                }
            } finally {
                input.close();
            }
        } finally {
            in.close();
        }
        return tempDir;
    }

    private void saveVideoFrames(ArtifactRowKey artifactRowKey, List<ArtifactExtractedInfo.VideoFrame> videoFrames) throws IOException {
        for (ArtifactExtractedInfo.VideoFrame videoFrame : videoFrames) {
            saveVideoFrame(artifactRowKey, videoFrame);
        }
    }

    private void saveVideoFrame(ArtifactRowKey artifactRowKey, ArtifactExtractedInfo.VideoFrame videoFrame) throws IOException {
        InputStream in = getHdfsFileSystem().open(new Path(videoFrame.getHdfsPath()));
        try {
            videoFrameRepository.saveVideoFrame(artifactRowKey, in, videoFrame.getFrameStartTime(), getUser());
        } finally {
            in.close();
        }
        getHdfsFileSystem().delete(new Path(videoFrame.getHdfsPath()), false);
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
        LOGGER.debug("Copying file locally for processing: " + localFile);
        OutputStream localFileOut = new FileOutputStream(localFile);
        try {
            long numberOfBytesCopied = IOUtils.copyLarge(in, localFileOut);
            LOGGER.debug("Copied " + numberOfBytesCopied + " to file " + localFile);
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

    protected FileMetadata getFileMetadata(Tuple input) throws Exception {
        String fileName = input.getString(0);
        if (fileName == null || fileName.length() == 0) {
            throw new RuntimeException("Invalid item on the queue.");
        }
        String mimeType = null;

        if (fileName.startsWith("{")) {
            JSONObject json = getJsonFromTuple(input);
            fileName = json.optString("fileName");
            mimeType = json.optString("mimeType");
            if (fileName == null || fileName.length() == 0) {
                throw new RuntimeException("Expected 'fileName' in JSON document but got.\n" + json.toString());
            }
        }
        if (mimeType == null) {
            mimeType = getMimeType(fileName);
        }

        return new FileMetadata(fileName, mimeType);
    }

    protected InputStream getInputStream(String fileName, ArtifactExtractedInfo artifactExtractedInfo) throws Exception {
        InputStream in;
        if (getFileSize(fileName) < Artifact.MAX_SIZE_OF_INLINE_FILE) {
            InputStream rawIn = openFile(fileName);
            byte[] data;
            try {
                data = IOUtils.toByteArray(rawIn);
                if (artifactExtractedInfo != null) {
                    artifactExtractedInfo.setRaw(data);
                }
            } finally {
                rawIn.close();
            }
            in = new ByteArrayInputStream(data);
        } else {
            in = openFile(fileName);
        }
        return in;
    }

    private String getMimeType(String fileName) throws Exception {
        InputStream in = getInputStream(fileName, null);
        return contentTypeExtractor.extract(in, FilenameUtils.getExtension(fileName));
    }


    protected String moveRawFile(String fileName, String rowKey) throws IOException {
        String rawArtifactHdfsPath = "/lumify/artifacts/raw/" + rowKey;
        if (getHdfsFileSystem().exists(new Path(rawArtifactHdfsPath))) {
            getHdfsFileSystem().delete(new Path(fileName), false);
        } else {
            moveFile(fileName, rawArtifactHdfsPath);
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
        LOGGER.info("Moving file " + fileName + " -> " + newPath);
        getHdfsFileSystem().delete(new Path(newPath), false);
        getHdfsFileSystem().rename(new Path(fileName), new Path(newPath));
        return newPath;
    }

    protected void setThreadedInputStreamProcess(ThreadedInputStreamProcess<ArtifactExtractedInfo, AdditionalArtifactWorkData> threadedInputStreamProcess) {
        this.threadedInputStreamProcess = threadedInputStreamProcess;
    }

    @Override
    public void safeExecute(Tuple input) throws Exception {
        GraphVertex graphVertex = processFile(input);
        onAfterGraphVertexCreated(graphVertex);
    }

    protected void onAfterGraphVertexCreated(GraphVertex graphVertex) {
        workQueueRepository.pushText(graphVertex.getId());
    }

    @Inject
    public void setContentTypeExtractor(ContentTypeExtractor contentTypeExtractor) {
        this.contentTypeExtractor = contentTypeExtractor;
    }

    @Inject
    public void setVideoFrameRepository(VideoFrameRepository videoFrameRepository) {
        this.videoFrameRepository = videoFrameRepository;
    }

    @Inject
    public void setWorkQueueRepository(WorkQueueRepository workQueueRepository) {
        this.workQueueRepository = workQueueRepository;
    }
}
