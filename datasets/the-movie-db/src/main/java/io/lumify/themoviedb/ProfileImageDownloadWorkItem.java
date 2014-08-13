package io.lumify.themoviedb;

import java.io.IOException;

public class ProfileImageDownloadWorkItem extends ImageDownloadWorkItem {
    public ProfileImageDownloadWorkItem(int personId, String profileImage) {
        super(personId, profileImage);
    }

    @Override
    public void writeImage(TheMovieDbImport theMovieDbImport, byte[] profileImage) throws IOException {
        theMovieDbImport.writeProfileImage(getEntityId(), getImagePath(), profileImage);
    }
}
