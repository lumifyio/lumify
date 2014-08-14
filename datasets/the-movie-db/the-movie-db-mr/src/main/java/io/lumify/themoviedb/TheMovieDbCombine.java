package io.lumify.themoviedb;

import com.google.inject.Inject;
import io.lumify.core.cmdline.CommandLineBase;
import io.lumify.core.config.Configuration;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

public class TheMovieDbCombine extends CommandLineBase {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(TheMovieDbCombine.class);
    private static final String CMD_OPT_CACHE_DIRECTORY = "cachedir";
    private static final String CMD_OPT_JSON_OUT = "jsonout";
    private static final String CMD_OPT_IMG_OUT = "imgout";
    public static final String DIR_MOVIES = "movies";
    public static final String DIR_PERSONS = "persons";
    public static final String DIR_IMAGES = "images";
    public static final String DIR_PRODUCTION_COMPANIES = "productionCompanies";
    private Configuration configuration;
    private File cacheDir;
    private File imageDir;

    public static void main(String[] args) throws Exception {
        int res = new TheMovieDbCombine().run(args);
        if (res != 0) {
            System.exit(res);
        }
    }

    @Override
    protected Options getOptions() {
        Options opts = super.getOptions();

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_CACHE_DIRECTORY)
                        .withDescription("Directory to cache json documents in")
                        .hasArg()
                        .isRequired()
                        .create()
        );

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_JSON_OUT)
                        .withDescription("The HDFS JSON output filename")
                        .hasArg()
                        .isRequired()
                        .create()
        );

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_IMG_OUT)
                        .withDescription("The HDFS image output filename")
                        .hasArg()
                        .isRequired()
                        .create()
        );

        return opts;
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        String cacheDirString = cmd.getOptionValue(CMD_OPT_CACHE_DIRECTORY);
        this.cacheDir = new File(cacheDirString);
        this.imageDir = new File(this.cacheDir, DIR_IMAGES);

        String jsonOutString = cmd.getOptionValue(CMD_OPT_JSON_OUT);
        Path jsonSeqFilePath = new Path(jsonOutString);
        SequenceFile.Writer jsonOut = SequenceFile.createWriter(
                this.configuration.toHadoopConfiguration(),
                SequenceFile.Writer.file(jsonSeqFilePath),
                SequenceFile.Writer.keyClass(SequenceFileKey.class),
                SequenceFile.Writer.valueClass(Text.class));

        String imageOutString = cmd.getOptionValue(CMD_OPT_IMG_OUT);
        Path imageSeqFilePath = new Path(imageOutString);
        SequenceFile.Writer imageOut = SequenceFile.createWriter(
                this.configuration.toHadoopConfiguration(),
                SequenceFile.Writer.file(imageSeqFilePath),
                SequenceFile.Writer.keyClass(SequenceFileKey.class),
                SequenceFile.Writer.valueClass(BytesWritable.class));

        try {
            processDirectory(jsonOut, imageOut, new File(cacheDir, DIR_MOVIES), RecordType.MOVIE);
            processDirectory(jsonOut, imageOut, new File(cacheDir, DIR_PERSONS), RecordType.PERSON);
            processDirectory(jsonOut, imageOut, new File(cacheDir, DIR_PRODUCTION_COMPANIES), RecordType.PRODUCTION_COMPANY);
        } finally {
            jsonOut.close();
            imageOut.close();
        }
        return 0;
    }

    private void processDirectory(SequenceFile.Writer jsonOut, SequenceFile.Writer imageOut, File dir, RecordType type) throws IOException {
        LOGGER.info("Processing directory: %s", dir.getAbsolutePath());
        for (File f : dir.listFiles()) {
            if (!f.getName().endsWith(".json")) {
                continue;
            }
            processFile(jsonOut, imageOut, f, type);
        }
    }

    private void processFile(SequenceFile.Writer jsonOut, SequenceFile.Writer imageOut, File f, RecordType type) throws IOException {
        String fileContents = FileUtils.readFileToString(f);
        JSONObject json = new JSONObject(fileContents);
        int id = json.getInt("id");
        String title = getTitleFromJson(json);
        jsonOut.append(new SequenceFileKey(type, id, null, title), new Text(fileContents));

        switch (type) {
            case MOVIE:
                writeMovieImages(imageOut, id, title, json);
                break;
            case PERSON:
                writePersonImages(imageOut, id, title, json);
                break;
            case PRODUCTION_COMPANY:
                writeProductionCompanyImages(imageOut, id, title, json);
                break;
        }
    }

    private String getTitleFromJson(JSONObject json) {
        String title = json.optString("title");
        if (title == null || title.length() == 0) {
            title = json.getString("name");
        }
        return title;
    }

    private void writeMovieImages(SequenceFile.Writer imageOut, int id, String title, JSONObject json) throws IOException {
        String posterPath = json.optString("poster_path");
        if (posterPath == null || posterPath.length() == 0) {
            return;
        }
        writeImage(imageOut, RecordType.MOVIE, id, title, posterPath);
    }

    private void writePersonImages(SequenceFile.Writer imageOut, int id, String title, JSONObject json) throws IOException {
        String profilePath = json.optString("profile_path");
        if (profilePath == null || profilePath.length() == 0) {
            return;
        }
        writeImage(imageOut, RecordType.PERSON, id, title, profilePath);
    }

    private void writeProductionCompanyImages(SequenceFile.Writer imageOut, int id, String title, JSONObject json) throws IOException {
        String logoPath = json.optString("logo_path");
        if (logoPath == null || logoPath.length() == 0) {
            return;
        }
        writeImage(imageOut, RecordType.PERSON, id, title, logoPath);
    }

    private void writeImage(SequenceFile.Writer imageOut, RecordType recordType, int id, String title, String path) throws IOException {
        File f = new File(this.imageDir, path);
        if (!f.exists()) {
            return;
        }
        byte[] imageData = FileUtils.readFileToByteArray(f);
        imageOut.append(new SequenceFileKey(recordType, id, path, title), new BytesWritable(imageData));
    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
