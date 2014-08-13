package io.lumify.themoviedb;

import io.lumify.core.model.properties.types.DateLumifyProperty;
import io.lumify.core.model.properties.types.DoubleLumifyProperty;
import io.lumify.core.model.properties.types.IntegerLumifyProperty;
import io.lumify.core.model.properties.types.StringLumifyProperty;

public class TheMovieDbOntology {
    public static final String EDGE_LABEL_PRODUCED = "http://lumify.io/themoviedb#produced";
    public static final String EDGE_LABEL_HAS_PROFILE_IMAGE = "http://lumify.io/themoviedb#hasProfileImage";
    public static final String EDGE_LABEL_HAS_POSTER_IMAGE = "http://lumify.io/themoviedb#hasPosterImage";
    public static final String EDGE_LABEL_STARRED_IN = "http://lumify.io/themoviedb#starredin";
    public static final String EDGE_LABEL_HAS_LOGO = "http://lumify.io/themoviedb#hasLogo";

    public static final String CONCEPT_TYPE_THE_MOVIE_DB = "http://lumify.io/themoviedb#the-movie-db-root";
    public static final String CONCEPT_TYPE_PERSON = "http://lumify.io/themoviedb#person";
    public static final String CONCEPT_TYPE_PROFILE_IMAGE = "http://lumify.io/themoviedb#profileimage";
    public static final String CONCEPT_TYPE_MOVIE = "http://lumify.io/themoviedb#movie";
    public static final String CONCEPT_TYPE_LOGO = "http://lumify.io/themoviedb#logo";
    public static final String CONCEPT_TYPE_PRODUCTION_COMPANY = "http://lumify.io/themoviedb#productionCompany";
    public static final String CONCEPT_TYPE_POSTER_IMAGE = "http://lumify.io/themoviedb#posterImage";

    public static final DoubleLumifyProperty RUNTIME = new DoubleLumifyProperty("http://lumify.io/themoviedb#runtime");
    public static final IntegerLumifyProperty REVENUE = new IntegerLumifyProperty("http://lumify.io/themoviedb#revenue");
    public static final StringLumifyProperty GENRE = new StringLumifyProperty("http://lumify.io/themoviedb#genre");
    public static final DateLumifyProperty BIRTHDATE = new DateLumifyProperty("http://lumify.io/themoviedb#birthdate");
    public static final StringLumifyProperty TAG_LINE = new StringLumifyProperty("http://lumify.io/themoviedb#tagLine");
    public static final DateLumifyProperty DEATH_DATE = new DateLumifyProperty("http://lumify.io/themoviedb#deathdate");
    public static final IntegerLumifyProperty BUDGET = new IntegerLumifyProperty("http://lumify.io/themoviedb#budge");
    public static final StringLumifyProperty OVERVIEW = new StringLumifyProperty("http://lumify.io/themoviedb#overview");
    public static final StringLumifyProperty ALSO_KNOWN_AS = new StringLumifyProperty("http://lumify.io/themoviedb#aka");
    public static final DateLumifyProperty RELEASE_DATE = new DateLumifyProperty("http://lumify.io/themoviedb#releaseDate");
    public static final StringLumifyProperty BIOGRAPHY = new StringLumifyProperty("http://lumify.io/themoviedb#biography");

    public static String getPersonHasProfileImageEdgeId(int personId, String profileImagePath) {
        return "MOVIEDB_PERSON_HAS_PROFILE_IMAGE_" + personId + "_" + profileImagePath;
    }

    public static String getProfileImageVertexId(String profileImagePath) {
        return "MOVIEDB_PROFILE_IMAGE_" + profileImagePath;
    }

    public static String getStarredInEdgeId(int personId, int movieId) {
        return "MOVIEDB_STARRED_" + personId + "_" + movieId;
    }

    public static String getMovieVertexId(int movieId) {
        return "MOVIEDB_MOVIE_" + movieId;
    }

    public static String getPersonVertexId(int personId) {
        return "MOVIEDB_PERSON_" + personId;
    }

    public static String getPosterImageVertexId(String posterImagePath) {
        return "MOVIEDB_POSTER_IMAGE_" + posterImagePath;
    }

    public static String getMovieHasPosterImageEdgeId(int movieId, String posterImagePath) {
        return "MOVIEDB_MOVIE_HAS_POSTER_IMAGE_" + movieId + "_" + posterImagePath;
    }

    public static String getProductionCompanyVertexId(int productionCompanyId) {
        return "MOVIEDB_PRODCO_" + productionCompanyId;
    }

    public static String getProductionCompanyProducedEdgeId(int productionCompanyId, int movieId) {
        return "MOVIEDB_PRODCO_PRODUCED_" + productionCompanyId + "_" + movieId;
    }

    public static String getProductionCompanyImageVertexId(String logoImagePath) {
        return "MOVIEDB_LOGO_" + logoImagePath;
    }

    public static String getProductionCompanyHasLogoEdgeId(int productionCompanyId, String logoImagePath) {
        return "MOVIEDB_PRODCO_HAS_LOGO_" + productionCompanyId + "_" + logoImagePath;
    }
}
