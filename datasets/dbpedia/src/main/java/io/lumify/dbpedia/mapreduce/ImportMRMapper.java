package io.lumify.dbpedia.mapreduce;

import io.lumify.core.exception.LumifyException;
import io.lumify.core.mapreduce.LumifyElementMapperBase;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.dbpedia.mapreduce.model.LineData;
import io.lumify.dbpedia.mapreduce.model.LinkValue;
import io.lumify.wikipedia.WikipediaConstants;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.securegraph.Vertex;
import org.securegraph.VertexBuilder;
import org.securegraph.Visibility;
import org.securegraph.accumulo.AccumuloAuthorizations;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ImportMRMapper extends LumifyElementMapperBase<LongWritable, Text> {
    private Counter linesProcessedCounter;
    private Visibility visibility;
    private AccumuloAuthorizations authorizations;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);

        this.visibility = new Visibility("");
        this.authorizations = new AccumuloAuthorizations();
        linesProcessedCounter = context.getCounter(DbpediaImportCounters.LINES_PROCESSED);
    }

    @Override
    protected void safeMap(LongWritable key, Text line, Context context) throws Exception {
        String lineString = line.toString().trim();
        try {
            if (lineString.length() == 0) {
                return;
            }
            if (lineString.startsWith("#")) {
                return;
            }

            LineData lineData = LineData.parse(lineString);

            String wikipediaPageVertexId = WikipediaConstants.getWikipediaPageVertexId(lineData.getPageTitle());
            VertexBuilder pageVertexBuilder = prepareVertex(wikipediaPageVertexId, visibility);
            LumifyProperties.CONCEPT_TYPE.setProperty(pageVertexBuilder, WikipediaConstants.WIKIPEDIA_PAGE_CONCEPT_URI, visibility);

            Map<String, Object> titleMetadata = new HashMap<String, Object>();
            LumifyProperties.CONFIDENCE.setMetadata(titleMetadata, 0.1);
            LumifyProperties.TITLE.addPropertyValue(pageVertexBuilder, ImportMR.MULTI_VALUE_KEY, lineData.getPageTitle(), titleMetadata, visibility);

            if (!(lineData.getValue() instanceof LinkValue)) {
                String multiValueKey = lineData.getValue().getValueString();
                pageVertexBuilder.addPropertyValue(multiValueKey, lineData.getPropertyIri(), lineData.getValue().getValue(), visibility);
            }

            Vertex pageVertex = pageVertexBuilder.save(authorizations);

            if (lineData.getValue() instanceof LinkValue) {
                LinkValue linkValue = (LinkValue) lineData.getValue();

                String linkedPageVertexId = WikipediaConstants.getWikipediaPageVertexId(linkValue.getPageTitle());
                VertexBuilder linkedPageVertexBuilder = prepareVertex(linkedPageVertexId, visibility);
                LumifyProperties.CONCEPT_TYPE.setProperty(linkedPageVertexBuilder, WikipediaConstants.WIKIPEDIA_PAGE_CONCEPT_URI, visibility);

                Map<String, Object> linkedTitleMetadata = new HashMap<String, Object>();
                LumifyProperties.CONFIDENCE.setMetadata(linkedTitleMetadata, 0.1);
                LumifyProperties.TITLE.addPropertyValue(linkedPageVertexBuilder, ImportMR.MULTI_VALUE_KEY, linkValue.getPageTitle(), linkedTitleMetadata, visibility);

                Vertex linkedPageVertex = linkedPageVertexBuilder.save(authorizations);

                String label = lineData.getPropertyIri();
                String edgeId = pageVertex.getId() + "_" + label + "_" + linkedPageVertex.getId();
                addEdge(edgeId, pageVertex, linkedPageVertex, label, visibility, authorizations);
            }

            linesProcessedCounter.increment(1);
        } catch (Throwable ex) {
            throw new LumifyException("Could not process line: " + lineString, ex);
        }
    }
}
