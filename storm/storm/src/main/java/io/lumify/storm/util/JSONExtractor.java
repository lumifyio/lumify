package io.lumify.storm.util;

import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.core.util.ProcessRunner;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class JSONExtractor {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(JSONExtractor.class);

    public static JSONObject retrieveJSONObjectUsingFFPROBE(ProcessRunner processRunner, GraphPropertyWorkData data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            processRunner.execute(
                    "ffprobe",
                    new String[]{
                            "-v", "quiet",
                            "-print_format", "json",
                            "-show_format",
                            "-show_streams",
                            data.getLocalFile().getAbsolutePath()
                    },
                    out,
                    data.getLocalFile().getAbsolutePath() + ": "
            );
            String outString = new String(out.toByteArray());
            JSONObject json = new JSONObject(outString);
            //LOGGER.debug("info for %s:\n%s", data.getLocalFile().getAbsolutePath(), json.toString(2));
            return json;
        } catch (IOException e) {
            LOGGER.info("Could not retrieve JSONObject using ffprobe.");
            return null;
        } catch (InterruptedException e) {
            LOGGER.info("Could not retrieve JSONObject using ffprobe.");
            return null;
        }

    }
}
