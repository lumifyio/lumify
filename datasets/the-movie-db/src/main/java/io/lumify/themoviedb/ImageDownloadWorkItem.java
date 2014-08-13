package io.lumify.themoviedb;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;

import java.io.IOException;

public abstract class ImageDownloadWorkItem extends WorkItem {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ImageDownloadWorkItem.class);
    private final int entityId;
    private final String imagePath;

    protected ImageDownloadWorkItem(int entityId, String imagePath) {
        this.entityId = entityId;
        this.imagePath = imagePath;
    }

    @Override
    public boolean process(TheMovieDbImport theMovieDbImport) throws Exception {
        if (theMovieDbImport.hasImageInCache(imagePath)) {
            return false;
        }
        LOGGER.debug("Downloading image: %s for %s id %d", imagePath, this.getClass().getSimpleName(), entityId);
        byte[] profileImage = theMovieDbImport.getTheMovieDb().getImage(imagePath);
        writeImage(theMovieDbImport, profileImage);
        return true;
    }

    public abstract void writeImage(TheMovieDbImport theMovieDbImport, byte[] imageData) throws IOException;

    @Override
    public String toString() {
        return "ImageDownloadWorkItem{" +
                "entityId=" + entityId +
                ", imagePath='" + imagePath + '\'' +
                '}';
    }

    public int getEntityId() {
        return entityId;
    }

    public String getImagePath() {
        return imagePath;
    }
}
