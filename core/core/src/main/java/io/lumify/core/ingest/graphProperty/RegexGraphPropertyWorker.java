package io.lumify.core.ingest.graphProperty;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.ontology.Concept;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.termMention.TermMentionBuilder;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.securegraph.Element;
import org.securegraph.Property;
import org.securegraph.Vertex;

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

    protected abstract Concept getConcept();

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        LOGGER.debug("Extractor prepared for entity type [%s] with regular expression: %s", getConcept().getIRI(), this.pattern.toString());
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        LOGGER.debug("Extracting pattern [%s] from provided text", pattern);

        final String text = CharStreams.toString(new InputStreamReader(in, Charsets.UTF_8));

        final Matcher matcher = pattern.matcher(text);

        Vertex sourceVertex = (Vertex) data.getElement();

        List<Vertex> termMentions = new ArrayList<Vertex>();
        while (matcher.find()) {
            final String patternGroup = matcher.group();
            int start = matcher.start();
            int end = matcher.end();

            Vertex termMention = new TermMentionBuilder()
                    .sourceVertex(sourceVertex)
                    .propertyKey(data.getProperty().getKey())
                    .start(start)
                    .end(end)
                    .title(patternGroup)
                    .conceptIri(getConcept().getIRI())
                    .visibilityJson(data.getVisibilityJson())
                    .process(getClass().getName())
                    .save(getGraph(), getVisibilityTranslator(), getAuthorizations());
            termMentions.add(termMention);
        }
        applyTermMentionFilters(sourceVertex, termMentions);
        getAuditRepository().auditAnalyzedBy(AuditAction.ANALYZED_BY, sourceVertex, getClass().getSimpleName(), getUser(), sourceVertex.getVisibility());
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        if (property.getName().equals(LumifyProperties.RAW.getPropertyName())) {
            return false;
        }

        String mimeType = (String) property.getMetadata().getValue(LumifyProperties.MIME_TYPE.getPropertyName());
        return !(mimeType == null || !mimeType.startsWith("text"));
    }
}
