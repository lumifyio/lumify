package io.lumify.gpw.util;

import io.lumify.core.exception.LumifyJsonParseException;
import io.lumify.core.util.JSONUtil;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.core.util.ProcessRunner;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

public class FFprobeExecutor {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(FFprobeExecutor.class);

    public static JSONObject getJson(ProcessRunner processRunner, String absolutePath) {
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
                            absolutePath
                    },
                    byteArrayOutputStream,
                    absolutePath + ": "
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