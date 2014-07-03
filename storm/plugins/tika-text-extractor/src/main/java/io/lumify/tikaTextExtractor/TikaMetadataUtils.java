package io.lumify.tikaTextExtractor;

import org.apache.tika.metadata.Metadata;

import java.util.List;

/**
 * Utility to determine if any keys in a list exist in the provided metadata
 * map. Limited to ASCII at this point, and probably not optimal
 */
public class TikaMetadataUtils {
    public static String findKey(List<String> potentialKeys, Metadata metadata) {
        String discoveredKey = null;
        for (String key : potentialKeys) {
            for (String name : metadata.names()) {
                if (key.equalsIgnoreCase(name)) {
                    discoveredKey = name;
                    break;
                }
            }

            if (discoveredKey != null) {
                break;
            }
        }

        return discoveredKey;
    }

}
