package io.lumify.themoviedb;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;

public class MovieDownloadWorkItem extends WorkItem {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(MovieDownloadWorkItem.class);
    private final int movieId;

    public MovieDownloadWorkItem(int movieId) {
        this.movieId = movieId;
    }

    @Override
    public boolean process(TheMovieDbImport theMovieDbImport) throws IOException, ParseException {
        if (theMovieDbImport.hasMovieInCache(movieId)) {
            return false;
        }
        LOGGER.debug("Downloading movie: %d", movieId);
        JSONObject movieJson = theMovieDbImport.getTheMovieDb().getMovieInfo(movieId);
        theMovieDbImport.writeMovie(movieId, movieJson);
        return true;
    }

    @Override
    public String toString() {
        return "MovieDownloadWorkItem{" +
                "movieId='" + movieId + '\'' +
                '}';
    }
}
