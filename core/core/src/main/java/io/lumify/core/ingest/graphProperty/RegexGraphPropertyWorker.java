package io.lumify.core.ingest.graphProperty;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import io.lumify.core.ingest.term.extraction.TermMention;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.properties.RawLumifyProperties;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.securegraph.Element;
import org.securegraph.Property;
import org.securegraph.Vertex;
import org.securegraph.Visibility;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class RegexGraphPropertyWorker extends GraphPropertyWorker {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(RegexGraphPropertyWorker.class);
    private final Pattern pattern;

    public RegexGraphPropertyWorker(String regEx) {
        this.pattern = Pattern.compile(regEx, Pattern.MULTILINE);
    }

    protected abstract String getOntologyClassUri();

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        LOGGER.debug("Extractor prepared for entity type [%s] with regular expression: %s", getOntologyClassUri(), this.pattern.toString());
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
        Vertex v = (Vertex)data.getElement();
        getAuditRepository().auditAnalyzedBy(AuditAction.ANALYZED_BY, v, getClass().getSimpleName(), getUser(), v.getVisibility());
        saveTermMentions((Vertex) data.getElement(), termMentions);
    }

    private TermMention createTerm(final Matcher matched, String propertyKey, Visibility visibility) {
        final String patternGroup = matched.group();
        int start = matched.start();
        int end = matched.end();

        return new TermMention.Builder(start, end, patternGroup, getOntologyClassUri(), propertyKey, visibility)
                .resolved(false)
                .useExisting(true)
                .process(getClass().getName())
                .build();
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        if (property.getName().equals(RawLumifyProperties.RAW.getPropertyName())) {
            return false;
        }

        String mimeType = (String) property.getMetadata().get(RawLumifyProperties.MIME_TYPE.getPropertyName());
        return !(mimeType == null || !mimeType.startsWith("text"));
    }
}
