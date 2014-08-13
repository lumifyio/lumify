package io.lumify.themoviedb;

import io.lumify.core.cmdline.CommandLineBase;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.util.JSONUtil;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.securegraph.Vertex;
import org.securegraph.VertexBuilder;
import org.securegraph.Visibility;
import org.securegraph.property.StreamingPropertyValue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class TheMovieDbImport extends CommandLineBase {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(TheMovieDbImport.class);
    private static final String CMD_OPT_API_KEY = "apikey";
    private static final String CMD_OPT_CACHE_DIRECTORY = "cachedir";
    private static final String CMD_OPT_DOWNLOAD_PERSON = "personid";
    private static final String CMD_OPT_DOWNLOAD_MOVIE = "movieid";
    private static final String CMD_OPT_DOWNLOAD_PRODUCTION_COMPANY = "productioncompanyid";
    public static final String DIR_MOVIES = "movies";
    public static final String DIR_PERSONS = "persons";
    private static final String DIR_IMAGES = "images";
    private static final String DIR_PRODUCTION_COMPANIES = "productionCompanies";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final String THE_MOVIE_DB_SOURCE = "TheMovieDb.org";
    private static final String MULTI_VALUE_KEY = TheMovieDbImport.class.getName();
    private final Queue<WorkItem> workQueue = new LinkedList<WorkItem>();
    private File cacheDir;
    private File moviesDir;
    private File personsDir;
    private File imagesDir;
    private File productionCompanyDir;
    private TheMovieDb theMovieDb;
    private Visibility visibility;

    public static void main(String[] args) throws Exception {
        int res = new TheMovieDbImport().run(args);
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
        if (apiKey != null) {
            theMovieDb = new TheMovieDb(apiKey);
        }
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

        visibility = new Visibility("");

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

        loadCacheDirIntoLumify();
        getGraph().flush();
        if (theMovieDb != null) {
            download();
        }

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
                    getGraph().flush();
                    Thread.sleep(1000 + random.nextInt(500));
                }
            } catch (Exception e) {
                LOGGER.error("Could not process work item: %s", workItem.toString(), e);
            }
        }
    }

    private void loadCacheDirIntoLumify() {
        LOGGER.debug("Loading: %s", getCacheDir().getAbsolutePath());

        loadMoviesCacheDirIntoLumify(getMoviesDir());
        loadPersonsCacheDirIntoLumify(getPersonsDir());
        loadProductionCompaniesCacheDirIntoLumify(getProductionCompanyDir());
    }

    private void loadProductionCompaniesCacheDirIntoLumify(File productionCompanyDir) {
        for (File productionCompanyFile : productionCompanyDir.listFiles()) {
            loadProductionCompanyFileIntoLumify(productionCompanyFile);
        }
    }

    private void loadProductionCompanyFileIntoLumify(File productionCompanyFile) {
        try {
            if (!productionCompanyFile.getName().endsWith(".json")) {
                return;
            }
            LOGGER.debug("Loading production company File: %s", productionCompanyFile);
            String data = FileUtils.readFileToString(productionCompanyFile);
            JSONObject productionCompanyJson = JSONUtil.parse(data);
            loadProductionCompanyJsonIntoLumify(productionCompanyJson);
        } catch (Exception e) {
            LOGGER.error("Could not read production company %s", productionCompanyFile.getAbsolutePath(), e);
        }
    }

    private void loadProductionCompanyJsonIntoLumify(JSONObject productionCompanyJson) throws IOException {
        int productionCompanyId = productionCompanyJson.getInt("id");
        LOGGER.debug("Loading production company: %d", productionCompanyId);

        VertexBuilder productionCompanyMutation = getGraph().prepareVertex(TheMovieDbOntology.getProductionCompanyVertexId(productionCompanyId), visibility);
        LumifyProperties.CONCEPT_TYPE.addPropertyValue(productionCompanyMutation, MULTI_VALUE_KEY, TheMovieDbOntology.CONCEPT_TYPE_PRODUCTION_COMPANY, visibility);
        LumifyProperties.SOURCE.addPropertyValue(productionCompanyMutation, MULTI_VALUE_KEY, THE_MOVIE_DB_SOURCE, visibility);
        String name = productionCompanyJson.optString("name");
        if (name != null && name.length() > 0) {
            LumifyProperties.TITLE.addPropertyValue(productionCompanyMutation, MULTI_VALUE_KEY, name, visibility);
        }
        productionCompanyMutation.save(getAuthorizations());

        String logoImagePath = productionCompanyJson.optString("logo_path");
        if (logoImagePath != null && logoImagePath.length() > 0) {
            if (hasImageInCache(logoImagePath)) {
                byte[] imageData = FileUtils.readFileToByteArray(new File(getImagesDir(), logoImagePath));
                loadProductionCompanyLogoImageIntoLumify(productionCompanyId, logoImagePath, imageData);
            } else {
                queueProductionCompanyImageDownload(productionCompanyId, logoImagePath);
            }
        }
    }

    private void loadMoviesCacheDirIntoLumify(File moviesDir) {
        for (File movieFile : moviesDir.listFiles()) {
            loadMovieFileIntoLumify(movieFile);
        }
    }

    private void loadMovieFileIntoLumify(File movieFile) {
        try {
            if (!movieFile.getName().endsWith(".json")) {
                return;
            }
            LOGGER.debug("Loading Movie File: %s", movieFile);
            String data = FileUtils.readFileToString(movieFile);
            JSONObject movieJson = JSONUtil.parse(data);
            loadMovieJsonIntoLumify(movieJson);
        } catch (Exception e) {
            LOGGER.error("Could not read movie %s", movieFile.getAbsolutePath(), e);
        }
    }

    private void loadMovieJsonIntoLumify(JSONObject movieJson) throws IOException, ParseException {
        int movieId = movieJson.getInt("id");
        LOGGER.debug("Loading movie: %d", movieId);
        String title = movieJson.getString("title");
        String vertexId = TheMovieDbOntology.getMovieVertexId(movieId);

        VertexBuilder m = getGraph().prepareVertex(vertexId, visibility);
        LumifyProperties.CONCEPT_TYPE.addPropertyValue(m, MULTI_VALUE_KEY, TheMovieDbOntology.CONCEPT_TYPE_MOVIE, visibility);
        LumifyProperties.SOURCE.addPropertyValue(m, MULTI_VALUE_KEY, THE_MOVIE_DB_SOURCE, visibility);
        StreamingPropertyValue rawValue = new StreamingPropertyValue(new ByteArrayInputStream(movieJson.toString().getBytes()), byte[].class);
        rawValue.store(true);
        rawValue.searchIndex(false);
        LumifyProperties.RAW.addPropertyValue(m, MULTI_VALUE_KEY, rawValue, visibility);
        LumifyProperties.TITLE.addPropertyValue(m, MULTI_VALUE_KEY, title, visibility);

        String releaseDateString = movieJson.optString("release_date");
        if (releaseDateString != null && releaseDateString.length() > 0) {
            Date releaseDate = DATE_FORMAT.parse(releaseDateString);
            TheMovieDbOntology.RELEASE_DATE.addPropertyValue(m, MULTI_VALUE_KEY, releaseDate, visibility);
        }

        JSONArray genres = movieJson.optJSONArray("genres");
        if (genres != null) {
            for (int i = 0; i < genres.length(); i++) {
                JSONObject genre = genres.getJSONObject(i);
                String genreName = genre.getString("name");
                TheMovieDbOntology.GENRE.addPropertyValue(m, MULTI_VALUE_KEY + "_" + genreName, genreName, visibility);
            }
        }

        double runtime = movieJson.optDouble("runtime", -1);
        if (runtime > 0) {
            runtime = runtime * 60;
            TheMovieDbOntology.RUNTIME.addPropertyValue(m, MULTI_VALUE_KEY, runtime, visibility);
        }

        int revenue = movieJson.optInt("revenue", -1);
        if (revenue > 0) {
            TheMovieDbOntology.REVENUE.addPropertyValue(m, MULTI_VALUE_KEY, revenue, visibility);
        }

        int budget = movieJson.optInt("budget", -1);
        if (budget > 0) {
            TheMovieDbOntology.BUDGET.addPropertyValue(m, MULTI_VALUE_KEY, budget, visibility);
        }

        String overview = movieJson.optString("overview");
        if (overview != null && overview.length() > 0) {
            TheMovieDbOntology.OVERVIEW.addPropertyValue(m, MULTI_VALUE_KEY, overview, visibility);
        }

        String tagLine = movieJson.optString("tagline");
        if (tagLine != null && tagLine.length() > 0) {
            TheMovieDbOntology.TAG_LINE.addPropertyValue(m, MULTI_VALUE_KEY, tagLine, visibility);
        }

        Vertex movieVertex = m.save(getAuthorizations());

        JSONObject credits = movieJson.getJSONObject("credits");
        JSONArray cast = credits.getJSONArray("cast");
        for (int i = 0; i < cast.length(); i++) {
            JSONObject castJson = cast.getJSONObject(i);
            int personId = castJson.getInt("id");
            VertexBuilder personMutation = getGraph().prepareVertex(TheMovieDbOntology.getPersonVertexId(personId), visibility);
            LumifyProperties.CONCEPT_TYPE.addPropertyValue(personMutation, MULTI_VALUE_KEY, TheMovieDbOntology.CONCEPT_TYPE_PERSON, visibility);
            LumifyProperties.SOURCE.addPropertyValue(personMutation, MULTI_VALUE_KEY, THE_MOVIE_DB_SOURCE, visibility);
            String name = castJson.optString("name");
            if (name != null && name.length() > 0) {
                LumifyProperties.TITLE.addPropertyValue(personMutation, MULTI_VALUE_KEY, name, visibility);
            }
            Vertex personVertex = personMutation.save(getAuthorizations());
            getGraph().addEdge(TheMovieDbOntology.getStarredInEdgeId(personId, movieId), personVertex, movieVertex, TheMovieDbOntology.EDGE_LABEL_STARRED_IN, visibility, getAuthorizations());

            queuePersonDownload(personId);
        }

        JSONArray productionCompanies = movieJson.optJSONArray("production_companies");
        if (productionCompanies != null) {
            for (int i = 0; i < productionCompanies.length(); i++) {
                JSONObject productionCompany = productionCompanies.getJSONObject(i);
                int productionCompanyId = productionCompany.getInt("id");
                VertexBuilder productionCompanyMutation = getGraph().prepareVertex(TheMovieDbOntology.getProductionCompanyVertexId(productionCompanyId), visibility);
                LumifyProperties.CONCEPT_TYPE.addPropertyValue(productionCompanyMutation, MULTI_VALUE_KEY, TheMovieDbOntology.CONCEPT_TYPE_PRODUCTION_COMPANY, visibility);
                LumifyProperties.SOURCE.addPropertyValue(productionCompanyMutation, MULTI_VALUE_KEY, THE_MOVIE_DB_SOURCE, visibility);
                String name = productionCompany.optString("name");
                if (name != null && name.length() > 0) {
                    LumifyProperties.TITLE.addPropertyValue(productionCompanyMutation, MULTI_VALUE_KEY, name, visibility);
                }
                Vertex productionCompanyVertex = productionCompanyMutation.save(getAuthorizations());
                getGraph().addEdge(TheMovieDbOntology.getProductionCompanyProducedEdgeId(productionCompanyId, movieId), productionCompanyVertex, movieVertex, TheMovieDbOntology.EDGE_LABEL_PRODUCED, visibility, getAuthorizations());

                queueProductionCompanyDownload(productionCompanyId);
            }
        }

        String posterImagePath = movieJson.optString("poster_path");
        if (posterImagePath != null && posterImagePath.length() > 0) {
            if (hasImageInCache(posterImagePath)) {
                byte[] imageData = FileUtils.readFileToByteArray(new File(getImagesDir(), posterImagePath));
                loadPosterImageIntoLumify(movieId, posterImagePath, imageData);
            } else {
                queuePosterImageDownload(movieId, posterImagePath);
            }
        }
    }

    private void loadPersonsCacheDirIntoLumify(File personsDir) {
        for (File personFile : personsDir.listFiles()) {
            loadPersonFileIntoLumify(personFile);
        }
    }

    private void loadPersonFileIntoLumify(File personFile) {
        try {
            if (!personFile.getName().endsWith(".json")) {
                return;
            }
            LOGGER.debug("Loading Person File: %s", personFile);
            String data = FileUtils.readFileToString(personFile);
            JSONObject personJson = JSONUtil.parse(data);
            loadPersonJsonIntoLumify(personJson);
        } catch (Exception e) {
            LOGGER.error("Could not read person %s", personFile.getAbsolutePath(), e);
        }
    }

    private void loadPersonJsonIntoLumify(JSONObject personJson) throws ParseException, IOException {
        int personId = personJson.getInt("id");
        LOGGER.debug("Loading person: %d", personId);
        String name = personJson.getString("name");
        String vertexId = TheMovieDbOntology.getPersonVertexId(personId);

        VertexBuilder m = getGraph().prepareVertex(vertexId, visibility);
        LumifyProperties.CONCEPT_TYPE.addPropertyValue(m, MULTI_VALUE_KEY, TheMovieDbOntology.CONCEPT_TYPE_PERSON, visibility);
        LumifyProperties.SOURCE.addPropertyValue(m, MULTI_VALUE_KEY, THE_MOVIE_DB_SOURCE, visibility);
        StreamingPropertyValue rawValue = new StreamingPropertyValue(new ByteArrayInputStream(personJson.toString().getBytes()), byte[].class);
        rawValue.store(true);
        rawValue.searchIndex(false);
        LumifyProperties.RAW.addPropertyValue(m, MULTI_VALUE_KEY, rawValue, visibility);
        LumifyProperties.TITLE.addPropertyValue(m, MULTI_VALUE_KEY, name, visibility);

        String biography = personJson.optString("biography");
        if (biography != null) {
            TheMovieDbOntology.BIOGRAPHY.addPropertyValue(m, MULTI_VALUE_KEY, biography, visibility);
        }

        String birthDateString = personJson.optString("birthday");
        if (birthDateString != null && birthDateString.length() > 0) {
            Date birthDate = DATE_FORMAT.parse(birthDateString);
            TheMovieDbOntology.BIRTHDATE.addPropertyValue(m, MULTI_VALUE_KEY, birthDate, visibility);
        }

        String deathDateString = personJson.optString("deathday");
        if (deathDateString != null && deathDateString.length() > 0) {
            Date deathDate = DATE_FORMAT.parse(deathDateString);
            TheMovieDbOntology.DEATH_DATE.addPropertyValue(m, MULTI_VALUE_KEY, deathDate, visibility);
        }

        JSONArray akas = personJson.optJSONArray("also_known_as");
        if (akas != null) {
            for (int i = 0; i < akas.length(); i++) {
                String aka = akas.getString(i);
                TheMovieDbOntology.ALSO_KNOWN_AS.addPropertyValue(m, "aka" + i, aka, visibility);
            }
        }

        Vertex personVertex = m.save(getAuthorizations());

        JSONObject combinedCredits = personJson.getJSONObject("combined_credits");
        JSONArray cast = combinedCredits.getJSONArray("cast");
        for (int i = 0; i < cast.length(); i++) {
            JSONObject movieJson = cast.getJSONObject(i);
            String mediaType = movieJson.getString("media_type");
            if (!mediaType.equals("movie")) {
                continue;
            }
            int movieId = movieJson.getInt("id");
            VertexBuilder movieMutation = getGraph().prepareVertex(TheMovieDbOntology.getMovieVertexId(movieId), visibility);
            LumifyProperties.CONCEPT_TYPE.addPropertyValue(movieMutation, MULTI_VALUE_KEY, TheMovieDbOntology.CONCEPT_TYPE_MOVIE, visibility);
            LumifyProperties.SOURCE.addPropertyValue(movieMutation, MULTI_VALUE_KEY, THE_MOVIE_DB_SOURCE, visibility);
            String title = movieJson.optString("title");
            if (title != null && title.length() > 0) {
                LumifyProperties.TITLE.addPropertyValue(movieMutation, MULTI_VALUE_KEY, title, visibility);
            }
            Vertex movieVertex = movieMutation.save(getAuthorizations());
            queueMovieDownload(movieId);

            getGraph().addEdge(TheMovieDbOntology.getStarredInEdgeId(personId, movieId), personVertex, movieVertex, TheMovieDbOntology.EDGE_LABEL_STARRED_IN, visibility, getAuthorizations());
        }

        String profileImage = personJson.optString("profile_path");
        if (profileImage != null && profileImage.length() > 0) {
            if (hasImageInCache(profileImage)) {
                byte[] imageData = FileUtils.readFileToByteArray(new File(getImagesDir(), profileImage));
                loadProfileImageIntoLumify(personId, profileImage, imageData);
            } else {
                queueProfileImageDownload(personId, profileImage);
            }
        }
    }

    public void writeProductionCompany(int productionCompanyId, JSONObject productionCompanyJson) throws IOException {
        File personFile = new File(getProductionCompanyDir(), productionCompanyId + ".json");
        FileUtils.write(personFile, productionCompanyJson.toString(2));
        loadProductionCompanyJsonIntoLumify(productionCompanyJson);
    }

    public void writePerson(int personId, JSONObject personJson) throws IOException, ParseException {
        File personFile = new File(getPersonsDir(), personId + ".json");
        FileUtils.write(personFile, personJson.toString(2));
        loadPersonJsonIntoLumify(personJson);
    }

    public void writeMovie(int movieId, JSONObject movieJdon) throws IOException, ParseException {
        File movieFile = new File(getMoviesDir(), movieId + ".json");
        FileUtils.write(movieFile, movieJdon.toString(2));
        loadMovieJsonIntoLumify(movieJdon);
    }

    public void writeProfileImage(int personId, String profileImagePath, byte[] image) throws IOException {
        File profileImageFile = new File(getImagesDir(), profileImagePath);
        profileImageFile.getParentFile().mkdirs();
        FileUtils.writeByteArrayToFile(profileImageFile, image);
        loadProfileImageIntoLumify(personId, profileImagePath, image);
    }

    public void loadProfileImageIntoLumify(int personId, String profileImagePath, byte[] image) {
        String vertexId = TheMovieDbOntology.getProfileImageVertexId(profileImagePath);

        Vertex personVertex = getGraph().getVertex(TheMovieDbOntology.getPersonVertexId(personId), getAuthorizations());
        String personTitle = LumifyProperties.TITLE.getPropertyValue(personVertex);

        VertexBuilder m = getGraph().prepareVertex(vertexId, visibility);
        LumifyProperties.CONCEPT_TYPE.addPropertyValue(m, MULTI_VALUE_KEY, TheMovieDbOntology.CONCEPT_TYPE_PROFILE_IMAGE, visibility);
        LumifyProperties.SOURCE.addPropertyValue(m, MULTI_VALUE_KEY, THE_MOVIE_DB_SOURCE, visibility);
        StreamingPropertyValue rawValue = new StreamingPropertyValue(new ByteArrayInputStream(image), byte[].class);
        rawValue.store(true);
        rawValue.searchIndex(false);
        LumifyProperties.RAW.addPropertyValue(m, MULTI_VALUE_KEY, rawValue, visibility);
        LumifyProperties.TITLE.addPropertyValue(m, MULTI_VALUE_KEY, "Profile Image of " + personTitle, visibility);
        Vertex profileImageVertex = m.save(getAuthorizations());

        getGraph().addEdge(TheMovieDbOntology.getPersonHasProfileImageEdgeId(personId, profileImagePath), personVertex, profileImageVertex, TheMovieDbOntology.EDGE_LABEL_HAS_PROFILE_IMAGE, visibility, getAuthorizations());
        LumifyProperties.ENTITY_HAS_IMAGE_VERTEX_ID.addPropertyValue(personVertex, MULTI_VALUE_KEY, profileImageVertex.getId(), visibility, getAuthorizations());
    }

    public void writePosterImage(int movieId, String posterImagePath, byte[] posterImage) throws IOException {
        File posterImageFile = new File(getImagesDir(), posterImagePath);
        posterImageFile.getParentFile().mkdirs();
        FileUtils.writeByteArrayToFile(posterImageFile, posterImage);
        loadPosterImageIntoLumify(movieId, posterImagePath, posterImage);
    }

    public void writeProductionCompanyLogo(int productionCompanyId, String imagePath, byte[] imageData) throws IOException {
        File posterImageFile = new File(getImagesDir(), imagePath);
        posterImageFile.getParentFile().mkdirs();
        FileUtils.writeByteArrayToFile(posterImageFile, imageData);
        loadProductionCompanyLogoImageIntoLumify(productionCompanyId, imagePath, imageData);
    }

    private void loadPosterImageIntoLumify(int movieId, String posterImagePath, byte[] image) {
        String vertexId = TheMovieDbOntology.getPosterImageVertexId(posterImagePath);

        Vertex movieVertex = getGraph().getVertex(TheMovieDbOntology.getMovieVertexId(movieId), getAuthorizations());
        String movieTitle = LumifyProperties.TITLE.getPropertyValue(movieVertex);

        VertexBuilder m = getGraph().prepareVertex(vertexId, visibility);
        LumifyProperties.CONCEPT_TYPE.addPropertyValue(m, MULTI_VALUE_KEY, TheMovieDbOntology.CONCEPT_TYPE_PROFILE_IMAGE, visibility);
        LumifyProperties.SOURCE.addPropertyValue(m, MULTI_VALUE_KEY, THE_MOVIE_DB_SOURCE, visibility);
        StreamingPropertyValue rawValue = new StreamingPropertyValue(new ByteArrayInputStream(image), byte[].class);
        rawValue.store(true);
        rawValue.searchIndex(false);
        LumifyProperties.RAW.addPropertyValue(m, MULTI_VALUE_KEY, rawValue, visibility);
        LumifyProperties.TITLE.addPropertyValue(m, MULTI_VALUE_KEY, "Poster Image of " + movieTitle, visibility);
        Vertex posterImageVertex = m.save(getAuthorizations());

        getGraph().addEdge(TheMovieDbOntology.getMovieHasPosterImageEdgeId(movieId, posterImagePath), movieVertex, posterImageVertex, TheMovieDbOntology.EDGE_LABEL_HAS_POSTER_IMAGE, visibility, getAuthorizations());
        LumifyProperties.ENTITY_HAS_IMAGE_VERTEX_ID.addPropertyValue(movieVertex, MULTI_VALUE_KEY, posterImageVertex.getId(), visibility, getAuthorizations());
    }

    private void loadProductionCompanyLogoImageIntoLumify(int productionCompanyId, String logoImagePath, byte[] imageData) {
        String vertexId = TheMovieDbOntology.getProductionCompanyImageVertexId(logoImagePath);

        Vertex productionCompanyVertex = getGraph().getVertex(TheMovieDbOntology.getProductionCompanyVertexId(productionCompanyId), getAuthorizations());
        String productionCompanyTitle = LumifyProperties.TITLE.getPropertyValue(productionCompanyVertex);

        VertexBuilder m = getGraph().prepareVertex(vertexId, visibility);
        LumifyProperties.CONCEPT_TYPE.addPropertyValue(m, MULTI_VALUE_KEY, TheMovieDbOntology.CONCEPT_TYPE_LOGO, visibility);
        LumifyProperties.SOURCE.addPropertyValue(m, MULTI_VALUE_KEY, THE_MOVIE_DB_SOURCE, visibility);
        StreamingPropertyValue rawValue = new StreamingPropertyValue(new ByteArrayInputStream(imageData), byte[].class);
        rawValue.store(true);
        rawValue.searchIndex(false);
        LumifyProperties.RAW.addPropertyValue(m, MULTI_VALUE_KEY, rawValue, visibility);
        LumifyProperties.TITLE.addPropertyValue(m, MULTI_VALUE_KEY, "Logo of " + productionCompanyTitle, visibility);
        Vertex logoImageVertex = m.save(getAuthorizations());

        getGraph().addEdge(TheMovieDbOntology.getProductionCompanyHasLogoEdgeId(productionCompanyId, logoImagePath), productionCompanyVertex, logoImageVertex, TheMovieDbOntology.EDGE_LABEL_HAS_LOGO, visibility, getAuthorizations());
        LumifyProperties.ENTITY_HAS_IMAGE_VERTEX_ID.addPropertyValue(productionCompanyVertex, MULTI_VALUE_KEY, logoImageVertex.getId(), visibility, getAuthorizations());
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

    public TheMovieDb getTheMovieDb() {
        return theMovieDb;
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

    private void queueProfileImageDownload(int personId, String profileImage) {
        if (hasImageInCache(profileImage)) {
            return;
        }
        workQueue.add(new ProfileImageDownloadWorkItem(personId, profileImage));
    }

    private void queuePosterImageDownload(int movieId, String posterImagePath) {
        if (hasImageInCache(posterImagePath)) {
            return;
        }
        workQueue.add(new PosterImageDownloadWorkItem(movieId, posterImagePath));
    }

    private void queueProductionCompanyImageDownload(int productionCompanyId, String logoImagePath) {
        if (hasImageInCache(logoImagePath)) {
            return;
        }
        workQueue.add(new ProductionCompanyImageDownloadWorkItem(productionCompanyId, logoImagePath));
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
}
