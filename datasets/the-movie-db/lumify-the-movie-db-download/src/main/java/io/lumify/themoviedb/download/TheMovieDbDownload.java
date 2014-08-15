package io.lumify.themoviedb.download;

import io.lumify.core.cmdline.CommandLineBase;
import io.lumify.core.util.JSONUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class TheMovieDbDownload extends CommandLineBase {
    private static final String CMD_OPT_API_KEY = "apikey";
    private static final String CMD_OPT_CACHE_DIRECTORY = "cachedir";
    private static final String CMD_OPT_DOWNLOAD_PERSON = "personid";
    private static final String CMD_OPT_DOWNLOAD_MOVIE = "movieid";
    private static final String CMD_OPT_DOWNLOAD_PRODUCTION_COMPANY = "productioncompanyid";
    public static final String DIR_MOVIES = "movies";
    public static final String DIR_PERSONS = "persons";
    public static final String DIR_IMAGES = "images";
    public static final String DIR_PRODUCTION_COMPANIES = "productionCompanies";

    private final Queue<WorkItem> workQueue = new LinkedList<WorkItem>();
    private TheMovieDb theMovieDb;
    private File cacheDir;
    private File moviesDir;
    private File personsDir;
    private File imagesDir;
    private File productionCompanyDir;

    public static void main(String[] args) throws Exception {
        int res = new TheMovieDbDownload().run(args);
        if (res != 0) {
            System.exit(res);
        }
    }

    @Override
    protected Options getOptions() {
        Options opts = super.getOptions();

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_API_KEY)
                        .withDescription("TheMovieDb API Key")
                        .hasArg()
                        .isRequired()
                        .create()
        );

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
                        .withLongOpt(CMD_OPT_DOWNLOAD_PERSON)
                        .withDescription("Initial person id to download")
                        .hasArg()
                        .create()
        );

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_DOWNLOAD_MOVIE)
                        .withDescription("Initial movie id to download")
                        .hasArg()
                        .create()
        );

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_DOWNLOAD_PRODUCTION_COMPANY)
                        .withDescription("Initial production company id to download")
                        .hasArg()
                        .create()
        );

        return opts;
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        String apiKey = cmd.getOptionValue(CMD_OPT_API_KEY);
        theMovieDb = new TheMovieDb(apiKey);
        String cacheDirString = cmd.getOptionValue(CMD_OPT_CACHE_DIRECTORY);
        this.cacheDir = new File(cacheDirString);
        cacheDir.mkdirs();

        this.moviesDir = new File(cacheDir, DIR_MOVIES);
        moviesDir.mkdirs();
        this.personsDir = new File(cacheDir, DIR_PERSONS);
        personsDir.mkdirs();
        this.imagesDir = new File(cacheDir, DIR_IMAGES);
        imagesDir.mkdirs();
        this.productionCompanyDir = new File(cacheDir, DIR_PRODUCTION_COMPANIES);
        productionCompanyDir.mkdirs();

        String[] personIds = cmd.getOptionValues(CMD_OPT_DOWNLOAD_PERSON);
        if (personIds != null) {
            for (String personIdString : personIds) {
                int personId = Integer.parseInt(personIdString);
                queuePersonDownload(personId);
            }
        }

        String[] movieIds = cmd.getOptionValues(CMD_OPT_DOWNLOAD_MOVIE);
        if (movieIds != null) {
            for (String movieIdString : movieIds) {
                int movieId = Integer.parseInt(movieIdString);
                queueMovieDownload(movieId);
            }
        }

        String[] productionCompanyIds = cmd.getOptionValues(CMD_OPT_DOWNLOAD_PRODUCTION_COMPANY);
        if (productionCompanyIds != null) {
            for (String productionCompanyIdString : productionCompanyIds) {
                int productionCompanyId = Integer.parseInt(productionCompanyIdString);
                queueProductionCompanyDownload(productionCompanyId);
            }
        }

        loadCacheDir();
        download();

        return 0;
    }

    private void download() {
        Random random = new Random();
        while (true) {
            LOGGER.debug("%d still in queue", workQueue.size());
            WorkItem workItem = workQueue.poll();
            if (workItem == null) {
                break;
            }
            try {
                if (workItem.process(this)) {
                    Thread.sleep(1000 + random.nextInt(500));
                }
            } catch (Exception e) {
                LOGGER.error("Could not process work item: %s", workItem.toString(), e);
            }
        }
    }


    private void loadCacheDir() {
        LOGGER.debug("Loading: %s", getCacheDir().getAbsolutePath());

        loadProductionCompaniesCacheDir(getProductionCompanyDir());
        loadPersonsCacheDir(getPersonsDir());
        loadMoviesCacheDir(getMoviesDir());
    }

    private void loadMoviesCacheDir(File moviesDir) {
        File[] files = moviesDir.listFiles();
        int i = 1;
        for (File movieFile : files) {
            LOGGER.debug("loading movie %d/%d", i, files.length);
            loadMovieFile(movieFile);
            i++;
        }
    }

    private void loadMovieFile(File movieFile) {
        try {
            if (!movieFile.getName().endsWith(".json")) {
                return;
            }
            LOGGER.debug("Loading Movie File: %s", movieFile);
            String data = FileUtils.readFileToString(movieFile);
            JSONObject movieJson = JSONUtil.parse(data);
            loadMovieJson(movieJson);
        } catch (Exception e) {
            LOGGER.error("Could not read movie %s", movieFile.getAbsolutePath(), e);
        }
    }

    private void loadMovieJson(JSONObject movieJson) throws IOException, ParseException {
        JSONObject credits = movieJson.getJSONObject("credits");
        JSONArray cast = credits.getJSONArray("cast");
        for (int i = 0; i < cast.length(); i++) {
            JSONObject castJson = cast.getJSONObject(i);
            int personId = castJson.getInt("id");
            queuePersonDownload(personId);
        }

        JSONArray productionCompanies = movieJson.optJSONArray("production_companies");
        if (productionCompanies != null) {
            for (int i = 0; i < productionCompanies.length(); i++) {
                JSONObject productionCompany = productionCompanies.getJSONObject(i);
                int productionCompanyId = productionCompany.getInt("id");
                queueProductionCompanyDownload(productionCompanyId);
            }
        }

        getGraph().flush();

        String posterImagePath = movieJson.optString("poster_path");
        if (posterImagePath != null && posterImagePath.length() > 0) {
            if (!hasImageInCache(posterImagePath)) {
                queueImageDownload(posterImagePath);
            }
        }
    }

    private void loadPersonsCacheDir(File personsDir) {
        File[] files = personsDir.listFiles();
        int i = 1;
        for (File personFile : files) {
            LOGGER.debug("Loading person %d/%d", i, files.length);
            loadPersonFile(personFile);
            i++;
        }
    }

    private void loadPersonFile(File personFile) {
        try {
            if (!personFile.getName().endsWith(".json")) {
                return;
            }
            LOGGER.debug("Loading Person File: %s", personFile);
            String data = FileUtils.readFileToString(personFile);
            JSONObject personJson = JSONUtil.parse(data);
            loadPersonJson(personJson);
        } catch (Exception e) {
            LOGGER.error("Could not read person %s", personFile.getAbsolutePath(), e);
        }
    }

    private void loadPersonJson(JSONObject personJson) throws ParseException, IOException {
        JSONObject combinedCredits = personJson.getJSONObject("combined_credits");
        JSONArray cast = combinedCredits.getJSONArray("cast");
        for (int i = 0; i < cast.length(); i++) {
            JSONObject movieJson = cast.getJSONObject(i);
            String mediaType = movieJson.getString("media_type");
            if (!mediaType.equals("movie")) {
                continue;
            }
            int movieId = movieJson.getInt("id");
            queueMovieDownload(movieId);
        }

        getGraph().flush();

        String profileImage = personJson.optString("profile_path");
        if (profileImage != null && profileImage.length() > 0) {
            if (!hasImageInCache(profileImage)) {
                queueImageDownload(profileImage);
            }
        }
    }

    private void loadProductionCompaniesCacheDir(File productionCompanyDir) {
        for (File productionCompanyFile : productionCompanyDir.listFiles()) {
            loadProductionCompanyFile(productionCompanyFile);
        }
    }

    private void loadProductionCompanyFile(File productionCompanyFile) {
        try {
            if (!productionCompanyFile.getName().endsWith(".json")) {
                return;
            }
            LOGGER.debug("Loading production company File: %s", productionCompanyFile);
            String data = FileUtils.readFileToString(productionCompanyFile);
            JSONObject productionCompanyJson = JSONUtil.parse(data);
            loadProductionCompanyJson(productionCompanyJson);
        } catch (Exception e) {
            LOGGER.error("Could not read production company %s", productionCompanyFile.getAbsolutePath(), e);
        }
    }

    private void loadProductionCompanyJson(JSONObject productionCompanyJson) throws IOException {
        int productionCompanyId = productionCompanyJson.getInt("id");
        LOGGER.debug("Loading production company: %d", productionCompanyId);

        String logoImagePath = productionCompanyJson.optString("logo_path");
        if (logoImagePath != null && logoImagePath.length() > 0) {
            if (!hasImageInCache(logoImagePath)) {
                queueImageDownload(logoImagePath);
            }
        }
    }

    public File getCacheDir() {
        return cacheDir;
    }

    public File getMoviesDir() {
        return moviesDir;
    }

    public File getPersonsDir() {
        return personsDir;
    }

    public File getProductionCompanyDir() {
        return productionCompanyDir;
    }

    public File getImagesDir() {
        return imagesDir;
    }

    public boolean hasPersonInCache(int personId) {
        return new File(getPersonsDir(), personId + ".json").exists();
    }

    public boolean hasProductionCompanyInCache(int productionCompanyId) {
        return new File(getProductionCompanyDir(), productionCompanyId + ".json").exists();
    }

    public boolean hasMovieInCache(int movieId) {
        return new File(getMoviesDir(), movieId + ".json").exists();
    }

    public boolean hasImageInCache(String imagePath) {
        return new File(getImagesDir(), imagePath).exists();
    }

    private void queueImageDownload(String imagePath) {
        if (hasImageInCache(imagePath)) {
            return;
        }
        workQueue.add(new ImageDownloadWorkItem(imagePath));
    }

    private void queueMovieDownload(int movieId) {
        if (hasMovieInCache(movieId)) {
            return;
        }
        workQueue.add(new MovieDownloadWorkItem(movieId));
    }

    private void queuePersonDownload(int personId) {
        if (hasPersonInCache(personId)) {
            return;
        }
        workQueue.add(new PersonDownloadWorkItem(personId));
    }

    private void queueProductionCompanyDownload(int productionCompanyId) {
        if (hasProductionCompanyInCache(productionCompanyId)) {
            return;
        }
        workQueue.add(new ProductionCompanyDownloadWorkItem(productionCompanyId));
    }

    public TheMovieDb getTheMovieDb() {
        return theMovieDb;
    }

    public void writeMovie(int movieId, JSONObject movieJson) throws IOException, ParseException {
        File movieFile = new File(getMoviesDir(), movieId + ".json");
        FileUtils.write(movieFile, movieJson.toString(2));
        loadMovieJson(movieJson);
    }

    public void writePerson(int personId, JSONObject personJson) throws IOException, ParseException {
        File personFile = new File(getPersonsDir(), personId + ".json");
        FileUtils.write(personFile, personJson.toString(2));
        loadPersonJson(personJson);
    }

    public void writeProductionCompany(int productionCompanyId, JSONObject productionCompanyJson) throws IOException {
        File personFile = new File(getProductionCompanyDir(), productionCompanyId + ".json");
        FileUtils.write(personFile, productionCompanyJson.toString(2));
        loadProductionCompanyJson(productionCompanyJson);
    }

    public void writeImage(String imagePath, byte[] imageData) throws IOException {
        File imageFile = new File(getImagesDir(), imagePath);
        imageFile.getParentFile().mkdirs();
        FileUtils.writeByteArrayToFile(imageFile, imageData);
    }
}
