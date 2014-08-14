package io.lumify.themoviedb.download;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;

public class ImageDownloadWorkItem extends WorkItem {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ImageDownloadWorkItem.class);
    private final String imagePath;

    public ImageDownloadWorkItem(String imagePath) {
        this.imagePath = imagePath;
    }

    @Override
    public boolean process(TheMovieDbDownload theMovieDbDownload) throws Exception {
        if (theMovieDbDownload.hasImageInCache(imagePath)) {
            return false;
        }
        LOGGER.debug("Downloading image: %s", imagePath);
        byte[] imageData = theMovieDbDownload.getTheMovieDb().getImage(imagePath);
        theMovieDbDownload.writeImage(imagePath, imageData);
        return true;
    }

    @Override
    public String toString() {
        return "ImageDownloadWorkItem{" +
                "imagePath='" + imagePath + '\'' +
                '}';
    }
}
