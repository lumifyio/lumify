package io.lumify.knownEntity;

import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.ingest.term.extraction.TermMention;
import io.lumify.core.model.properties.RawLumifyProperties;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.arabidopsis.ahocorasick.AhoCorasick;
import org.arabidopsis.ahocorasick.SearchResult;
import org.securegraph.Element;
import org.securegraph.Property;
import org.securegraph.Vertex;
import org.securegraph.Visibility;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class KnownEntityExtractorGraphPropertyWorker extends GraphPropertyWorker {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(KnownEntityExtractorGraphPropertyWorker.class);
    public static final String PATH_PREFIX_CONFIG = "termextraction.knownEntities.pathPrefix";
    public static final String DEFAULT_PATH_PREFIX = "hdfs://";
    private AhoCorasick tree;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        String pathPrefix = (String) workerPrepareData.getStormConf().get(PATH_PREFIX_CONFIG);
        if (pathPrefix == null) {
            pathPrefix = DEFAULT_PATH_PREFIX;
        }
        FileSystem fs = workerPrepareData.getHdfsFileSystem();
        this.tree = loadDictionaries(fs, pathPrefix);
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        String text = IOUtils.toString(in); // TODO convert AhoCorasick to use InputStream
        Iterator<SearchResult<Match>> searchResults = tree.search(text.getBytes());
        List<TermMention> termMentions = new ArrayList<TermMention>();
        while (searchResults.hasNext()) {
            SearchResult searchResult = searchResults.next();
            outputResultToTermMention(termMentions, searchResult, data.getProperty().getKey(), data.getVisibility());
            getGraph().flush();
        }
        saveTermMentions((Vertex) data.getElement(), termMentions);
    }

    private void outputResultToTermMention(List<TermMention> termMentions, SearchResult<Match> searchResult, String propertyKey, Visibility visibility) {
        for (Match match : searchResult.getOutputs()) {
            int start = searchResult.getLastIndex() - match.getMatchText().length();
            int end = searchResult.getLastIndex();
            String sign = match.getEntityTitle();
            String ontologyClassUri = match.getConceptTitle();
            termMentions.add(new TermMention.Builder(start, end, sign, ontologyClassUri, propertyKey, visibility)
                    .resolved(true)
                    .useExisting(true)
                    .process(getClass().getName())
                    .build());
        }
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        if (property.getName().equals(RawLumifyProperties.RAW.getKey())) {
            return false;
        }

        String mimeType = (String) property.getMetadata().get(RawLumifyProperties.MIME_TYPE.getKey());
        return !(mimeType == null || !mimeType.startsWith("text"));
    }

    private static AhoCorasick loadDictionaries(FileSystem fs, String pathPrefix) throws IOException {
        AhoCorasick tree = new AhoCorasick();
        Path hdfsDirectory = new Path(pathPrefix, "dictionaries");
        if (!fs.exists(hdfsDirectory)) {
            fs.mkdirs(hdfsDirectory);
        }
        for (FileStatus dictionaryFileStatus : fs.listStatus(hdfsDirectory)) {
            Path hdfsPath = dictionaryFileStatus.getPath();
            if (hdfsPath.getName().startsWith(".") || !hdfsPath.getName().endsWith(".dict")) {
                continue;
            }
            LOGGER.info("Loading known entity dictionary %s", hdfsPath.toString());
            String conceptName = FilenameUtils.getBaseName(hdfsPath.getName());
            conceptName = URLDecoder.decode(conceptName, "UTF-8");
            InputStream dictionaryInputStream = fs.open(hdfsPath);
            try {
                addDictionaryEntriesToTree(tree, conceptName, dictionaryInputStream);
            } finally {
                dictionaryInputStream.close();
            }
        }
        tree.prepare();
        return tree;
    }

    private static void addDictionaryEntriesToTree(AhoCorasick tree, String type, InputStream dictionaryInputStream) throws IOException {
        CsvPreference csvPrefs = CsvPreference.EXCEL_PREFERENCE;
        CsvListReader csvReader = new CsvListReader(new InputStreamReader(dictionaryInputStream), csvPrefs);
        List<String> line;
        while ((line = csvReader.read()) != null) {
            if (line.size() != 2) {
                throw new RuntimeException("Invalid number of entries on a line. Expected 2 found " + line.size());
            }
            tree.add(line.get(0).getBytes(), new Match(type, line.get(0), line.get(1)));
        }
    }

    private static class Match {
        private final String conceptTitle;
        private final String entityTitle;
        private final String matchText;

        public Match(String type, String matchText, String entityTitle) {
            conceptTitle = type;
            this.matchText = matchText;
            this.entityTitle = entityTitle;
        }

        private String getConceptTitle() {
            return conceptTitle;
        }

        private String getEntityTitle() {
            return entityTitle;
        }

        private String getMatchText() {
            return matchText;
        }

        @Override
        public String toString() {
            return matchText;
        }
    }
}
