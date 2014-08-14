package io.lumify.themoviedb.download;

public abstract class WorkItem {
    public abstract boolean process(TheMovieDbDownload theMovieDbDownload) throws Exception;
}
