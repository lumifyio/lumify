package com.altamiracorp.lumify.core.ingest.audio;

import com.altamiracorp.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import com.altamiracorp.lumify.core.ingest.graphProperty.GraphPropertyWorkResult;
import com.altamiracorp.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.ConceptType;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.properties.RawLumifyProperties;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.lumify.core.util.ProcessRunner;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Property;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.mutation.ExistingElementMutation;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import com.google.inject.Inject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.CONCEPT_TYPE;
import static com.altamiracorp.lumify.core.model.properties.MediaLumifyProperties.*;
import static com.google.common.base.Preconditions.checkNotNull;

public class AudioMp4EncodingWorker extends GraphPropertyWorker {
    private static final String PROPERTY_KEY = AudioMp4EncodingWorker.class.getName();
    private ProcessRunner processRunner;
    private OntologyRepository ontologyRepository;
    private Graph graph;
    private WorkQueueRepository workQueueRepository;

    @Override
    public GraphPropertyWorkResult execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        File mp4File = File.createTempFile("encode_mp4_", ".mp4");
        try {
            processRunner.execute(
                    "ffmpeg",
                    new String[]{
                            "-y", // overwrite output files
                            "-i", data.getLocalFile().getAbsolutePath(),
                            "-acodec", "libfdk_aac",
                            mp4File.getAbsolutePath()
                    },
                    null,
                    data.getLocalFile().getAbsolutePath() + ": "
            );

            ExistingElementMutation<Vertex> m = data.getVertex().prepareMutation();

            InputStream mp4FileIn = new FileInputStream(mp4File);
            try {
                StreamingPropertyValue spv = new StreamingPropertyValue(mp4FileIn, byte[].class);
                spv.searchIndex(false);
                getAudioProperty(AUDIO_TYPE_MP4).addPropertyValue(m, PROPERTY_KEY, spv, data.getProperty().getVisibility());
                getAudioSizeProperty(AUDIO_TYPE_MP4).addPropertyValue(m, PROPERTY_KEY, mp4File.length(), data.getProperty().getVisibility());

                Concept concept = ontologyRepository.getConceptById(ConceptType.AUDIO.toString());
                checkNotNull(concept, "Could not find concept " + ConceptType.AUDIO.toString());
                CONCEPT_TYPE.setProperty(m, concept.getId(), data.getVertex().getVisibility());

                m.save();
                graph.flush();
                workQueueRepository.pushGraphPropertyQueue(data.getVertex().getId(), PROPERTY_KEY, getAudioProperty(AUDIO_TYPE_MP4).getKey());
                workQueueRepository.pushGraphPropertyQueue(data.getVertex().getId(), PROPERTY_KEY, getAudioSizeProperty(AUDIO_TYPE_MP4).getKey());
            } finally {
                mp4FileIn.close();
            }

            return new GraphPropertyWorkResult();
        } finally {
            mp4File.delete();
        }
    }

    @Override
    public boolean isHandled(Vertex vertex, Property property) {
        if (!property.getName().equals(RawLumifyProperties.RAW.getKey())) {
            return false;
        }
        String mimeType = RawLumifyProperties.MIME_TYPE.getPropertyValue(vertex);
        if (mimeType == null || !mimeType.startsWith("audio")) {
            return false;
        }

        return true;
    }

    @Override
    public boolean isLocalFileRequired() {
        return true;
    }

    @Inject
    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    @Inject
    public void setWorkQueueRepository(WorkQueueRepository workQueueRepository) {
        this.workQueueRepository = workQueueRepository;
    }

    @Inject
    public void setOntologyRepository(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }

    @Inject
    public void setProcessRunner(ProcessRunner ffmpeg) {
        this.processRunner = ffmpeg;
    }
}
