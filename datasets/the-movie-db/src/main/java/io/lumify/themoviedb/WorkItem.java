package io.lumify.themoviedb;

public abstract class WorkItem {
    public abstract boolean process(TheMovieDbImport theMovieDbImport) throws Exception;
}
