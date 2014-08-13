package io.lumify.themoviedb;

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
    public boolean process(TheMovieDbImport theMovieDbImport) throws IOException, ParseException {
        if (theMovieDbImport.hasPersonInCache(personId)) {
            return false;
        }
        LOGGER.debug("Downloading actor: %d", personId);
        JSONObject personJson = theMovieDbImport.getTheMovieDb().getPersonInfo(personId);
        theMovieDbImport.writePerson(personId, personJson);
        return true;
    }

    @Override
    public String toString() {
        return "ActorDownloadWorkItem{" +
                "personId='" + personId + '\'' +
                '}';
    }
}
