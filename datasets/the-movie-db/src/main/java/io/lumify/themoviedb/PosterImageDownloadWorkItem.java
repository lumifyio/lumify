package io.lumify.themoviedb;

import java.io.IOException;

public class PosterImageDownloadWorkItem extends ImageDownloadWorkItem {
    public PosterImageDownloadWorkItem(int movieId, String posterImagePath) {
        super(movieId, posterImagePath);
    }

    @Override
    public void writeImage(TheMovieDbImport theMovieDbImport, byte[] posterImage) throws IOException {
        theMovieDbImport.writePosterImage(getEntityId(), getImagePath(), posterImage);
    }
}
