package io.lumify.themoviedb.download;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.json.JSONObject;

public class ProductionCompanyDownloadWorkItem extends WorkItem {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ProductionCompanyDownloadWorkItem.class);
    private final int productionCompanyId;

    public ProductionCompanyDownloadWorkItem(int productionCompanyId) {
        this.productionCompanyId = productionCompanyId;
    }

    @Override
    public boolean process(TheMovieDbDownload theMovieDbDownload) throws Exception {
        if (theMovieDbDownload.hasProductionCompanyInCache(productionCompanyId)) {
            return false;
        }
        LOGGER.debug("Downloading production company: %d", productionCompanyId);
        JSONObject productionCompanyJson = theMovieDbDownload.getTheMovieDb().getProductionCompanyInfo(productionCompanyId);
        theMovieDbDownload.writeProductionCompany(productionCompanyId, productionCompanyJson);
        return true;
    }
}
