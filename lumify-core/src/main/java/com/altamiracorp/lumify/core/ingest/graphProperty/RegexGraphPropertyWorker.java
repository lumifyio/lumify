package com.altamiracorp.lumify.core.ingest.graphProperty;

import com.altamiracorp.lumify.core.ingest.term.extraction.TermMention;
import com.altamiracorp.lumify.core.model.properties.RawLumifyProperties;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.securegraph.Property;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.Visibility;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class RegexGraphPropertyWorker extends GraphPropertyWorker {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(RegexGraphPropertyWorker.class);
    private final Pattern pattern;
    private final String ontologyClassUri;

    public RegexGraphPropertyWorker(String regEx, String ontologyClassUri) {
        this.pattern = Pattern.compile(regEx, Pattern.MULTILINE);
        this.ontologyClassUri = ontologyClassUri;
    }

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        LOGGER.debug("Extractor prepared for entity type [%s] with regular expression: %s", this.ontologyClassUri, this.pattern.toString());
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        LOGGER.debug("Extracting pattern [%s] from provided text", pattern);

        final String text = CharStreams.toString(new InputStreamReader(in, Charsets.UTF_8));

        final Matcher matcher = pattern.matcher(text);

        List<TermMention> termMentions = new ArrayList<TermMention>();
        while (matcher.find()) {
            TermMention termMention = createTerm(matcher, data.getProperty().getKey(), data.getVisibility());
            termMentions.add(termMention);
        }
        saveTermMentions(data.getVertex(), termMentions);
    }

    private TermMention createTerm(final Matcher matched, String propertyKey, Visibility visibility) {
        final String patternGroup = matched.group();
        int start = matched.start();
        int end = matched.end();

        return new TermMention.Builder(start, end, patternGroup, this.ontologyClassUri, propertyKey, visibility)
                .resolved(false)
                .useExisting(true)
                .process(getClass().getName())
                .build();
    }

    @Override
    public boolean isHandled(Vertex vertex, Property property) {
        if (property.getName().equals(RawLumifyProperties.RAW.getKey())) {
            return false;
        }

        String mimeType = (String) property.getMetadata().get(RawLumifyProperties.METADATA_MIME_TYPE);
        return !(mimeType == null || !mimeType.startsWith("text"));
    }
}
