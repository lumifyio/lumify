package io.lumify.themoviedb.download;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;

public class PersonDownloadWorkItem extends WorkItem {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(PersonDownloadWorkItem.class);
    private final int personId;

    public PersonDownloadWorkItem(int personId) {
        this.personId = personId;
    }

    @Override
    public boolean process(TheMovieDbDownload theMovieDbDownload) throws IOException, ParseException {
        if (theMovieDbDownload.hasPersonInCache(personId)) {
            return false;
        }
        LOGGER.debug("Downloading actor: %d", personId);
        JSONObject personJson = theMovieDbDownload.getTheMovieDb().getPersonInfo(personId);
        theMovieDbDownload.writePerson(personId, personJson);
        return true;
    }

    @Override
    public String toString() {
        return "ActorDownloadWorkItem{" +
                "personId='" + personId + '\'' +
                '}';
    }
}
