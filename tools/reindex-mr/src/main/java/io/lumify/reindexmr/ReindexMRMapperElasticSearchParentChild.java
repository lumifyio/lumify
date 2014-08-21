package io.lumify.reindexmr;


import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.elasticsearch.action.index.IndexRequest;
import org.json.JSONObject;
import org.securegraph.Authorizations;
import org.securegraph.Element;
import org.securegraph.GraphFactory;
import org.securegraph.Property;
import org.securegraph.accumulo.AccumuloAuthorizations;
import org.securegraph.accumulo.AccumuloGraph;
import org.securegraph.accumulo.mapreduce.SecureGraphMRUtils;
import org.securegraph.elasticsearch.ElasticSearchParentChildSearchIndex;
import org.securegraph.elasticsearch.ElasticSearchSearchIndexBase;
import org.securegraph.search.SearchIndex;
import org.securegraph.util.MapUtils;

import java.io.IOException;
import java.util.Map;

public class ReindexMRMapperElasticSearchParentChild extends Mapper<Text, Element, Object, byte[]> {
    private Authorizations authorizations;
    private ElasticSearchParentChildSearchIndex searchIndex;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        Map configurationMap = SecureGraphMRUtils.toMap(context.getConfiguration());
        AccumuloGraph graph = (AccumuloGraph) new GraphFactory().createGraph(MapUtils.getAllWithPrefix(configurationMap, "graph"));
        this.authorizations = new AccumuloAuthorizations(context.getConfiguration().getStrings(SecureGraphMRUtils.CONFIG_AUTHORIZATIONS));
        SearchIndex si = graph.getSearchIndex();
        if (!(si instanceof ElasticSearchParentChildSearchIndex)) {
            throw new IOException("search index not of type " + ElasticSearchParentChildSearchIndex.class.getName() + " but instead is " + si.getClass().getName());
        }
        this.searchIndex = (ElasticSearchParentChildSearchIndex) si;
    }

    @Override
    protected void map(Text rowKey, Element element, Context context) throws IOException, InterruptedException {
        if (element == null) {
            return;
        }
        context.setStatus("Element Id: " + element.getId());

        writeParentDocument(element, context);

        for (Property property : element.getProperties()) {
            writeProperty(element, property, context);
            context.getCounter(ReindexCounters.PROPERTIES_PROCESSED).increment(1);
        }

        context.getCounter(ReindexCounters.ELEMENTS_PROCESSED).increment(1);
    }

    private void writeProperty(Element element, Property property, Context context) throws IOException, InterruptedException {
        IndexRequest propertyDocumentIndexRequest = this.searchIndex.getPropertyDocumentIndexRequest(element, property);
        if (propertyDocumentIndexRequest == null) {
            return;
        }
        JSONObject source = new JSONObject(propertyDocumentIndexRequest.source().toUtf8());
        source.put(ReindexMRElasticSearchParentChild.ES_ID_PROPERTY, propertyDocumentIndexRequest.id());
        source.put(ReindexMRElasticSearchParentChild.ES_ELEMENT_TYPE_PROPERTY, ElasticSearchParentChildSearchIndex.PROPERTY_TYPE);
        source.put(ReindexMRElasticSearchParentChild.ES_PARENT_PROPERTY, propertyDocumentIndexRequest.parent());
        context.write(NullWritable.get(), source.toString().getBytes());
    }

    private void writeParentDocument(Element element, Context context) throws IOException, InterruptedException {
        IndexRequest parentDocumentIndexRequest = this.searchIndex.getParentDocumentIndexRequest(element, authorizations);
        JSONObject source = new JSONObject(parentDocumentIndexRequest.source().toUtf8());
        source.put(ReindexMRElasticSearchParentChild.ES_ID_PROPERTY, parentDocumentIndexRequest.id());
        source.put(ReindexMRElasticSearchParentChild.ES_ELEMENT_TYPE_PROPERTY, ElasticSearchSearchIndexBase.ELEMENT_TYPE);
        source.put(ReindexMRElasticSearchParentChild.ES_PARENT_PROPERTY, "");
        context.write(NullWritable.get(), source.toString().getBytes());
        context.progress();
    }
}
