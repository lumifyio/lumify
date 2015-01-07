package io.lumify.phoneNumber;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.i18n.phonenumbers.PhoneNumberMatch;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.termMention.TermMentionBuilder;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.web.clientapi.model.VisibilityJson;
import org.securegraph.Element;
import org.securegraph.Property;
import org.securegraph.Vertex;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static org.securegraph.util.IterableUtils.count;

public class PhoneNumberGraphPropertyWorker extends GraphPropertyWorker {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(PhoneNumberGraphPropertyWorker.class);
    public static final String CONFIG_PHONE_NUMBER_IRI = "ontology.iri.phoneNumber";
    public static final String DEFAULT_REGION_CODE = "phoneNumber.defaultRegionCode";
    public static final String DEFAULT_DEFAULT_REGION_CODE = "US";

    private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
    private String defaultRegionCode;
    private String entityType;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);

        defaultRegionCode = (String) workerPrepareData.getConfiguration().get(DEFAULT_REGION_CODE);
        if (defaultRegionCode == null) {
            defaultRegionCode = DEFAULT_DEFAULT_REGION_CODE;
        }

        entityType = (String) workerPrepareData.getConfiguration().get(CONFIG_PHONE_NUMBER_IRI);
        if (entityType == null || entityType.length() == 0) {
            throw new LumifyException("Could not find config: " + CONFIG_PHONE_NUMBER_IRI);
        }
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        LOGGER.debug("Extracting phone numbers from provided text");

        final String text = CharStreams.toString(new InputStreamReader(in, Charsets.UTF_8));

        Vertex sourceVertex = (Vertex) data.getElement();
        VisibilityJson visibilityJson = LumifyProperties.VISIBILITY_JSON.getPropertyValue(sourceVertex);
        final Iterable<PhoneNumberMatch> phoneNumbers = phoneNumberUtil.findNumbers(text, defaultRegionCode);
        List<Vertex> termMentions = new ArrayList<Vertex>();
        for (final PhoneNumberMatch phoneNumber : phoneNumbers) {
            final String formattedNumber = phoneNumberUtil.format(phoneNumber.number(), PhoneNumberUtil.PhoneNumberFormat.E164);
            int start = phoneNumber.start();
            int end = phoneNumber.end();

            Vertex termMention = new TermMentionBuilder()
                    .sourceVertex(sourceVertex)
                    .propertyKey(data.getProperty().getKey())
                    .start(start)
                    .end(end)
                    .title(formattedNumber)
                    .conceptIri(entityType)
                    .visibilityJson(visibilityJson)
                    .process(getClass().getName())
                    .save(getGraph(), getVisibilityTranslator(), getAuthorizations());
            termMentions.add(termMention);
        }
        getGraph().flush();
        applyTermMentionFilters(sourceVertex, termMentions);

        LOGGER.debug("Number of phone numbers extracted: %d", count(phoneNumbers));
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        if (property.getName().equals(LumifyProperties.RAW.getPropertyName())) {
            return false;
        }

        String mimeType = LumifyProperties.MIME_TYPE.getMetadataValue(property.getMetadata(), null);
        return !(mimeType == null || !mimeType.startsWith("text"));
    }
}
