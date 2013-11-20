package com.altamiracorp.lumify.entityExtraction;

import com.altamiracorp.lumify.ConfigurableMapJobBase;
import com.altamiracorp.lumify.LumifyMapper;
import com.altamiracorp.lumify.core.model.dictionary.DictionaryEntry;
import com.altamiracorp.lumify.core.model.dictionary.DictionaryEntryRepository;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.accumulo.core.util.CachedConfiguration;
import org.apache.commons.io.FilenameUtils;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;

import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.LineRecordReader;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;

public class DictionaryImportMR extends ConfigurableMapJobBase {

    @Override
    protected Class<? extends InputFormat> getInputFormatClassAndInit(Job job) {
        return DictionaryFileInputFormat.class;
    }

    @Override
    protected Class<? extends Mapper> getMapperClass(Job job, Class clazz) {
        return DictionaryImportMapper.class;
    }

    public static class DictionaryImportMapper extends LumifyMapper<Text, Text, Text, DictionaryEntry> {

        DictionaryEntryRepository dictionaryEntryRepository;

        @Override
        protected void setup(Context context, Injector injector) throws Exception {
            //no-op
        }

        @Override
        protected void safeMap(Text concept, Text tokens, Context context) throws Exception {
            DictionaryEntry entry = dictionaryEntryRepository.createNew(tokens.toString(), concept.toString());
            context.write(new Text(DictionaryEntry.TABLE_NAME), entry);
        }

        @Inject
        public void setDictionaryEntryRepository (DictionaryEntryRepository dictionaryEntryRepository) {
            this.dictionaryEntryRepository = dictionaryEntryRepository;
        }
    }

    @Override
    protected boolean hasConfigurableClassname() {
        return false;
    }

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(CachedConfiguration.getInstance(), new DictionaryImportMR(), args);
        if (res != 0) {
            System.exit(res);
        }
    }

    public static class DictionaryFileInputFormat extends FileInputFormat<Text, Text> {
        @Override
        public RecordReader<Text, Text> createRecordReader(InputSplit split,TaskAttemptContext context) {
            return new DictionaryEntryRecordReader();
        }

        @Override
        protected boolean isSplitable(JobContext context, Path file) {
            CompressionCodec codec =
                    new CompressionCodecFactory(context.getConfiguration()).getCodec(file);
            return codec == null;
        }

    }

    public static class DictionaryEntryRecordReader extends RecordReader<Text,Text> {

        private LineRecordReader wrappedLineRR;
        private Text currentK;

        public DictionaryEntryRecordReader () {
            wrappedLineRR = new LineRecordReader();
        }

        @Override
        public void initialize(InputSplit split, TaskAttemptContext context) throws IOException {
            wrappedLineRR.initialize(split,context);
            FileSplit fileSplit = (FileSplit) split;
            Path path = fileSplit.getPath();
            this.currentK = new Text(FilenameUtils.getBaseName(path.toString()));
        }

        @Override
        public boolean nextKeyValue() throws IOException {
            return wrappedLineRR.nextKeyValue();
        }

        @Override
        public Text getCurrentKey() throws IOException, InterruptedException {
            return this.currentK;
        }

        @Override
        public Text getCurrentValue() throws IOException, InterruptedException {
            return wrappedLineRR.getCurrentValue();
        }

        @Override
        public float getProgress() throws IOException, InterruptedException {
            return wrappedLineRR.getProgress();
        }

        @Override
        public void close() throws IOException {
            wrappedLineRR.close();;
        }
    }

    public static class DictionaryPathFilter implements PathFilter {

        private static final String DICTIONARY_EXTENSION = "dict";

        @Override
        public boolean accept(Path path) {
            return FilenameUtils.getExtension(path.toString()).equals(DICTIONARY_EXTENSION);
        }
    }
}
