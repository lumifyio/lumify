package com.altamiracorp.lumify.textExtraction;

import com.altamiracorp.lumify.ConfigurableMapJobBase;
import com.altamiracorp.lumify.LumifyMapper;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.ucd.AccumuloArtifactInputFormat;
import com.altamiracorp.lumify.core.model.artifact.Artifact;
import com.google.inject.Injector;
import org.apache.accumulo.core.util.CachedConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;


public class TextExtractorConsolidationMR extends ConfigurableMapJobBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(TextExtractorConsolidationMR.class.getName());

    @Override
    protected Class<? extends InputFormat> getInputFormatClassAndInit(Job job) {
        //TODO: Storm refactor
        throw new RuntimeException("No longer valid in the Storm refactor!");
        /*
        Configuration c = getConfiguration();
        AccumuloArtifactInputFormat.init(job, c.getDataStoreUserName(), c.getDataStorePassword(), getAuthorizations(), c.getZookeeperInstanceName(), c.getZookeeperServerNames());
        return AccumuloArtifactInputFormat.class;
        */
    }

    @Override
    protected Class<? extends Mapper> getMapperClass(Job job, Class clazz) {
        return TextExtractorConsolidationMapper.class;
    }

    public static class TextExtractorConsolidationMapper extends LumifyMapper<Text, Artifact, Text, Artifact> {

        @Override
        protected void setup(Context context, Injector injector) throws Exception {
        }

        @Override
        public void safeMap(Text rowKey, Artifact artifact, Context context) throws Exception {
            throw new RuntimeException("storm refactor - not implemented"); // TODO storm refactor
//            LOGGER.info("Consolidating extracted text for artifact: " + artifact.getRowKey().toString());
//            StringBuilder consolidatedText = new StringBuilder();
//            Iterator<Column> columnIterator = artifact.getArtifactExtractedText().getColumns().iterator();
//            while (columnIterator.hasNext()) {
//                consolidatedText.append(columnIterator.next().getValue().toString());
//                if (columnIterator.hasNext()) {
//                    consolidatedText.append("\n\n");
//                }
//            }
//
//            if (StringUtils.isBlank(consolidatedText.toString())) {
//                artifact.getContent().setDocExtractedText((artifact.getGenericMetadata().getFileName()
//                        + "."
//                        + artifact.getGenericMetadata().getFileExtension()).getBytes());
//            } else {
//                artifact.getContent().setDocExtractedText(consolidatedText.toString().getBytes());
//            }
//
//            context.write(new Text(Artifact.TABLE_NAME), artifact);
        }

    }

    @Override
    protected boolean hasConfigurableClassname() {
        return false;
    }

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(CachedConfiguration.getInstance(), new TextExtractorConsolidationMR(), args);
        if (res != 0) {
            System.exit(res);
        }
    }
}
