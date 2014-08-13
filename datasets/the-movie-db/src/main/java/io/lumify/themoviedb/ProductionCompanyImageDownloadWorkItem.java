package io.lumify.themoviedb;

import java.io.IOException;

public class ProductionCompanyImageDownloadWorkItem extends ImageDownloadWorkItem {
    public ProductionCompanyImageDownloadWorkItem(int productionCompanyId, String logoImagePath) {
        super(productionCompanyId, logoImagePath);
    }

    @Override
    public void writeImage(TheMovieDbImport theMovieDbImport, byte[] imageData) throws IOException {
        theMovieDbImport.writeProductionCompanyLogo(getEntityId(), getImagePath(), imageData);
    }
}
