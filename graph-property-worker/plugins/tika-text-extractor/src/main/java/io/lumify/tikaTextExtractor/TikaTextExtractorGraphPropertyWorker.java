package io.lumify.tikaTextExtractor;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.extractors.NumWordsRulesExtractor;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.commons.io.IOUtils;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TemporaryResources;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.CompositeParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.LumifyParserConfig;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.SecureContentHandler;
import org.json.JSONException;
import org.json.JSONObject;
import org.securegraph.Element;
import org.securegraph.Property;
import org.securegraph.Vertex;
import org.securegraph.mutation.ExistingElementMutation;
import org.securegraph.property.StreamingPropertyValue;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static com.google.common.base.Preconditions.checkNotNull;

public class TikaTextExtractorGraphPropertyWorker extends GraphPropertyWorker {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(TikaTextExtractorGraphPropertyWorker.class);

    public static final String MULTI_VALUE_KEY = TikaTextExtractorGraphPropertyWorker.class.getName();

    private static final String PROPS_FILE = "tika-extractor.properties";
    private static final String DATE_KEYS_PROPERTY = "tika.extraction.datekeys";
    private static final String SUBJECT_KEYS_PROPERTY = "tika.extraction.titlekeys";
    private static final String AUTHOR_PROPERTY = "tika.extractions.author";
    private static final String URL_KEYS_PROPERTY = "tika.extraction.urlkeys";
    private static final String TYPE_KEYS_PROPERTY = "tika.extraction.typekeys";
    private static final String EXT_URL_KEYS_PROPERTY = "tika.extraction.exturlkeys";
    private static final String SRC_TYPE_KEYS_PROPERTY = "tika.extraction.srctypekeys";
    private static final String RETRIEVAL_TIMESTAMP_KEYS_PROPERTY = "tika.extraction.retrievaltimestampkeys";
    private static final String CUSTOM_FLICKR_METADATA_KEYS_PROPERTY = "tika.extraction.customflickrmetadatakeys";

    private List<String> dateKeys;
    private List<String> subjectKeys;
    private List<String> urlKeys;
    private List<String> typeKeys;
    private List<String> extUrlKeys;
    private List<String> srcTypeKeys;
    private List<String> retrievalTimestampKeys;
    private List<String> customFlickrMetadataKeys;
    private List<String> authorKeys;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);

        // TODO: Create an actual properties class?
        Properties tikaProperties = new Properties();
        try {
            // don't require the properties file
            InputStream propsIn = Thread.currentThread().getContextClassLoader().getResourceAsStream(PROPS_FILE);
            if (propsIn != null) {
                tikaProperties.load(propsIn);
            }
        } catch (IOException e) {
            LOGGER.error("Could not load config: %s", PROPS_FILE);
        }

        dateKeys = Arrays.asList(tikaProperties.getProperty(DATE_KEYS_PROPERTY, "date,published,pubdate,publish_date,last-modified,atc:last-modified").split(","));
        subjectKeys = Arrays.asList(tikaProperties.getProperty(SUBJECT_KEYS_PROPERTY, "title,subject").split(","));
        urlKeys = Arrays.asList(tikaProperties.getProperty(URL_KEYS_PROPERTY, "url,og:url").split(","));
        typeKeys = Arrays.asList(tikaProperties.getProperty(TYPE_KEYS_PROPERTY, "Content-Type").split(","));
        extUrlKeys = Arrays.asList(tikaProperties.getProperty(EXT_URL_KEYS_PROPERTY, "atc:result-url").split(","));
        srcTypeKeys = Arrays.asList(tikaProperties.getProperty(SRC_TYPE_KEYS_PROPERTY, "og:type").split(","));
        retrievalTimestampKeys = Arrays.asList(tikaProperties.getProperty(RETRIEVAL_TIMESTAMP_KEYS_PROPERTY, "atc:retrieval-timestamp").split(","));
        customFlickrMetadataKeys = Arrays.asList(tikaProperties.getProperty(CUSTOM_FLICKR_METADATA_KEYS_PROPERTY, "Unknown tag (0x9286)").split(","));
        authorKeys = Arrays.asList(tikaProperties.getProperty(AUTHOR_PROPERTY, "author").split(","));
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        String mimeType = (String) data.getProperty().getMetadata().getValue(LumifyProperties.MIME_TYPE.getPropertyName());
        checkNotNull(mimeType, LumifyProperties.MIME_TYPE.getPropertyName() + " is a required metadata field");

        Charset charset = Charset.forName("UTF-8");
        Metadata metadata = new Metadata();
        metadata.set(Metadata.CONTENT_TYPE, mimeType);
        String text = extractText(in, mimeType, metadata);

        ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();

        // TODO set("url", extractUrl(metadata));
        // TODO set("type", extractTextField(metadata, typeKeys));
        // TODO set("extUrl", extractTextField(metadata, extUrlKeys));
        // TODO set("srcType", extractTextField(metadata, srcTypeKeys));

        String author = extractTextField(metadata, authorKeys);
        if (author != null && author.length() > 0) {
            LumifyProperties.AUTHOR.addPropertyValue(m, MULTI_VALUE_KEY, author, data.createPropertyMetadata(), data.getVisibility());
        }

        String customImageMetadata = extractTextField(metadata, customFlickrMetadataKeys);
        org.securegraph.Metadata textMetadata = data.createPropertyMetadata();
        textMetadata.add(LumifyProperties.MIME_TYPE.getPropertyName(), "text/plain", getVisibilityTranslator().getDefaultVisibility());
        textMetadata.add(LumifyProperties.META_DATA_TEXT_DESCRIPTION, "Extracted Text", getVisibilityTranslator().getDefaultVisibility());

        if (customImageMetadata != null && !customImageMetadata.equals("")) {
            try {
                JSONObject customImageMetadataJson = new JSONObject(customImageMetadata);

                text = new JSONObject(customImageMetadataJson.get("description").toString()).get("_content") +
                        "\n" + customImageMetadataJson.get("tags").toString();
                StreamingPropertyValue textValue = new StreamingPropertyValue(new ByteArrayInputStream(text.getBytes(charset)), String.class);
                LumifyProperties.TEXT.addPropertyValue(m, MULTI_VALUE_KEY, textValue, textMetadata, data.getVisibility());

                Date lastupdate = GenericDateExtractor
                        .extractSingleDate(customImageMetadataJson.get("lastupdate").toString());
                LumifyProperties.CREATE_DATE.addPropertyValue(m, MULTI_VALUE_KEY, lastupdate, data.createPropertyMetadata(), data.getVisibility());

                // TODO set("retrievalTime", Long.parseLong(customImageMetadataJson.get("atc:retrieval-timestamp").toString()));

                org.securegraph.Metadata titleMetadata = data.createPropertyMetadata();
                LumifyProperties.CONFIDENCE.setMetadata(titleMetadata, 0.4, getVisibilityTranslator().getDefaultVisibility());
                LumifyProperties.TITLE.addPropertyValue(m, MULTI_VALUE_KEY, customImageMetadataJson.get("title").toString(), titleMetadata, data.getVisibility());
            } catch (JSONException e) {
                LOGGER.warn("Image returned invalid custom metadata");
            }
        } else {
            StreamingPropertyValue textValue = new StreamingPropertyValue(new ByteArrayInputStream(text.getBytes(charset)), String.class);
            LumifyProperties.TEXT.addPropertyValue(m, MULTI_VALUE_KEY, textValue, textMetadata, data.getVisibility());

            LumifyProperties.CREATE_DATE.addPropertyValue(m, MULTI_VALUE_KEY, extractDate(metadata), data.createPropertyMetadata(), data.getVisibility());
            String title = extractTextField(metadata, subjectKeys).trim();
            if (title != null && title.length() > 0) {
                org.securegraph.Metadata titleMetadata = data.createPropertyMetadata();
                LumifyProperties.CONFIDENCE.setMetadata(titleMetadata, 0.4, getVisibilityTranslator().getDefaultVisibility());
                LumifyProperties.TITLE.addPropertyValue(m, MULTI_VALUE_KEY, title, titleMetadata, data.getVisibility());
            }

            // TODO set("retrievalTime", extractRetrievalTime(metadata));
        }

        Vertex v = m.save(getAuthorizations());
        getAuditRepository().auditVertexElementMutation(AuditAction.UPDATE, m, v, MULTI_VALUE_KEY, getUser(), data.getVisibility());
        getAuditRepository().auditAnalyzedBy(AuditAction.ANALYZED_BY, v, getClass().getSimpleName(), getUser(), v.getVisibility());

        getGraph().flush();
        getWorkQueueRepository().pushGraphPropertyQueue(data.getElement(), MULTI_VALUE_KEY,
                LumifyProperties.TEXT.getPropertyName(), data.getWorkspaceId(), data.getVisibilitySource());
    }

    private String extractText(InputStream in, String mimeType, Metadata metadata) throws IOException, SAXException, TikaException, BoilerpipeProcessingException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(in, out);
        byte[] textBytes = out.toByteArray();
        String text;

        metadata.set(Metadata.CONTENT_TYPE, mimeType);
        String bodyContent = extractTextWithTika(textBytes, metadata);

        if (isHtml(mimeType)) {
            text = extractTextFromHtml(IOUtils.toString(textBytes, "UTF-8"));
            if (text == null || text.length() == 0) {
                text = cleanExtractedText(bodyContent);
            }
        } else {
            text = cleanExtractedText(bodyContent);
        }

        return Normalizer.normalize(text, Normalizer.Form.NFC);
    }

    private static String extractTextWithTika(byte[] textBytes, Metadata metadata) throws TikaException, SAXException, IOException {
        TikaConfig tikaConfig = TikaConfig.getDefaultConfig();
        CompositeParser compositeParser = new CompositeParser(tikaConfig.getMediaTypeRegistry(), tikaConfig.getParser());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(output, "UTF-8");
        ContentHandler handler = new BodyContentHandler(writer);
        ParseContext context = new ParseContext();
        context.set(PDFParserConfig.class, new LumifyParserConfig());
        ByteArrayInputStream stream = new ByteArrayInputStream(textBytes);

        TemporaryResources tmp = new TemporaryResources();
        try {
            TikaInputStream tis = TikaInputStream.get(stream, tmp);

            // TIKA-216: Zip bomb prevention
            SecureContentHandler sch = new SecureContentHandler(handler, tis);
            try {
                compositeParser.parse(tis, sch, metadata, context);
            } catch (SAXException e) {
                // Convert zip bomb exceptions to TikaExceptions
                sch.throwIfCauseOf(e);
                throw e;
            }
        } finally {
            tmp.dispose();
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("extracted %d bytes", output.size());
            LOGGER.debug("metadata");
            for (String metadataName : metadata.names()) {
                LOGGER.debug("  %s: %s", metadataName, metadata.get(metadataName));
            }
        }
        return IOUtils.toString(output.toByteArray(), "UTF-8");
    }

    private String extractTextFromHtml(String text) throws BoilerpipeProcessingException {
        String extractedText;

        text = cleanHtml(text);

        extractedText = NumWordsRulesExtractor.getInstance().getText(text);
        if (extractedText != null && extractedText.length() > 0) {
            return extractedText;
        }

        extractedText = ArticleExtractor.getInstance().getText(text);
        if (extractedText != null && extractedText.length() > 0) {
            return extractedText;
        }

        return null;
    }

    private String cleanHtml(String text) {
        text = text.replaceAll("&mdash;", "--");
        text = text.replaceAll("&ldquo;", "\"");
        text = text.replaceAll("&rdquo;", "\"");
        text = text.replaceAll("&lsquo;", "'");
        text = text.replaceAll("&rsquo;", "'");
        return text;
    }

    private Date extractDate(Metadata metadata) {
        // find the date metadata property, if there is one
        String dateKey = TikaMetadataUtils.findKey(dateKeys, metadata);
        Date date = null;
        if (dateKey != null) {
            date = GenericDateExtractor
                    .extractSingleDate(metadata.get(dateKey));
        }

        if (date == null) {
            date = new Date();
        }

        return date;
    }

    private Long extractRetrievalTime(Metadata metadata) {
        Long retrievalTime = 0l;
        String retrievalTimeKey = TikaMetadataUtils.findKey(retrievalTimestampKeys, metadata);

        if (retrievalTimeKey != null) {
            retrievalTime = Long.parseLong(metadata.get(retrievalTimeKey));
        }

        return retrievalTime;
    }

    private String extractTextField(Metadata metadata, List<String> keys) {
        // find the title metadata property, if there is one
        String field = "";
        String fieldKey = TikaMetadataUtils.findKey(keys, metadata);

        if (fieldKey != null) {
            field = metadata.get(fieldKey);
        }

        return field;
    }

    private String extractUrl(Metadata metadata) {
        // find the url metadata property, if there is one; strip down to domain name
        String urlKey = TikaMetadataUtils.findKey(urlKeys, metadata);
        String host = "";
        if (urlKey != null) {
            String url = metadata.get(urlKey);
            try {
                URL netUrl = new URL(url);
                host = netUrl.getHost();
                if (host.startsWith("www")) {
                    host = host.substring("www".length() + 1);
                }
            } catch (MalformedURLException e) {
                throw new RuntimeException("Bad url: " + url);
            }
        }
        return host;
    }

    private boolean isHtml(String mimeType) {
        return mimeType.contains("html");
    }

    private String cleanExtractedText(String extractedText) {
        return extractedText
                // Normalize line breaks
                .replaceAll("\r", "\n")
                        // Remove tabs
                .replaceAll("\t", " ")
                        // Remove non-breaking spaces
                .replaceAll("\u00A0", " ")
                        // Remove newlines that are just paragraph wrapping
                .replaceAll("(?<![\\n])[\\n](?![\\n])", " ")
                        // Remove remaining newlines with exactly 2
                .replaceAll("([ ]*\\n[ ]*)+", "\n\n")
                        // Remove duplicate spaces
                .replaceAll("[ ]+", " ");
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        if (!property.getName().equals(LumifyProperties.RAW.getPropertyName())) {
            return false;
        }

        String mimeType = (String) property.getMetadata().getValue(LumifyProperties.MIME_TYPE.getPropertyName());
        if (mimeType == null) {
            return false;
        }

        if (mimeType.startsWith("image") || mimeType.startsWith("video") || mimeType.startsWith("audio")) {
            return false;
        }

        StreamingPropertyValue mappingJson = LumifyProperties.MAPPING_JSON.getPropertyValue(element);
        if (mappingJson != null) {
            return false;
        }

        return true;
    }
}

