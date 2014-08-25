package io.lumify.storm.util;

import io.lumify.core.exception.LumifyJsonParseException;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.util.JSONUtil;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.core.util.ProcessRunner;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class FFprobeExecutor {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(FFprobeExecutor.class);

    public static JSONObject getJson(ProcessRunner processRunner, GraphPropertyWorkData data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String outString = null;
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
            outString = new String(out.toByteArray());
            JSONObject json = JSONUtil.parse(outString);
            return json;
        } catch (LumifyJsonParseException e) {
            LOGGER.error("outstring, " + outString + ", is not a valid json");
            return null;
        } catch (IOException e) {
            LOGGER.debug("IOException occurred. Could not retrieve JSONObject using ffprobe.");
            return null;
        } catch (InterruptedException e) {
            LOGGER.debug("InterruptedException occurred. Could not retrieve JSONObject using ffprobe.");
            return null;
        }

    }
}