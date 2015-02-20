package io.lumify.palantir.service;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.palantir.DataToSequenceFile;
import io.lumify.palantir.model.PtModelBase;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;

import java.io.IOException;

public abstract class ExporterBase<T extends PtModelBase> implements Exporter {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ExporterBase.class);
    private final Class<T> ptClass;

    protected ExporterBase(Class<T> ptClass) {
        this.ptClass = ptClass;
    }

    @Override
    public Class getObjectClass() {
        return this.ptClass;
    }

    public void run(Exporter.ExporterSource exporterSource) throws IOException {
        try (SequenceFile.Writer writer = SequenceFile.createWriter(
                exporterSource.getHadoopConfiguration(),
                SequenceFile.Writer.file(new Path(exporterSource.getDestinationPath(), getFileName())),
                SequenceFile.Writer.keyClass(getKeyClass()),
                SequenceFile.Writer.valueClass(getPtClass())
        )) {
            long startTime = System.currentTimeMillis();
            LOGGER.info(this.getClass().getName());
            LOGGER.debug("running sql: %s", getSql());
            Iterable<T> rows = exporterSource.getSqlRunner().select(getSql(), ptClass);
            long count = run(exporterSource, rows, writer);
            long endTime = System.currentTimeMillis();
            LOGGER.info("Wrote %d %s (time: %dms)", count, this.ptClass.getSimpleName(), endTime - startTime);
        }
    }

    protected void writeOntologyXmlFile(Exporter.ExporterSource exporterSource, String uri, String data) {
        String fileName = DataToSequenceFile.ONTOLOGY_XML_DIR_NAME + "/" + uri.replace('.', '/') + ".xml";
        exporterSource.writeFile(fileName, data);
    }

    protected void writeFile(Exporter.ExporterSource exporterSource, String fileName, String data) {
        exporterSource.writeFile(fileName, data);
    }

    protected Class<?> getKeyClass() {
        return LongWritable.class;
    }

    public String getFileName() {
        return getPtClass().getSimpleName() + ".seq";
    }

    protected abstract String getSql();

    protected long run(Exporter.ExporterSource exporterSource, Iterable<T> rows, SequenceFile.Writer outputFile) {
        int count = 0;

        beforeProcessRows(exporterSource);
        for (T row : rows) {
            if (count % 1000 == 0) {
                LOGGER.debug("Exporting %s: %d", getPtClass().getSimpleName(), count);
            }
            try {
                processRow(exporterSource, row, outputFile);
            } catch (Throwable ex) {
                handleProcessRowError(row, ex);
            }
            count++;
        }
        afterProcessRows(exporterSource);

        return count;
    }

    protected void beforeProcessRows(Exporter.ExporterSource exporterSource) {

    }

    protected void afterProcessRows(Exporter.ExporterSource exporterSource) {

    }

    protected void processRow(Exporter.ExporterSource exporterSource, T row, SequenceFile.Writer outputFile) throws IOException {
        outputFile.append(row.getKey(), row);
    }

    protected void handleProcessRowError(T row, Throwable ex) {
        LOGGER.error("Could not process row: %s (type: %s)", row, getPtClass().getSimpleName(), ex);
    }

    public Class<T> getPtClass() {
        return ptClass;
    }
}
