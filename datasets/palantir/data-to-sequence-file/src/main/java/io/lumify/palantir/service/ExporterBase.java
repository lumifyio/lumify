package io.lumify.palantir.service;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.palantir.DataToSequenceFile;
import io.lumify.palantir.model.PtModelBase;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;

import java.io.IOException;

public abstract class ExporterBase<T extends PtModelBase> {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ExporterBase.class);
    private final Class<T> ptClass;

    protected ExporterBase(Class<T> ptClass) {
        this.ptClass = ptClass;
    }

    public void run(DataToSequenceFile dataToSequenceFile) throws IOException {
        try (SequenceFile.Writer writer = SequenceFile.createWriter(
                dataToSequenceFile.getHadoopConfiguration(),
                SequenceFile.Writer.file(new Path(dataToSequenceFile.getDestinationPath(), getFileName())),
                SequenceFile.Writer.keyClass(getKeyClass()),
                SequenceFile.Writer.valueClass(getPtClass())
        )) {
            long startTime = System.currentTimeMillis();
            LOGGER.info(this.getClass().getName());
            LOGGER.debug("running sql: %s", getSql());
            Iterable<T> rows = dataToSequenceFile.getSqlRunner().select(getSql(), ptClass);
            long count = run(dataToSequenceFile, rows, writer);
            long endTime = System.currentTimeMillis();
            LOGGER.info("Wrote %d %s (time: %dms)", count, this.ptClass.getSimpleName(), endTime - startTime);
        }
    }

    protected void writeOntologyXmlFile(DataToSequenceFile dataToSequenceFile, String uri, String data) {
        String fileName = DataToSequenceFile.ONTOLOGY_XML_DIR_NAME + "/" + uri.replace('.', '/') + ".xml";
        dataToSequenceFile.writeFile(fileName, data);
    }

    protected void writeFile(DataToSequenceFile dataToSequenceFile, String fileName, String data) {
        dataToSequenceFile.writeFile(fileName, data);
    }

    protected Class<?> getKeyClass() {
        return LongWritable.class;
    }

    public String getFileName() {
        return getPtClass().getSimpleName() + ".seq";
    }

    protected abstract String getSql();

    protected long run(DataToSequenceFile dataToSequenceFile, Iterable<T> rows, SequenceFile.Writer outputFile) {
        int count = 0;

        beforeProcessRows(dataToSequenceFile);
        for (T row : rows) {
            if (count % 1000 == 0) {
                LOGGER.debug("Exporting %s: %d", getPtClass().getSimpleName(), count);
            }
            try {
                processRow(dataToSequenceFile, row, outputFile);
            } catch (Throwable ex) {
                handleProcessRowError(row, ex);
            }
            count++;
        }
        afterProcessRows(dataToSequenceFile);

        return count;
    }

    protected void beforeProcessRows(DataToSequenceFile dataToSequenceFile) {

    }

    protected void afterProcessRows(DataToSequenceFile dataToSequenceFile) {

    }

    protected void processRow(DataToSequenceFile dataToSequenceFile, T row, SequenceFile.Writer outputFile) throws IOException {
        outputFile.append(row.getKey(), row);
    }

    protected void handleProcessRowError(T row, Throwable ex) {
        LOGGER.error("Could not process row: %s (type: %s)", row, getPtClass().getSimpleName(), ex);
    }

    public Class<T> getPtClass() {
        return ptClass;
    }
}
