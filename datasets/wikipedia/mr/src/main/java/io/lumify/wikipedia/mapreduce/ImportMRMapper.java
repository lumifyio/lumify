package io.lumify.wikipedia.mapreduce;

import com.altamiracorp.bigtable.model.accumulo.AccumuloSession;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.config.HashMapConfigurationLoader;
import io.lumify.core.mapreduce.LumifyElementMapperBase;
import io.lumify.core.model.audit.Audit;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.termMention.TermMentionBuilder;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.security.DirectVisibilityTranslator;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.user.SystemUser;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.core.version.VersionService;
import io.lumify.securegraph.model.audit.SecureGraphAuditRepository;
import io.lumify.web.clientapi.model.VisibilityJson;
import io.lumify.wikipedia.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.securegraph.*;
import org.securegraph.accumulo.AccumuloAuthorizations;
import org.securegraph.accumulo.mapreduce.SecureGraphMRUtils;
import org.securegraph.property.StreamingPropertyValue;
import org.securegraph.util.ConvertingIterable;
import org.securegraph.util.JoinIterable;
import org.sweble.wikitext.engine.EngineException;
import org.sweble.wikitext.engine.PageId;
import org.sweble.wikitext.engine.PageTitle;
import org.sweble.wikitext.engine.WtEngineImpl;
import org.sweble.wikitext.engine.config.WikiConfigImpl;
import org.sweble.wikitext.engine.nodes.EngProcessedPage;
import org.sweble.wikitext.engine.utils.DefaultConfigEnWp;
import org.sweble.wikitext.parser.parser.LinkTargetException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

class ImportMRMapper extends LumifyElementMapperBase<LongWritable, Text> {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ImportMRMapper.class);
    public static final String TEXT_XPATH = "/page/revision/text/text()";
    public static final String TITLE_XPATH = "/page/title/text()";
    public static final String REVISION_TIMESTAMP_XPATH = "/page/revision/timestamp/text()";
    public static final SimpleDateFormat ISO8601DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    public static final String CONFIG_SOURCE_FILE_NAME = "sourceFileName";
    private static final String WIKIPEDIA_PROCESS = ImportMR.class.getName();

    private XPathExpression<org.jdom2.Text> textXPath;
    private XPathExpression<org.jdom2.Text> titleXPath;
    private XPathExpression<org.jdom2.Text> revisionTimestampXPath;
    private Visibility visibility;
    private Authorizations authorizations;
    private WikiConfigImpl config;
    private WtEngineImpl compiler;
    private User user;
    private SecureGraphAuditRepository auditRepository;
    private UserRepository userRepository;
    private String sourceFileName;
    private Counter pagesProcessedCounter;
    private Text auditTableNameText;
    private Counter pagesSkippedCounter;
    private VisibilityJson visibilityJson;
    private VisibilityTranslator visibilityTranslator;
    private Visibility defaultVisibility;

    public ImportMRMapper() {
        this.textXPath = XPathFactory.instance().compile(TEXT_XPATH, Filters.text());
        this.titleXPath = XPathFactory.instance().compile(TITLE_XPATH, Filters.text());
        this.revisionTimestampXPath = XPathFactory.instance().compile(REVISION_TIMESTAMP_XPATH, Filters.text());
    }

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        Map configurationMap = SecureGraphMRUtils.toMap(context.getConfiguration());
        this.visibilityTranslator = new DirectVisibilityTranslator();
        this.visibility = this.visibilityTranslator.getDefaultVisibility();
        this.defaultVisibility = this.visibilityTranslator.getDefaultVisibility();
        this.visibilityJson = new VisibilityJson();
        this.authorizations = new AccumuloAuthorizations();
        this.user = new SystemUser(null);
        VersionService versionService = new VersionService();
        Configuration configuration = new HashMapConfigurationLoader(configurationMap).createConfiguration();
        this.auditRepository = new SecureGraphAuditRepository(null, versionService, configuration, null, userRepository);
        this.sourceFileName = context.getConfiguration().get(CONFIG_SOURCE_FILE_NAME);

        try {
            config = DefaultConfigEnWp.generate();
            compiler = new WtEngineImpl(config);
        } catch (Exception ex) {
            throw new IOException("Could not configure sweble", ex);
        }

        pagesProcessedCounter = context.getCounter(WikipediaImportCounters.PAGES_PROCESSED);
        pagesSkippedCounter = context.getCounter(WikipediaImportCounters.PAGES_SKIPPED);
        auditTableNameText = new Text(Audit.TABLE_NAME);
    }

    @Override
    protected void safeMap(LongWritable filePosition, Text line, Context context) throws IOException, InterruptedException {
        ParsePage parsePage;

        TextConverter textConverter = new TextConverter(config);

        String pageString = line.toString().replaceAll("\\\\n", "\n");
        try {
            parsePage = new ParsePage(pageString).invoke();
        } catch (JDOMException e) {
            LOGGER.error("Could not parse XML: " + filePosition + ":\n" + pageString, e);
            context.getCounter(WikipediaImportCounters.XML_PARSE_ERRORS).increment(1);
            return;
        }
        context.progress();

        if (shouldSkip(parsePage)) {
            pagesSkippedCounter.increment(1);
            return;
        }

        String wikipediaPageVertexId = WikipediaConstants.getWikipediaPageVertexId(parsePage.getPageTitle());
        context.setStatus(wikipediaPageVertexId);

        try {
            String wikitext = getPageText(parsePage.getWikitext(), wikipediaPageVertexId, textConverter);
            parsePage.setWikitext(wikitext);
        } catch (Exception ex) {
            LOGGER.error("Could not process wikipedia text: " + filePosition + ":\n" + parsePage.getWikitext(), ex);
            context.getCounter(WikipediaImportCounters.WIKI_TEXT_PARSE_ERRORS).increment(1);
            return;
        }
        context.progress();

        String multiKey = ImportMR.MULTI_VALUE_KEY + '#' + parsePage.getPageTitle();

        Vertex pageVertex = savePage(context, wikipediaPageVertexId, parsePage, pageString, multiKey);
        context.progress();

        savePageLinks(context, pageVertex, textConverter, multiKey);

        pagesProcessedCounter.increment(1);
    }

    private boolean shouldSkip(ParsePage parsePage) {
        String lowerCaseTitle = parsePage.getPageTitle().toLowerCase();
        if (lowerCaseTitle.startsWith("wikipedia:")) {
            return true;
        }
        return false;
    }

    private Vertex savePage(Context context, String wikipediaPageVertexId, ParsePage parsePage, String pageString, String multiKey) throws IOException, InterruptedException {
        boolean isRedirect = parsePage.getWikitext().startsWith("REDIRECT:");

        StreamingPropertyValue rawPropertyValue = new StreamingPropertyValue(new ByteArrayInputStream(pageString.getBytes()), byte[].class);
        rawPropertyValue.store(true);
        rawPropertyValue.searchIndex(false);

        StreamingPropertyValue textPropertyValue = new StreamingPropertyValue(new ByteArrayInputStream(parsePage.getWikitext().getBytes()), String.class);

        VertexBuilder pageVertexBuilder = prepareVertex(wikipediaPageVertexId, visibility);
        LumifyProperties.CONCEPT_TYPE.setProperty(pageVertexBuilder, WikipediaConstants.WIKIPEDIA_PAGE_CONCEPT_URI, visibility);
        LumifyProperties.MIME_TYPE.setProperty(pageVertexBuilder, ImportMR.WIKIPEDIA_MIME_TYPE, visibility);
        LumifyProperties.FILE_NAME.setProperty(pageVertexBuilder, sourceFileName, visibility);
        LumifyProperties.SOURCE.setProperty(pageVertexBuilder, WikipediaConstants.WIKIPEDIA_SOURCE, visibility);

        Metadata rawMetadata = new Metadata();
        LumifyProperties.CONFIDENCE.setMetadata(rawMetadata, isRedirect ? 0.3 : 0.4, defaultVisibility);
        LumifyProperties.RAW.addPropertyValue(pageVertexBuilder, multiKey, rawPropertyValue, rawMetadata, visibility);

        Metadata titleMetadata = new Metadata();
        LumifyProperties.CONFIDENCE.setMetadata(titleMetadata, isRedirect ? 0.3 : 0.4, defaultVisibility);
        LumifyProperties.TITLE.addPropertyValue(pageVertexBuilder, multiKey, parsePage.getPageTitle(), titleMetadata, visibility);

        Metadata sourceUrlMetadata = new Metadata();
        LumifyProperties.CONFIDENCE.setMetadata(sourceUrlMetadata, isRedirect ? 0.3 : 0.4, defaultVisibility);
        LumifyProperties.SOURCE_URL.addPropertyValue(pageVertexBuilder, multiKey, parsePage.getSourceUrl(), sourceUrlMetadata, visibility);

        if (parsePage.getRevisionTimestamp() != null) {
            Metadata publishedDateMetadata = new Metadata();
            LumifyProperties.CONFIDENCE.setMetadata(publishedDateMetadata, isRedirect ? 0.3 : 0.4, defaultVisibility);
            LumifyProperties.PUBLISHED_DATE.addPropertyValue(pageVertexBuilder, multiKey, parsePage.getRevisionTimestamp(), publishedDateMetadata, visibility);
        }

        if (!isRedirect) {
            Metadata textMetadata = new Metadata();
            textMetadata.add(LumifyProperties.META_DATA_TEXT_DESCRIPTION, "Text", defaultVisibility);
            LumifyProperties.TEXT.addPropertyValue(pageVertexBuilder, multiKey, textPropertyValue, textMetadata, visibility);
        }

        Vertex pageVertex = pageVertexBuilder.save(authorizations);

        // audit vertex
        Audit audit = auditRepository.createAudit(AuditAction.CREATE, pageVertex.getId(), "Wikipedia MR", "", user, visibility);
        context.write(auditTableNameText, AccumuloSession.createMutationFromRow(audit));

        // because save above will cause the StreamingPropertyValue to be read we need to reset the position to 0 for search indexing
        rawPropertyValue.getInputStream().reset();
        textPropertyValue.getInputStream().reset();
        return pageVertex;
    }

    private String getPageText(String wikitext, String wikipediaPageVertexId, TextConverter textConverter) throws LinkTargetException, EngineException {
        String fileTitle = wikipediaPageVertexId;
        PageId pageId = new PageId(PageTitle.make(config, fileTitle), -1);
        EngProcessedPage compiledPage = compiler.postprocess(pageId, wikitext, null);
        String text = (String) textConverter.go(compiledPage.getPage());
        if (text.length() > 0) {
            wikitext = text;
        }
        return wikitext;
    }

    private void savePageLinks(Context context, Vertex pageVertex, TextConverter textConverter, String pageTextKey) throws IOException, InterruptedException {
        for (LinkWithOffsets link : getLinks(textConverter)) {
            savePageLink(context, pageVertex, link, pageTextKey);
            context.progress();
        }
    }

    private void savePageLink(Context context, Vertex pageVertex, LinkWithOffsets link, String pageTextKey) throws IOException, InterruptedException {
        String linkTarget = link.getLinkTargetWithoutHash();
        String linkVertexId = WikipediaConstants.getWikipediaPageVertexId(linkTarget);
        context.setStatus(pageVertex.getId() + " [" + linkVertexId + "]");
        VertexBuilder linkedPageVertexBuilder = prepareVertex(linkVertexId, visibility);
        LumifyProperties.CONCEPT_TYPE.setProperty(linkedPageVertexBuilder, WikipediaConstants.WIKIPEDIA_PAGE_CONCEPT_URI, visibility);
        LumifyProperties.MIME_TYPE.setProperty(linkedPageVertexBuilder, ImportMR.WIKIPEDIA_MIME_TYPE, visibility);
        LumifyProperties.SOURCE.setProperty(linkedPageVertexBuilder, WikipediaConstants.WIKIPEDIA_SOURCE, visibility);
        LumifyProperties.FILE_NAME.setProperty(linkedPageVertexBuilder, sourceFileName, visibility);

        Metadata titleMetadata = new Metadata();
        LumifyProperties.CONFIDENCE.setMetadata(titleMetadata, 0.1, defaultVisibility);
        String linkTargetHash = Base64.encodeBase64String(linkTarget.trim().toLowerCase().getBytes());
        LumifyProperties.TITLE.addPropertyValue(linkedPageVertexBuilder, ImportMR.MULTI_VALUE_KEY + "#" + linkTargetHash, linkTarget, titleMetadata, visibility);

        Vertex linkedPageVertex = linkedPageVertexBuilder.save(authorizations);
        Edge edge = addEdge(WikipediaConstants.getWikipediaPageToPageEdgeId(pageVertex, linkedPageVertex),
                pageVertex,
                linkedPageVertex,
                WikipediaConstants.WIKIPEDIA_PAGE_INTERNAL_LINK_WIKIPEDIA_PAGE_CONCEPT_URI,
                visibility,
                authorizations);

        new TermMentionBuilder()
                .sourceVertex(pageVertex)
                .propertyKey(pageTextKey)
                .start(link.getStartOffset())
                .end(link.getEndOffset())
                .title(linkTarget)
                .conceptIri(WikipediaConstants.WIKIPEDIA_PAGE_CONCEPT_URI)
                .visibilityJson(visibilityJson)
                .process(WIKIPEDIA_PROCESS)
                .resolvedTo(linkedPageVertex, edge)
                .save(getGraph(), visibilityTranslator, authorizations);
    }

    private Iterable<LinkWithOffsets> getLinks(TextConverter textConverter) {
        return new JoinIterable<LinkWithOffsets>(
                new ConvertingIterable<InternalLinkWithOffsets, LinkWithOffsets>(textConverter.getInternalLinks()) {
                    @Override
                    protected LinkWithOffsets convert(InternalLinkWithOffsets internalLinkWithOffsets) {
                        return internalLinkWithOffsets;
                    }
                },
                new ConvertingIterable<RedirectWithOffsets, LinkWithOffsets>(textConverter.getRedirects()) {
                    @Override
                    protected LinkWithOffsets convert(RedirectWithOffsets redirectWithOffsets) {
                        return redirectWithOffsets;
                    }
                }
        );
    }

    @Inject
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private class ParsePage {
        private String pageString;
        private String wikitext;
        private String pageTitle;
        private String sourceUrl;
        private Date revisionTimestamp;

        public ParsePage(String pageString) {
            this.pageString = pageString;
        }

        public String getWikitext() {
            return wikitext;
        }

        public String getPageTitle() {
            return pageTitle;
        }

        public String getSourceUrl() {
            return sourceUrl;
        }

        public Date getRevisionTimestamp() {
            return revisionTimestamp;
        }

        public ParsePage invoke() throws JDOMException, IOException {
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(new ByteArrayInputStream(pageString.getBytes()));
            pageTitle = textToString(titleXPath.evaluateFirst(doc));
            wikitext = textToString(textXPath.evaluate(doc));
            sourceUrl = "http://en.wikipedia.org/wiki/" + pageTitle;
            String revisionTimestampString = textToString(revisionTimestampXPath.evaluateFirst(doc));
            revisionTimestamp = null;
            try {
                revisionTimestamp = ISO8601DATEFORMAT.parse(revisionTimestampString);
            } catch (Exception ex) {
                LOGGER.error("Could not parse revision timestamp %s", revisionTimestampString, ex);
            }
            return this;
        }

        private String textToString(List<org.jdom2.Text> texts) {
            StringBuilder sb = new StringBuilder();
            for (org.jdom2.Text t : texts) {
                sb.append(textToString(t));
            }
            return sb.toString();
        }

        private String textToString(org.jdom2.Text text) {
            if (text == null) {
                return "";
            }
            return text.getText();
        }

        public void setWikitext(String wikitext) {
            this.wikitext = wikitext;
        }
    }
}