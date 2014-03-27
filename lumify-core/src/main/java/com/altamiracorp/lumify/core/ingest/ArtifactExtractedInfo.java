package com.altamiracorp.lumify.core.ingest;

import com.altamiracorp.lumify.core.ingest.video.VideoTranscript;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class ArtifactExtractedInfo {
    private static final String ROW_KEY = "rowKey";
    private static final String TEXT = "text";
    private static final String TITLE = "title";
    private static final String DATE = "date";
    private static final String TEXT_HDFS_PATH = "textHdfsPath";
    private static final String ONTOLOGY_CLASS_URI = "ontologyClassUri";
    private static final String RAW = "raw";
    private static final String MP4_HDFS_PATH = "mp4HdfsPath";
    private static final String AUDIO_OGG_HDFS_PATH = "oggHdfsPath";
    private static final String AUDIO_MP4_HDFS_PATH = "audioMp4HdfsPath";
    private static final String WEBM_HDFS_PATH = "webmHdfsPath";
    private static final String DETECTED_OBJECTS = "detectedObjects";
    private static final String VIDEO_TRANSCRIPT = "videoTranscript";
    private static final String AUDIO_HDFS_PATH = "audioHdfsPath";
    private static final String POSTER_FRAME_HDFS_PATH = "posterFrameHdfsPath";
    private static final String VIDEO_DURATION = "videoDuration";
    private static final String VIDEO_FRAMES = "videoFrames";
    private static final String MAPPING_JSON = "mappingJson";
    private static final String MIME_TYPE = "mimeType";
    private static final String FILE_EXTENSION = "fileExtension";
    private static final String FILE_NAME = "fileName";
    private static final String URL = "url";
    private static final String SOURCE = "source";
    private static final String AUTHOR = "author";
    private static final String PROCESS = "process";
    private static final String RAW_HDFS_PATH = "rawHdfsPath";
    public static final int MAX_SIZE_OF_INLINE_FILE = 1 * 1024 * 1024;

    private final HashMap<String, Object> properties = new HashMap<String, Object>();

    public void mergeFrom(ArtifactExtractedInfo artifactExtractedInfo) {
        if (artifactExtractedInfo == null) {
            return;
        }
        for (Map.Entry<String, Object> prop : artifactExtractedInfo.properties.entrySet()) {
            if (prop.getKey().equals(VIDEO_TRANSCRIPT)) {
                properties.put(prop.getKey(), VideoTranscript.merge(getVideoTranscript(), (VideoTranscript) prop.getValue()));
            } else if (prop.getKey().equals(TEXT)) {
                mergeExtractedText(artifactExtractedInfo);
            } else {
                properties.put(prop.getKey(), prop.getValue());
            }
        }
    }

    public void mergeExtractedText(ArtifactExtractedInfo artifactExtractedInfo) {
        if (StringUtils.isBlank(artifactExtractedInfo.getText())) {
            return;
        }

        String mergedText;
        if (getText() == null) {
            mergedText = artifactExtractedInfo.getText();
        } else {
            StringBuilder sb = new StringBuilder(getText())
                    .append(artifactExtractedInfo.getText());
            mergedText = sb.toString();
        }

        setText(mergedText);
    }

    public void setRowKey(String rowKey) {
        properties.put(ROW_KEY, rowKey);
    }

    /**
     * Builder pattern for rowKey property.
     *
     * @param rowKey the rowKey
     * @return this
     */
    public ArtifactExtractedInfo rowKey(final String rowKey) {
        setRowKey(rowKey);
        return this;
    }

    public String getRowKey() {
        return (String) properties.get(ROW_KEY);
    }

    public String getText() {
        return (String) properties.get(TEXT);
    }

    public void setText(String text) {
        properties.put(TEXT, text);
    }

    public void setRawHdfsPath(String rawHdfsPath) {
        set(RAW_HDFS_PATH, rawHdfsPath);
    }

    /**
     * Builder pattern for rawHdfsPath property.
     *
     * @param rawHdfsPath the rawHdfsPath
     * @return this
     */
    public ArtifactExtractedInfo rawHdfsPath(final String rawHdfsPath) {
        setRawHdfsPath(rawHdfsPath);
        return this;
    }

    public String getRawHdfsPath() {
        return (String) properties.get(RAW_HDFS_PATH);
    }

    /**
     * Builder pattern for text property.
     *
     * @param text the text
     * @return this
     */
    public ArtifactExtractedInfo text(final String text) {
        setText(text);
        return this;
    }

    public void setTitle(String title) {
        properties.put(TITLE, title);
    }

    /**
     * Builder pattern for title property.
     *
     * @param title the title
     * @return this
     */
    public ArtifactExtractedInfo title(final String title) {
        setTitle(title);
        return this;
    }

    public String getTitle() {
        return (String) properties.get(TITLE);
    }

    public void setDate(Date date) {
        // ensure internal date object is not externally mutable
        Date safeClone = new Date(date.getTime());
        properties.put(DATE, safeClone);
    }

    /**
     * Builder pattern for date property.
     *
     * @param date the date
     * @return this
     */
    public ArtifactExtractedInfo date(final Date date) {
        setDate(date);
        return this;
    }

    public Date getDate() {
        Date internalDate = (Date) properties.get(DATE);
        // ensure internal date object is not externally mutable
        return internalDate != null ? new Date(internalDate.getTime()) : null;
    }

    public void set(String key, Object val) {
        properties.put(key, val);
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        for (Map.Entry<String, Object> prop : properties.entrySet()) {
            if ("raw".equals(prop.getKey())) {
                continue;
            }
            json.put(prop.getKey(), prop.getValue());
        }
        return json;
    }

    public void setTextHdfsPath(String textHdfsPath) {
        set(TEXT_HDFS_PATH, textHdfsPath);
    }

    /**
     * Builder pattern for textHdfsPath property.
     *
     * @param textHdfsPath the textHdfsPath
     * @return this
     */
    public ArtifactExtractedInfo textHdfsPath(final String textHdfsPath) {
        setTextHdfsPath(textHdfsPath);
        return this;
    }

    public String getTextHdfsPath() {
        return (String) properties.get(TEXT_HDFS_PATH);
    }

    public void setOntologyClassUri(String ontologyClassUri) {
        set(ONTOLOGY_CLASS_URI, ontologyClassUri);
    }

    /**
     * Builder pattern for ontologyClassUri property.
     *
     * @param ontologyClassUri the ontologyClassUri
     * @return this
     */
    public ArtifactExtractedInfo ontologyClassUri(final String ontologyClassUri) {
        setOntologyClassUri(ontologyClassUri);
        return this;
    }

    public String getOntologyClassUri() {
        return (String) properties.get(ONTOLOGY_CLASS_URI);
    }

    public void setRaw(byte[] raw) {
        set(RAW, raw);
    }

    /**
     * Builder pattern for raw property.
     *
     * @param raw the raw value
     * @return this
     */
    public ArtifactExtractedInfo raw(final byte[] raw) {
        setRaw(raw);
        return this;
    }

    public byte[] getRaw() {
        return (byte[]) properties.get(RAW);
    }

    public void setMp4HdfsFilePath(String mp4HdfsFilePath) {
        properties.put(MP4_HDFS_PATH, mp4HdfsFilePath);
    }

    /**
     * Builder pattern for mp4HdfsFilePath property.
     *
     * @param mp4HdfsFilePath the mp4HdfsFilePath
     * @return this
     */
    public ArtifactExtractedInfo mp4HdfsFilePath(final String mp4HdfsFilePath) {
        setMp4HdfsFilePath(mp4HdfsFilePath);
        return this;
    }

    public String getMp4HdfsFilePath() {
        return (String) properties.get(MP4_HDFS_PATH);
    }

    public void setAudioMp4HdfsFilePath(String oggHdfsFilePath) {
        properties.put(AUDIO_MP4_HDFS_PATH, oggHdfsFilePath);
    }

    public String getAudioMp4HdfsFilePath() {
        return (String) properties.get(AUDIO_MP4_HDFS_PATH);
    }

    public void setAudioOggHdfsFilePath(String oggHdfsFilePath) {
        properties.put(AUDIO_OGG_HDFS_PATH, oggHdfsFilePath);
    }

    public String getAudioOggHdfsFilePath() {
        return (String) properties.get(AUDIO_OGG_HDFS_PATH);
    }

    public void setWebMHdfsFilePath(String webMHdfsFilePath) {
        properties.put(WEBM_HDFS_PATH, webMHdfsFilePath);
    }

    /**
     * Builder pattern for webMHdfsFilePath property.
     *
     * @param webMHdfsFilePath the webMHdfsFilePath
     * @return this
     */
    public ArtifactExtractedInfo webMHdfsFilePath(final String webMHdfsFilePath) {
        setWebMHdfsFilePath(webMHdfsFilePath);
        return this;
    }

    public String getWebMHdfsFilePath() {
        return (String) properties.get(WEBM_HDFS_PATH);
    }

    public void setAudioHdfsPath(String audioHdfsPath) {
        properties.put(AUDIO_HDFS_PATH, audioHdfsPath);
    }

    public String getAudioHdfsPath() {
        return (String) properties.get(AUDIO_HDFS_PATH);
    }

    public void setDetectedObjects(List<ArtifactDetectedObject> detectedObjects) {
        properties.put(DETECTED_OBJECTS, detectedObjects);
    }

    public List<ArtifactDetectedObject> getDetectedObjects() {
        return (List<ArtifactDetectedObject>) properties.get(DETECTED_OBJECTS);
    }

    public void setVideoTranscript(VideoTranscript videoTranscript) {
        set(VIDEO_TRANSCRIPT, videoTranscript);
    }

    /**
     * Builder pattern for videoTranscript property.
     *
     * @param videoTranscript the videoTranscript
     * @return this
     */
    public ArtifactExtractedInfo videoTranscript(final VideoTranscript videoTranscript) {
        setVideoTranscript(videoTranscript);
        return this;
    }

    public VideoTranscript getVideoTranscript() {
        return (VideoTranscript) properties.get(VIDEO_TRANSCRIPT);
    }

    public void setPosterFrameHdfsPath(String posterFrameHdfsPath) {
        set(POSTER_FRAME_HDFS_PATH, posterFrameHdfsPath);
    }

    /**
     * Builder pattern for posterFrameHdfsPath property.
     *
     * @param posterFrameHdfsPath the posterFrameHdfsPath
     * @return this
     */
    public ArtifactExtractedInfo posterFrameHdfsPath(final String posterFrameHdfsPath) {
        setPosterFrameHdfsPath(posterFrameHdfsPath);
        return this;
    }

    public String getPosterFrameHdfsPath() {
        return (String) properties.get(POSTER_FRAME_HDFS_PATH);
    }

    public void setVideoDuration(long videoDuration) {
        set(VIDEO_DURATION, videoDuration);
    }

    public long getVideoDuration() {
        return (Long) properties.get(VIDEO_DURATION);
    }

    public void setVideoFrames(List<VideoFrame> videoFrames) {
        // ensure internal video frame list is not externally mutable
        List<VideoFrame> safeClone = Lists.newLinkedList(videoFrames);
        set(VIDEO_FRAMES, safeClone);
    }

    /**
     * Builder pattern for videoFrames property.
     *
     * @param videoFrames the videoFrames
     * @return this
     */
    public ArtifactExtractedInfo videoFrames(final List<VideoFrame> videoFrames) {
        setVideoFrames(videoFrames);
        return this;
    }

    public List<VideoFrame> getVideoFrames() {
        // ensure internal video frames are not externally mutable
        List<VideoFrame> frames = (List<VideoFrame>) properties.get(VIDEO_FRAMES);
        return frames != null ? Collections.unmodifiableList(frames) : null;
    }

    public void setMappingJson(final String mappingJson) {
        set(MAPPING_JSON, mappingJson);
    }

    /**
     * Builder pattern for the mappingJson property.
     *
     * @param mappingJson the mappingJson
     * @return this
     */
    public ArtifactExtractedInfo mappingJson(final String mappingJson) {
        setMappingJson(mappingJson);
        return this;
    }

    public String getMappingJson() {
        return (String) properties.get(MAPPING_JSON);
    }

    public String getMimeType() {
        return (String) properties.get(MIME_TYPE);
    }

    public void setMimeType(String mimeType) {
        set(MIME_TYPE, mimeType);
    }

    /**
     * Builder pattern for the mimeType property.
     *
     * @param mimeType the mimeType
     * @return this
     */
    public ArtifactExtractedInfo mimeType(final String mimeType) {
        setMimeType(mimeType);
        return this;
    }

    public String getFileName() {
        return (String) properties.get(FILE_NAME);
    }

    public void setFileName(String fileName) {
        set(FILE_NAME, fileName);
    }

    public String getFileExtension() {
        return (String) properties.get(FILE_EXTENSION);
    }

    public void setFileExtension(String extension) {
        set(FILE_EXTENSION, extension);
    }

    public String getUrl() {
        return (String) properties.get(URL);
    }

    public void setUrl(String url) {
        set(URL, url);
    }

    /**
     * Builder pattern for the url property.
     *
     * @param url the url
     * @return this
     */
    public ArtifactExtractedInfo url(final String url) {
        setUrl(url);
        return this;
    }

    public String getSource() {
        return (String) properties.get(SOURCE);
    }

    public void setSource(String source) {
        set(SOURCE, source);
    }

    /**
     * Builder pattern for the source property.
     *
     * @param source the source
     * @return this
     */
    public ArtifactExtractedInfo source(final String source) {
        setSource(source);
        return this;
    }

    public String getAuthor() {
        return (String) properties.get(AUTHOR);
    }

    public void setAuthor(String author) {
        set(AUTHOR, author);
    }

    /**
     * Builder pattern for the author property.
     *
     * @param author the author
     * @return this
     */
    public ArtifactExtractedInfo author(final String author) {
        setAuthor(author);
        return this;
    }

    public String getProcess() {
        return (String) properties.get(PROCESS);
    }

    public void setProcess(String process) {
        set(PROCESS, process);
    }

    /**
     * Builder pattern for the process property.
     *
     * @param process the process
     * @return this
     */
    public ArtifactExtractedInfo process(final String process) {
        setProcess(process);
        return this;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + (this.properties != null ? this.properties.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ArtifactExtractedInfo other = (ArtifactExtractedInfo) obj;
        if (!mapEquals(this.properties, other.properties)) {
            return false;
        }

        return true;
    }

    /**
     * Rewriting map equality to account for array-valued properties.  Arrays,
     * such as byte[], do not have a .equals() method and fail the equality
     * check even if Arrays.equals(a, b) returns true.
     *
     * @param map1 the first map
     * @param map2 the second map
     * @return true if the maps are equal
     */
    private <K, V> boolean mapEquals(Map<K, V> map1, Map<K, V> map2) {
        if (map2 == map1) {
            return true;
        }
        if (map2.size() != map1.size()) {
            return false;
        }

        try {
            for (Map.Entry<K, V> entry : map1.entrySet()) {
                K key = entry.getKey();
                V value = entry.getValue();
                if (value == null) {
                    if (!(map2.get(key) == null && map2.containsKey(key)))
                        return false;
                } else if (value.getClass().isArray()) {
                    // need to use reflection since we don't know what type the Array is
                    Method m = Arrays.class.getMethod("equals", value.getClass(), value.getClass());
                    if (m == null) {
                        m = Arrays.class.getMethod("equals", Object[].class, Object[].class);
                    }
                    Boolean eq = (Boolean) m.invoke(null, value, map2.get(key));
                    if (!eq.booleanValue()) {
                        return false;
                    }
                } else {
                    if (!value.equals(map2.get(key)))
                        return false;
                }
            }
        } catch (ClassCastException unused) {
            return false;
        } catch (NullPointerException unused) {
            return false;
        } catch (NoSuchMethodException unused) {
            return false;
        } catch (SecurityException unused) {
            return false;
        } catch (IllegalAccessException unused) {
            return false;
        } catch (IllegalArgumentException unused) {
            return false;
        } catch (InvocationTargetException unused) {
            return false;
        }

        return true;
    }

    public static class VideoFrame {
        private final String hdfsPath;
        private final long frameStartTime;

        public VideoFrame(String hdfsPath, long frameStartTime) {
            this.hdfsPath = hdfsPath;
            this.frameStartTime = frameStartTime;
        }

        public String getHdfsPath() {
            return hdfsPath;
        }

        public long getFrameStartTime() {
            return frameStartTime;
        }
    }
}
