package io.lumify.phoneNumber;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.i18n.phonenumbers.PhoneNumberMatch;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.ingest.term.extraction.TermMention;
import io.lumify.core.model.properties.LumifyProperties;
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

        defaultRegionCode = (String) workerPrepareData.getStormConf().get(DEFAULT_REGION_CODE);
        if (defaultRegionCode == null) {
            defaultRegionCode = DEFAULT_DEFAULT_REGION_CODE;
        }

        entityType = (String) workerPrepareData.getStormConf().get(CONFIG_PHONE_NUMBER_IRI);
        if (entityType == null || entityType.length() == 0) {
            throw new LumifyException("Could not find config: " + CONFIG_PHONE_NUMBER_IRI);
        }
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        LOGGER.debug("Extracting phone numbers from provided text");

        final String text = CharStreams.toString(new InputStreamReader(in, Charsets.UTF_8));

        final Iterable<PhoneNumberMatch> phoneNumbers = phoneNumberUtil.findNumbers(text, defaultRegionCode);
        List<TermMention> termMentions = new ArrayList<TermMention>();
        for (final PhoneNumberMatch phoneNumber : phoneNumbers) {
            TermMention termMention = createTerm(phoneNumber, data.getProperty().getKey(), data.getVisibility());
            termMentions.add(termMention);
        }
        saveTermMentions((Vertex) data.getElement(), termMentions);
        getGraph().flush();

        LOGGER.debug("Number of phone numbers extracted: %d", count(phoneNumbers));
    }

    private TermMention createTerm(final PhoneNumberMatch phoneNumber, String propertyKey, Visibility visibility) {
        final String formattedNumber = phoneNumberUtil.format(phoneNumber.number(), PhoneNumberUtil.PhoneNumberFormat.E164);
        int start = phoneNumber.start();
        int end = phoneNumber.end();

        return new TermMention.Builder(start, end, formattedNumber, entityType, propertyKey, visibility)
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

        if (property.getName().equals(LumifyProperties.RAW.getPropertyName())) {
            return false;
        }

        String mimeType = (String) property.getMetadata().get(LumifyProperties.MIME_TYPE.getPropertyName());
        return !(mimeType == null || !mimeType.startsWith("text"));
    }
}
