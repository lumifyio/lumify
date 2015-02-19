package io.lumify.palantir;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.palantir.service.PtMediaAndValueExporter;
import io.lumify.palantir.sqlrunner.SqlRunner;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Parameters(separators = "=", commandDescription = "Exports Palantir Oracle database to hadoop SequenceFiles")
public class DataToSequenceFile {
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

    private Path destinationPath;
    private FileSystem fs;
    private SqlRunner sqlRunner;
    private Configuration hadoopConfiguration;

    public DataToSequenceFile(String[] args) {
        new JCommander(this, args);
    }

    public static void main(String[] args) throws Exception {
        new DataToSequenceFile(args).run();
    }

    private void run() throws Exception {
        LOGGER.info("begin export");
        hadoopConfiguration = new Configuration(true);
        destinationPath = new Path(destination);
        fs = getFileSystem(hadoopConfiguration);
        sqlRunner = createSqlRunner();
        sqlRunner.connect();
        try {
//            new PtObjectTypeExporter().run(this);
//            new PtPropertyTypeExporter().run(this);
//            new PtLinkTypeExporter().run(this);
//            new PtNodeDisplayTypeExporter().run(this);
//            new PtImageInfoExporter().run(this);
//            new PtOntologyResourceExporter().run(this);
//            new PtLinkRelationExporter().run(this);
//            new OntologyToOwl(baseIri).run(getFs(), getDestinationPath());
//            new PtUserExporter().run(this);
//            new PtGraphExporter().run(this);
//            new PtObjectExporter().run(this);
//            new PtGraphObjectExporter().run(this);
//            new PtObjectObjectExporter().run(this);
            new PtMediaAndValueExporter().run(this);
        } finally {
            sqlRunner.close();
        }
        LOGGER.info("export complete!");
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

    public FileSystem getFs() {
        return fs;
    }

    public Path getDestinationPath() {
        return destinationPath;
    }

    public SqlRunner getSqlRunner() {
        return sqlRunner;
    }

    public Configuration getHadoopConfiguration() {
        return hadoopConfiguration;
    }

    public void writeFile(String fileName, String data) {
        Path path = new Path(getDestinationPath(), fileName);
        try {
            try (FSDataOutputStream out = getFs().create(path, true)) {
                out.write(data.getBytes());
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not write file: " + path, e);
        }
    }
}
