package io.lumify.sphinx;

import com.google.inject.Inject;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.ingest.video.VideoTranscript;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.util.ProcessRunner;
import org.securegraph.Element;
import org.securegraph.Metadata;
import org.securegraph.Property;
import org.securegraph.Vertex;
import org.securegraph.mutation.ExistingElementMutation;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static com.google.common.base.Preconditions.checkNotNull;

public class SphinxGraphPropertyWorker extends GraphPropertyWorker {
    private static final long BYTES_PER_SAMPLE = 2;
    private static final long SAMPLES_PER_SECOND = 16000;
    public static final String MULTI_VALUE_KEY = SphinxGraphPropertyWorker.class.getName();
    private ProcessRunner processRunner;

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        VideoTranscript transcript = extractTranscriptFromAudio(data.getLocalFile());
        if (transcript == null) {
            return;
        }

        ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();
        Metadata metadata = data.createPropertyMetadata();
        LumifyProperties.META_DATA_TEXT_DESCRIPTION.setMetadata(metadata, "Audio Transcript", getVisibilityTranslator().getDefaultVisibility());
        addVideoTranscriptAsTextPropertiesToMutation(m, MULTI_VALUE_KEY, transcript, metadata, data.getVisibility());
        Vertex v = m.save(getAuthorizations());
        getAuditRepository().auditVertexElementMutation(AuditAction.UPDATE, m, v, MULTI_VALUE_KEY, getUser(), data.getVisibility());
        getAuditRepository().auditAnalyzedBy(AuditAction.ANALYZED_BY, v, getClass().getSimpleName(), getUser(), v.getVisibility());

        getGraph().flush();
        pushVideoTranscriptTextPropertiesOnWorkQueue(data.getElement(), MULTI_VALUE_KEY, transcript);
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        String mimeType = (String) property.getMetadata().getValue(LumifyProperties.MIME_TYPE.getPropertyName());
        return !(mimeType == null || !mimeType.startsWith("audio"));
    }

    private VideoTranscript extractTranscriptFromAudio(File localFile) throws IOException, InterruptedException {
        checkNotNull(localFile, "localFile cannot be null");
        File wavFile = File.createTempFile("encode_wav_", ".wav");
        File wavFileNoSilence = File.createTempFile("encode_wav_no_silence_", ".wav");
        File wavFileNoHeaders = File.createTempFile("encode_wav_noheader_", ".wav");

        try {
            convertAudioTo16bit1Ch(localFile, wavFile);
            removeSilenceFromBeginning(wavFile, wavFileNoSilence);

            long silenceFileSizeDiff = wavFile.length() - wavFileNoSilence.length();
            double timeOffsetInSec = (double) silenceFileSizeDiff / BYTES_PER_SAMPLE / SAMPLES_PER_SECOND;

            WavFileUtil.fixWavHeaders(wavFileNoSilence, wavFileNoHeaders); // TODO patch sphinx to handle headers correctly

            String sphinxOutput = runSphinx(wavFileNoHeaders);

            return SphinxOutputParser.parse(sphinxOutput, timeOffsetInSec);
        } finally {
            wavFile.delete();
            wavFileNoSilence.delete();
            wavFileNoHeaders.delete();
        }
    }

    private String runSphinx(File inFile) throws IOException, InterruptedException {
        checkNotNull(inFile, "inFile cannot be null");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            processRunner.execute(
                    "pocketsphinx_continuous",
                    new String[]{
                            "-infile", inFile.getAbsolutePath(),
                            "-time", "true"
                    },
                    out,
                    inFile.getAbsolutePath() + ": "
            );
        } finally {
            out.close();
        }
        return new String(out.toByteArray());
    }

    private void removeSilenceFromBeginning(File inFile, File outFile) throws IOException, InterruptedException {
        checkNotNull(inFile, "inFile cannot be null");
        checkNotNull(outFile, "outFile cannot be null");
        processRunner.execute(
                "sox",
                new String[]{
                        inFile.getAbsolutePath(),
                        outFile.getAbsolutePath(),
                        "silence", "1", "0.1", "1%", // remove silence from beginning. at least 0.1s of less than 1% volume
                        "pad", "1", "0" // pad 1 second of silence to beginning
                },
                null,
                inFile.getAbsolutePath() + ": "
        );
    }

    private void convertAudioTo16bit1Ch(File inputFile, File outputFile) throws IOException, InterruptedException {
        checkNotNull(inputFile, "inputFile cannot be null");
        checkNotNull(outputFile, "outputFile cannot be null");
        processRunner.execute(
                "ffmpeg",
                new String[]{
                        "-y", // overwrite output files
                        "-i", inputFile.getAbsolutePath(),
                        "-acodec", "pcm_s16le",
                        "-ac", "1",
                        "-ar", Long.toString(SAMPLES_PER_SECOND),
                        outputFile.getAbsolutePath()
                },
                null,
                inputFile.getAbsolutePath() + ": "
        );
    }

    @Inject
    public void setProcessRunner(ProcessRunner processRunner) {
        this.processRunner = processRunner;
    }

    @Override
    public boolean isLocalFileRequired() {
        return true;
    }
}
