package io.lumify.opennlpDictionary;

import com.altamiracorp.bigtable.model.ModelSession;
import io.lumify.core.cmdline.CommandLineBase;
import io.lumify.core.model.ontology.Concept;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.user.User;
import io.lumify.opennlpDictionary.model.DictionaryEntryRepository;
import com.google.inject.Inject;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FilenameUtils;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;

import static com.google.common.base.Preconditions.checkNotNull;

public class DictionaryImporter extends CommandLineBase {
    private ModelSession modelSession;
    private DictionaryEntryRepository dictionaryEntryRepository;
    private OntologyRepository ontologyRepository;
    private String directory;
    private String extension;

    public static void main(String[] args) throws Exception {
        int res = new DictionaryImporter().run(args);
        if (res != 0) {
            System.exit(res);
        }
    }

    @Override
    protected void processOptions(CommandLine cmd) throws Exception {
        super.processOptions(cmd);
        this.directory = cmd.getOptionValue("directory");
        this.extension = cmd.getOptionValue("extension") == null ? "dict" : cmd.getOptionValue("extension");
    }

    @Override
    protected Options getOptions() {
        Options options = super.getOptions();

        options.addOption(
                OptionBuilder
                        .withLongOpt("directory")
                        .withDescription("The directory to search for dictionary files")
                        .isRequired()
                        .hasArg(true)
                        .withArgName("dir")
                        .create()
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt("extension")
                        .withDescription("Extension of dictionary files (default: dict)")
                        .hasArg(true)
                        .withArgName("extension")
                        .create()
        );

        return options;
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        User user = getUser();
        FileSystem fs = getFileSystem();

        Path dictionaryPath = new Path(directory);
        FileStatus[] files = fs.listStatus(dictionaryPath, new DictionaryPathFilter(this.extension));
        for (FileStatus fileStatus : files) {
            LOGGER.info("Importing dictionary file: " + fileStatus.getPath().toString());
            String conceptName = FilenameUtils.getBaseName(fileStatus.getPath().toString());
            conceptName = URLDecoder.decode(conceptName, "UTF-8");
            Concept concept = ontologyRepository.getConceptByIRI(conceptName);
            checkNotNull(concept, "Could not find concept with name " + conceptName);
            writeFile(fs.open(fileStatus.getPath()), conceptName, user);
        }

        modelSession.close();
        return 0;
    }

    protected void writeFile(InputStream in, String concept, User user) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line;
        while ((line = br.readLine()) != null) {
            dictionaryEntryRepository.saveNew(line, concept, user);
        }

        in.close();
    }

    @Inject
    public void setOntologyRepository(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }

    @Inject
    public void setModelSession(ModelSession modelSession) {
        this.modelSession = modelSession;
    }

    @Inject
    public void setDictionaryEntryRepository(DictionaryEntryRepository dictionaryEntryRepository) {
        this.dictionaryEntryRepository = dictionaryEntryRepository;
    }

    public static class DictionaryPathFilter implements PathFilter {

        private String extension;

        public DictionaryPathFilter(String extension) {
            this.extension = extension;
        }

        @Override
        public boolean accept(Path path) {
            return FilenameUtils.getExtension(path.toString()).equals(extension);
        }
    }
}
