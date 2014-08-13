package io.lumify.nlptools;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.commons.io.IOUtils;
import org.securegraph.Element;
import org.securegraph.Property;
import org.securegraph.Vertex;
import org.securegraph.mutation.ExistingElementMutation;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class SentimentAnalysisGraphPropertyWorker extends GraphPropertyWorker {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(SentimentAnalysisGraphPropertyWorker.class);
    private static final String MULTI_VALUE_KEY = SentimentAnalysisGraphPropertyWorker.class.getName();
    private static final String SENTIMENT_IRI = "ontology.iri.sentiment";
    private String sentimentIri;
    private StanfordCoreNLP pipeline;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);

        sentimentIri = (String) workerPrepareData.getStormConf().get(SENTIMENT_IRI);
        if (sentimentIri == null || sentimentIri.length() == 0) {
            LOGGER.warn("Could not find config: " + SENTIMENT_IRI + ": skipping adding sentiment value.");
        }

        pipeline = setupNLPPipeline();
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();
        String text = IOUtils.toString(in, "UTF-8");
        Double sentiment = calculateSentimentForText(pipeline, text);

        if (sentiment != null) {
            m.addPropertyValue(MULTI_VALUE_KEY, sentimentIri, sentiment, data.getVisibility());
            m.save(getAuthorizations());
            getGraph().flush();
            getWorkQueueRepository().pushGraphPropertyQueue(data.getElement(), MULTI_VALUE_KEY, sentimentIri);
        }
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        return isTextProperty(property);
    }

    private StanfordCoreNLP setupNLPPipeline() {
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma, parse, sentiment");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        return pipeline;
    }

    private Double calculateSentimentForText(StanfordCoreNLP pipeline, String text) {
        //Create an empty Annotation just with the given text.
        Annotation document = new Annotation(text);

        //Run all Annotators on this text.
        pipeline.annotate(document);

        // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        Double sentiment = calculateSentiment(sentences);
        return sentiment;
    }

    private static Double calculateSentiment(List<CoreMap> sentences) {
        int sentenceCount = 0;
        int totalSentiment = 0;
        for (CoreMap sentence : sentences) {
            Tree sentimentTree = sentence.get(SentimentCoreAnnotations.AnnotatedTree.class);
            int sentiment = RNNCoreAnnotations.getPredictedClass(sentimentTree);

            sentenceCount++;
            totalSentiment += sentiment;
        }
        if (sentenceCount > 0) {
            double averageSentiment = (double) totalSentiment / sentenceCount;
            return convertSentimentToNewScale(averageSentiment);
        } else {
            return null;
        }
    }

    /**
     * Convert sentiment from a 0,1,2,3,4 scale to a -1 to 1 scale.
     * Old scale:
     * 0 = very negative.
     * 2 = neutral.
     * 4 = very positive.
     * New scale:
     * -1 = very negative.
     * 0 = neutral.
     * 1 = very positive.
     *
     * @param oldSentiment
     * @return
     */
    private static double convertSentimentToNewScale(double oldSentiment) {
        double newSentiment = (oldSentiment - 2) / 2.0;
        return newSentiment;
    }

}
