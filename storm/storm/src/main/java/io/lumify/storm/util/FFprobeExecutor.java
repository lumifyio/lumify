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
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        String output = null;
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
                    byteArrayOutputStream,
                    data.getLocalFile().getAbsolutePath() + ": "
            );
            output = new String(byteArrayOutputStream.toByteArray());
            return JSONUtil.parse(output);
        } catch (LumifyJsonParseException e) {
            LOGGER.error("unable to parse ffprobe output: [%s]", output);
        } catch (Exception e) {
            LOGGER.error("exception running ffprobe", e);
        }

        return null;
    }
}