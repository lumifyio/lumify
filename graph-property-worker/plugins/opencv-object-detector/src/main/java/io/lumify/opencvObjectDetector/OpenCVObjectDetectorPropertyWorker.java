package io.lumify.opencvObjectDetector;

import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.ingest.ArtifactDetectedObject;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.objdetect.CascadeClassifier;
import org.securegraph.Element;
import org.securegraph.Property;
import org.securegraph.Vertex;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class OpenCVObjectDetectorPropertyWorker extends GraphPropertyWorker {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(OpenCVObjectDetectorPropertyWorker.class);
    public static final String MULTI_VALUE_KEY_PREFIX = OpenCVObjectDetectorPropertyWorker.class.getName();
    public static final String OPENCV_CLASSIFIER_CONCEPT_LIST = "objectdetection.classifierConcepts";
    public static final String OPENCV_CLASSIFIER_PATH_PREFIX = "objectdetection.classifier.";
    public static final String OPENCV_CLASSIFIER_PATH_SUFFIX = ".path";
    private static final String PROCESS = OpenCVObjectDetectorPropertyWorker.class.getName();

    private List<CascadeClassifierHolder> objectClassifiers = new ArrayList<CascadeClassifierHolder>();

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);

        loadNativeLibrary();

        String conceptListString = (String) workerPrepareData.getConfiguration().get(OPENCV_CLASSIFIER_CONCEPT_LIST);
        checkNotNull(conceptListString, OPENCV_CLASSIFIER_CONCEPT_LIST + " is a required configuration parameter");
        String[] classifierConcepts = conceptListString.split(",");
        for (String classifierConcept : classifierConcepts) {
            String classifierFilePath = (String) workerPrepareData.getConfiguration().get(OPENCV_CLASSIFIER_PATH_PREFIX + classifierConcept + OPENCV_CLASSIFIER_PATH_SUFFIX);

            File localFile = createLocalFile(classifierFilePath, workerPrepareData.getHdfsFileSystem());
            CascadeClassifier objectClassifier = new CascadeClassifier(localFile.getPath());
            String iriConfigurationKey = Configuration.ONTOLOGY_IRI_PREFIX + classifierConcept;
            String conceptIRI = (String) workerPrepareData.getConfiguration().get(iriConfigurationKey);
            if (conceptIRI == null) {
                throw new LumifyException("Could not find concept IRI for " + iriConfigurationKey);
            }
            addObjectClassifier(classifierConcept, objectClassifier, conceptIRI);
            if (!localFile.delete()) {
                LOGGER.warn("Could not delete file: %s", localFile.getAbsolutePath());
            }
        }
    }

    public void loadNativeLibrary() {
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        } catch (UnsatisfiedLinkError ex) {
            String javaLibraryPath = System.getProperty("java.library.path");
            throw new RuntimeException("Could not find opencv library: " + Core.NATIVE_LIBRARY_NAME + " (java.library.path: " + javaLibraryPath + ")", ex);
        }
    }

    public void addObjectClassifier(String concept, CascadeClassifier objectClassifier, String conceptIRI) {
        objectClassifiers.add(new CascadeClassifierHolder(concept, objectClassifier, conceptIRI));
    }

    private File createLocalFile(String classifierFilePath, FileSystem fs) throws IOException {
        File tempFile = File.createTempFile("lumify-opencv-objdetect", ".xml");
        FileOutputStream fos = null;
        InputStream in = null;
        try {
            if (!fs.exists(new Path(classifierFilePath))) {
                throw new LumifyException("HDFS file " + classifierFilePath + " does not exist");
            }
            in = fs.open(new Path(classifierFilePath));
            fos = new FileOutputStream(tempFile);
            IOUtils.copy(in, fos);
        } catch (IOException e) {
            throw new LumifyException("Could not create local file", e);
        } finally {
            if (in != null) {
                in.close();
            }
            if (fos != null) {
                fos.close();
            }
        }
        return tempFile;
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        BufferedImage bImage = ImageIO.read(in);

        List<ArtifactDetectedObject> detectedObjects = detectObjects(bImage);
        saveDetectedObjects((Vertex) data.getElement(), data.getProperty(), detectedObjects);
    }

    private void saveDetectedObjects(Vertex artifactVertex, Property property, List<ArtifactDetectedObject> detectedObjects) {
        getAuditRepository().auditAnalyzedBy(AuditAction.ANALYZED_BY, artifactVertex,
                getClass().getSimpleName(), getUser(), artifactVertex.getVisibility());
        for (ArtifactDetectedObject detectedObject : detectedObjects) {
            saveDetectedObject(artifactVertex, property, detectedObject);
        }
    }

    private void saveDetectedObject(Vertex artifactVertex, Property property, ArtifactDetectedObject detectedObject) {
        String multiKey = detectedObject.getMultivalueKey(MULTI_VALUE_KEY_PREFIX);
        LumifyProperties.DETECTED_OBJECT.addPropertyValue(artifactVertex, multiKey, detectedObject, property.getMetadata(), new LumifyVisibility().getVisibility(), getAuthorizations());
    }

    public List<ArtifactDetectedObject> detectObjects(BufferedImage bImage) {
        List<ArtifactDetectedObject> detectedObjectList = new ArrayList<ArtifactDetectedObject>();
        Mat image = OpenCVUtils.bufferedImageToMat(bImage);
        if (image != null) {
            MatOfRect faceDetections = new MatOfRect();
            double width = image.width();
            double height = image.height();
            for (CascadeClassifierHolder objectClassifier : objectClassifiers) {
                objectClassifier.cascadeClassifier.detectMultiScale(image, faceDetections);

                for (Rect rect : faceDetections.toArray()) {
                    ArtifactDetectedObject detectedObject = new ArtifactDetectedObject(
                            rect.x / width,
                            rect.y / height,
                            (rect.x + rect.width) / width,
                            (rect.y + rect.height) / height,
                            objectClassifier.conceptIRI,
                            PROCESS);
                    detectedObjectList.add(detectedObject);
                }
            }
        }
        return detectedObjectList;
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        String mimeType = (String) property.getMetadata().get(LumifyProperties.MIME_TYPE.getPropertyName());
        return !(mimeType == null || !mimeType.startsWith("image"));
    }

    private class CascadeClassifierHolder {
        public final String concept;
        public final CascadeClassifier cascadeClassifier;
        public final String conceptIRI;

        public CascadeClassifierHolder(String concept, CascadeClassifier cascadeClassifier, String conceptIRI) {
            this.concept = concept;
            this.cascadeClassifier = cascadeClassifier;
            this.conceptIRI = conceptIRI;
        }
    }
}
