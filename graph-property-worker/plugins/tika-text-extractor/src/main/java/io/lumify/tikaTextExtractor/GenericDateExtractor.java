package io.lumify.tikaTextExtractor;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GenericDateExtractor {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(GenericDateExtractor.class);
    private static List<String> DATE_FORMATS = new ArrayList<>();

    static {
        DATE_FORMATS.add("yyyy-MM-dd'T'HH:mm:ssX");
        DATE_FORMATS.add("yyyy-MM-dd'T'HH:mm:ssz");
        DATE_FORMATS.add("yyyy-MM-dd'T'HH:mm:ssZ");
        DATE_FORMATS.add("EEE MMM dd HH:mm:ss z yyyy");
        DATE_FORMATS.add("'D'':'yyyyMMddHHmmss");
    }

    public static Date extractSingleDate(String dateString) {
        for (String dateFormat : DATE_FORMATS) {
            try {
                LOGGER.debug("parsing %s using %s", dateString, dateFormat);
                Date result = new SimpleDateFormat(dateFormat).parse(dateString);
                LOGGER.debug("parsing %s using %s succeeded %s", dateString, dateFormat, new SimpleDateFormat(DATE_FORMATS.get(0)).format(result));
                return result;
            } catch (ParseException e) {
                LOGGER.debug("could not parse %s using %s", dateString, dateFormat, e);
            }
        }
        return null;
    }
}
