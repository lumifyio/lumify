package io.lumify.themoviedb;

import io.lumify.core.mapreduce.LumifyElementMapperBase;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.security.DirectVisibilityTranslator;
import io.lumify.core.security.VisibilityTranslator;
import org.apache.hadoop.io.Text;
import org.json.JSONArray;
import org.json.JSONObject;
import org.securegraph.Metadata;
import org.securegraph.Vertex;
import org.securegraph.VertexBuilder;
import org.securegraph.Visibility;
import org.securegraph.accumulo.AccumuloAuthorizations;
import org.securegraph.property.StreamingPropertyValue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ImportJsonMRMapper extends LumifyElementMapperBase<SequenceFileKey, Text> {
    public static final String MULTI_VALUE_KEY = ImportJsonMR.class.getName();
    public static final String SOURCE = "TheMovieDb.org";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat DATE_YEAR_FORMAT = new SimpleDateFormat("yyyy");
    private Visibility visibility;
    private AccumuloAuthorizations authorizations;
    private VisibilityTranslator visibilityTranslator;
    private Visibility defaultVisibility;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        this.visibilityTranslator = new DirectVisibilityTranslator();
        this.visibility = this.visibilityTranslator.getDefaultVisibility();
        this.defaultVisibility = visibilityTranslator.getDefaultVisibility();
        this.authorizations = new AccumuloAuthorizations();
    }

    @Override
    protected void safeMap(SequenceFileKey key, Text line, Context context) throws IOException, InterruptedException, ParseException {
        String lineString = line.toString();
        JSONObject json = new JSONObject(lineString);
        int id = json.getInt("id");
        RecordType recordType = key.getRecordType();
        context.setStatus(recordType + ":" + id);

        switch (recordType) {
            case MOVIE:
                mapMovie(id, json, context);
                break;
            case PERSON:
                mapPerson(id, json, context);
                break;
            case PRODUCTION_COMPANY:
                mapProductionCompany(id, json, context);
                break;
        }
    }

    private void mapProductionCompany(int productionCompanyId, JSONObject json, Context context) {
        VertexBuilder productionCompanyMutation = prepareVertex(TheMovieDbOntology.getProductionCompanyVertexId(productionCompanyId), visibility);
        LumifyProperties.CONCEPT_TYPE.addPropertyValue(productionCompanyMutation, MULTI_VALUE_KEY, TheMovieDbOntology.CONCEPT_TYPE_PRODUCTION_COMPANY, visibility);
        LumifyProperties.SOURCE.addPropertyValue(productionCompanyMutation, MULTI_VALUE_KEY, SOURCE, visibility);
        String name = json.optString("name");
        if (name != null && name.length() > 0) {
            LumifyProperties.TITLE.addPropertyValue(productionCompanyMutation, MULTI_VALUE_KEY, name, visibility);
        }
        productionCompanyMutation.save(authorizations);

        context.getCounter(TheMovieDbImportCounters.PRODUCTION_COMPANIES_PROCESSED).increment(1);
    }

    private void mapPerson(int personId, JSONObject personJson, Context context) throws ParseException {
        String name = personJson.getString("name");
        String vertexId = TheMovieDbOntology.getPersonVertexId(personId);

        VertexBuilder m = prepareVertex(vertexId, visibility);
        LumifyProperties.CONCEPT_TYPE.addPropertyValue(m, MULTI_VALUE_KEY, TheMovieDbOntology.CONCEPT_TYPE_PERSON, visibility);
        LumifyProperties.SOURCE.addPropertyValue(m, MULTI_VALUE_KEY, SOURCE, visibility);
        StreamingPropertyValue rawValue = new StreamingPropertyValue(new ByteArrayInputStream(personJson.toString().getBytes()), byte[].class);
        rawValue.store(true);
        rawValue.searchIndex(false);
        LumifyProperties.RAW.addPropertyValue(m, MULTI_VALUE_KEY, rawValue, visibility);
        LumifyProperties.TITLE.addPropertyValue(m, MULTI_VALUE_KEY, name, visibility);

        String biography = personJson.optString("biography");
        if (biography != null) {
            Metadata metadata = new Metadata();
            metadata.add(LumifyProperties.META_DATA_TEXT_DESCRIPTION, "Biography", defaultVisibility);
            metadata.add(LumifyProperties.META_DATA_MIME_TYPE, "text/plain", defaultVisibility);
            StreamingPropertyValue value = new StreamingPropertyValue(new ByteArrayInputStream(biography.getBytes()), String.class);
            LumifyProperties.TEXT.addPropertyValue(m, MULTI_VALUE_KEY, value, metadata, visibility);
        }

        String birthDateString = personJson.optString("birthday");
        if (birthDateString != null && birthDateString.length() > 0) {
            Date birthDate = parseDate(birthDateString);
            TheMovieDbOntology.BIRTHDATE.addPropertyValue(m, MULTI_VALUE_KEY, birthDate, visibility);
        }

        String deathDateString = personJson.optString("deathday");
        if (deathDateString != null && deathDateString.length() > 0) {
            Date deathDate = parseDate(deathDateString);
            TheMovieDbOntology.DEATH_DATE.addPropertyValue(m, MULTI_VALUE_KEY, deathDate, visibility);
        }

        JSONArray akas = personJson.optJSONArray("also_known_as");
        if (akas != null) {
            for (int i = 0; i < akas.length(); i++) {
                String aka = akas.getString(i);
                TheMovieDbOntology.ALSO_KNOWN_AS.addPropertyValue(m, "aka" + i, aka, visibility);
            }
        }

        Vertex personVertex = m.save(authorizations);

        processPersonCredits(personId, personJson, personVertex);

        context.getCounter(TheMovieDbImportCounters.PERSONS_PROCESSED).increment(1);
    }

    private void processPersonCredits(int personId, JSONObject personJson, Vertex personVertex) {
        JSONObject combinedCredits = personJson.getJSONObject("combined_credits");
        JSONArray cast = combinedCredits.getJSONArray("cast");
        for (int i = 0; i < cast.length(); i++) {
            JSONObject movieJson = cast.getJSONObject(i);
            String mediaType = movieJson.getString("media_type");
            if (!mediaType.equals("movie")) {
                continue;
            }
            int movieId = movieJson.getInt("id");
            VertexBuilder movieMutation = prepareVertex(TheMovieDbOntology.getMovieVertexId(movieId), visibility);
            LumifyProperties.CONCEPT_TYPE.addPropertyValue(movieMutation, MULTI_VALUE_KEY, TheMovieDbOntology.CONCEPT_TYPE_MOVIE, visibility);
            LumifyProperties.SOURCE.addPropertyValue(movieMutation, MULTI_VALUE_KEY, SOURCE, visibility);
            String title = movieJson.optString("title");
            if (title != null && title.length() > 0) {
                LumifyProperties.TITLE.addPropertyValue(movieMutation, MULTI_VALUE_KEY, title, visibility);
            }
            Vertex movieVertex = movieMutation.save(authorizations);

            addEdge(TheMovieDbOntology.getStarredInEdgeId(personId, movieId), personVertex, movieVertex, TheMovieDbOntology.EDGE_LABEL_STARRED_IN, visibility, authorizations);
        }
    }

    private void mapMovie(int movieId, JSONObject movieJson, Context context) throws ParseException {
        String title = movieJson.getString("title");
        String vertexId = TheMovieDbOntology.getMovieVertexId(movieId);
        String sourceUrl = "http://www.themoviedb.org/movie/" + movieId;

        VertexBuilder m = prepareVertex(vertexId, visibility);
        LumifyProperties.CONCEPT_TYPE.addPropertyValue(m, MULTI_VALUE_KEY, TheMovieDbOntology.CONCEPT_TYPE_MOVIE, visibility);
        LumifyProperties.SOURCE.addPropertyValue(m, MULTI_VALUE_KEY, SOURCE, visibility);
        LumifyProperties.SOURCE_URL.addPropertyValue(m, MULTI_VALUE_KEY, sourceUrl, visibility);
        StreamingPropertyValue rawValue = new StreamingPropertyValue(new ByteArrayInputStream(movieJson.toString().getBytes()), byte[].class);
        rawValue.store(true);
        rawValue.searchIndex(false);
        LumifyProperties.RAW.addPropertyValue(m, MULTI_VALUE_KEY, rawValue, visibility);
        LumifyProperties.TITLE.addPropertyValue(m, MULTI_VALUE_KEY, title, visibility);

        String releaseDateString = movieJson.optString("release_date");
        if (releaseDateString != null && releaseDateString.length() > 0) {
            Date releaseDate = parseDate(releaseDateString);
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
            Metadata metadata = new Metadata();
            metadata.add(LumifyProperties.META_DATA_TEXT_DESCRIPTION, "Overview", defaultVisibility);
            metadata.add(LumifyProperties.META_DATA_MIME_TYPE, "text/plain", defaultVisibility);
            StreamingPropertyValue value = new StreamingPropertyValue(new ByteArrayInputStream(overview.getBytes()), String.class);
            LumifyProperties.TEXT.addPropertyValue(m, MULTI_VALUE_KEY, value, metadata, visibility);
        }

        String tagLine = movieJson.optString("tagline");
        if (tagLine != null && tagLine.length() > 0) {
            TheMovieDbOntology.TAG_LINE.addPropertyValue(m, MULTI_VALUE_KEY, tagLine, visibility);
        }

        Vertex movieVertex = m.save(authorizations);

        processMovieCredits(movieId, movieJson, movieVertex);
        processMovieProductionCompanies(movieId, movieJson, movieVertex);

        context.getCounter(TheMovieDbImportCounters.MOVIES_PROCESSED).increment(1);
    }

    private void processMovieProductionCompanies(int movieId, JSONObject movieJson, Vertex movieVertex) {
        JSONArray productionCompanies = movieJson.optJSONArray("production_companies");
        if (productionCompanies != null) {
            for (int i = 0; i < productionCompanies.length(); i++) {
                JSONObject productionCompany = productionCompanies.getJSONObject(i);
                int productionCompanyId = productionCompany.getInt("id");
                String sourceUrl = "http://www.themoviedb.org/company/" + productionCompanyId;
                VertexBuilder productionCompanyMutation = prepareVertex(TheMovieDbOntology.getProductionCompanyVertexId(productionCompanyId), visibility);
                LumifyProperties.CONCEPT_TYPE.addPropertyValue(productionCompanyMutation, MULTI_VALUE_KEY, TheMovieDbOntology.CONCEPT_TYPE_PRODUCTION_COMPANY, visibility);
                LumifyProperties.SOURCE.addPropertyValue(productionCompanyMutation, MULTI_VALUE_KEY, SOURCE, visibility);
                LumifyProperties.SOURCE_URL.addPropertyValue(productionCompanyMutation, MULTI_VALUE_KEY, sourceUrl, visibility);
                String name = productionCompany.optString("name");
                if (name != null && name.length() > 0) {
                    LumifyProperties.TITLE.addPropertyValue(productionCompanyMutation, MULTI_VALUE_KEY, name, visibility);
                }
                Vertex productionCompanyVertex = productionCompanyMutation.save(authorizations);
                addEdge(TheMovieDbOntology.getProductionCompanyProducedEdgeId(productionCompanyId, movieId), productionCompanyVertex, movieVertex, TheMovieDbOntology.EDGE_LABEL_PRODUCED, visibility, authorizations);
            }
        }
    }

    private void processMovieCredits(int movieId, JSONObject movieJson, Vertex movieVertex) {
        JSONObject credits = movieJson.getJSONObject("credits");
        JSONArray cast = credits.getJSONArray("cast");
        for (int i = 0; i < cast.length(); i++) {
            JSONObject castJson = cast.getJSONObject(i);
            int personId = castJson.getInt("id");
            String sourceUrl = "http://www.themoviedb.org/person/" + personId;
            VertexBuilder personMutation = prepareVertex(TheMovieDbOntology.getPersonVertexId(personId), visibility);
            LumifyProperties.CONCEPT_TYPE.addPropertyValue(personMutation, MULTI_VALUE_KEY, TheMovieDbOntology.CONCEPT_TYPE_PERSON, visibility);
            LumifyProperties.SOURCE.addPropertyValue(personMutation, MULTI_VALUE_KEY, SOURCE, visibility);
            LumifyProperties.SOURCE_URL.addPropertyValue(personMutation, MULTI_VALUE_KEY, sourceUrl, visibility);
            String name = castJson.optString("name");
            if (name != null && name.length() > 0) {
                LumifyProperties.TITLE.addPropertyValue(personMutation, MULTI_VALUE_KEY, name, visibility);
            }
            Vertex personVertex = personMutation.save(authorizations);
            addEdge(TheMovieDbOntology.getStarredInEdgeId(personId, movieId), personVertex, movieVertex, TheMovieDbOntology.EDGE_LABEL_STARRED_IN, visibility, authorizations);

            String character = castJson.optString("character");
            if (character != null && character.length() > 0) {
                String roleId = TheMovieDbOntology.getRoleId(personId, movieId);
                VertexBuilder roleMutation = prepareVertex(TheMovieDbOntology.getRoleVertexId(roleId), visibility);
                LumifyProperties.CONCEPT_TYPE.addPropertyValue(roleMutation, MULTI_VALUE_KEY, TheMovieDbOntology.CONCEPT_TYPE_ROLE, visibility);
                LumifyProperties.SOURCE.addPropertyValue(roleMutation, MULTI_VALUE_KEY, SOURCE, visibility);
                LumifyProperties.TITLE.addPropertyValue(roleMutation, MULTI_VALUE_KEY, character, visibility);
                Vertex roleVertex = roleMutation.save(authorizations);

                addEdge(TheMovieDbOntology.getPlayedEdgeId(personId, roleId), personVertex, roleVertex, TheMovieDbOntology.EDGE_LABEL_PLAYED, visibility, authorizations);
                addEdge(TheMovieDbOntology.getHasRoleEdgeId(movieId, roleId), movieVertex, roleVertex, TheMovieDbOntology.EDGE_LABEL_HAS_ROLE, visibility, authorizations);
            }
        }
    }

    private Date parseDate(String str) throws ParseException {
        try {
            return DATE_FORMAT.parse(str);
        } catch (ParseException p) {
            return DATE_YEAR_FORMAT.parse(str);
        }
    }
}
