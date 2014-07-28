package io.lumify.imageMetadataHelper;

import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import org.json.JSONObject;

public class LeftoverMetadataExtractor {

    public static JSONObject getAsJSON(Metadata metadata) {
        JSONObject json = new JSONObject();
        for (Directory directory : metadata.getDirectories()) {
            if (directory != null) {
                JSONObject directoryJSON = new JSONObject();
                for (Tag tag : directory.getTags()) {
                    if (tag != null) {
                        directoryJSON.accumulate(tag.getTagName(), tag.getDescription());
                    }
                }
                String directoryName = directory.getName();
                json.accumulate(directoryName, directoryJSON);
            }
        }
        return json;
    }
}