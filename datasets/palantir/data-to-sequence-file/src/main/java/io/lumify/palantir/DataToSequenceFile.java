package io.lumify.palantir;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Joiner;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.palantir.model.PtNoteAndNoteValue;
import io.lumify.palantir.ontologyToOwl.OntologyToOwl;
import io.lumify.palantir.service.*;
import io.lumify.palantir.sqlrunner.SqlRunner;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.securegraph.util.ConvertingIterable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@Parameters(separators = "=", commandDescription = "Exports Palantir Oracle database to hadoop SequenceFiles")
public class DataToSequenceFile implements Exporter.ExporterSource {
    public static final String ONTOLOGY_XML_DIR_NAME = "OntologyXML";

    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(DataToSequenceFile.class);

    @Parameter(names = {"-n", "--namespace"}, description = "Table namespace", required = true)
    private String tableNamespace;

    @Parameter(names = {"-c", "--connectionstring"}, description = "Database connection string", required = true)
    private String databaseConnectionString;

    @Parameter(names = {"-u", "--username"}, description = "Database username", required = true)
    private String databaseUsername;

    @Parameter(names = {"-p", "--password"}, description = "Database password", required = true)
    private String databasePassword;

    @Parameter(names = {"-d", "--dest"}, description = "Destination path hadoop url", required = true)
    private String destination;

    @Parameter(names = {"--baseiri"}, description = "base IRI for ontology", required = true)
    private String baseIri;

    @Parameter(names = {"--exporters"}, description = "comma separated list of exporters (default: all)")
    private String exporters = "all";

    private Path destinationPath;
    private FileSystem fs;
    private SqlRunner sqlRunner;
    private Configuration hadoopConfiguration;
    public static final List<Exporter> EXPORTERS = new ArrayList<>();

    static {
        EXPORTERS.add(new PtObjectTypeExporter());
        EXPORTERS.add(new PtPropertyTypeExporter());
        EXPORTERS.add(new PtLinkTypeExporter());
        EXPORTERS.add(new PtTypeGroupExporter());
        EXPORTERS.add(new PtNodeDisplayTypeExporter());
        EXPORTERS.add(new PtImageInfoExporter());
        EXPORTERS.add(new PtOntologyResourceExporter());
        EXPORTERS.add(new PtLinkRelationExporter());
        EXPORTERS.add(new PtUserExporter());
        EXPORTERS.add(new PtGraphExporter());
        EXPORTERS.add(new PtObjectExporter());
        EXPORTERS.add(new PtGraphObjectExporter());
        EXPORTERS.add(new PtObjectObjectExporter());
        EXPORTERS.add(new PtMediaAndValueExporter());
        EXPORTERS.add(new PtPropertyAndValueExporter());
        EXPORTERS.add(new PtNoteAndNoteValueExporter());
    }

    public DataToSequenceFile(String[] args) {
        new JCommander(this, args);
        EXPORTERS.add(new OntologyToOwl(baseIri));

        if (exporters.equalsIgnoreCase("all")) {
            exporters = Joiner.on(',').join(new ConvertingIterable<Exporter, String>(EXPORTERS) {
                @Override
                protected String convert(Exporter o) {
                    return o.getObjectClass().getSimpleName();
                }
            });
        }

    }

    public static void main(String[] args) throws Exception {
        try {
            new DataToSequenceFile(args).run();
        } catch (Throwable ex) {
            LOGGER.error("Failed to export", ex);
        }
    }

    private void run() throws Exception {
        LOGGER.info("begin export");
        List<Exporter> exportersToRun = getExportersToRun(exporters);
        for (Exporter exporter : exportersToRun) {
            LOGGER.info("Preparing to run: %s", exporter.getClass().getSimpleName());
        }

        hadoopConfiguration = new Configuration(true);
        destinationPath = new Path(destination);
        fs = getFileSystem(hadoopConfiguration);
        sqlRunner = createSqlRunner();
        sqlRunner.connect();
        try {
            for (Exporter exporter : exportersToRun) {
                LOGGER.info("Running: %s", exporter.getClass().getSimpleName());
                exporter.run(this);
            }
        } finally {
            sqlRunner.close();
        }
        LOGGER.info("export complete!");
    }

    private List<Exporter> getExportersToRun(String exporters) {
        List<Exporter> results = new ArrayList<>();
        for (String exporterString : exporters.split(",")) {
            Exporter e = null;
            for (Exporter exporter : EXPORTERS) {
                if (exporter.getObjectClass().getSimpleName().equalsIgnoreCase(exporterString)) {
                    e = exporter;
                    break;
                }
            }
            if (e == null) {
                throw new RuntimeException("invalid exporter: " + exporterString);
            }
            results.add(e);
        }
        return results;
    }

    private SqlRunner createSqlRunner() {
        return new SqlRunner(
                databaseConnectionString,
                databaseUsername,
                databasePassword,
                tableNamespace);
    }

    private FileSystem getFileSystem(Configuration hadoopConfiguration) throws IOException, URISyntaxException {
        FileSystem fs = FileSystem.get(new URI(destination), hadoopConfiguration);
        fs.mkdirs(new Path(destination));
        return fs;
    }

    public Path getDestinationPath() {
        return destinationPath;
    }

    public SqlRunner getSqlRunner() {
        return sqlRunner;
    }

    @Override
    public FileSystem getFileSystem() {
        return fs;
    }

    public Configuration getHadoopConfiguration() {
        return hadoopConfiguration;
    }

    public void writeFile(String fileName, String data) {
        Path path = new Path(getDestinationPath(), fileName);
        try {
            try (FSDataOutputStream out = getFileSystem().create(path, true)) {
                out.write(data.getBytes());
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not write file: " + path, e);
        }
    }
}
