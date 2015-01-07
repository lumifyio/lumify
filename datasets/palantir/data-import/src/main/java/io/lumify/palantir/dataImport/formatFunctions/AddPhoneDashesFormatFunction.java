package io.lumify.palantir.dataImport.formatFunctions;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;

public class AddPhoneDashesFormatFunction extends FormatFunctionBase {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(AddPhoneDashesFormatFunction.class);

    @Override
    public String format(String value) {
        if (value.length() == 10) {
            return value.substring(0, 3) + "-" + value.substring(3, 6) + "-" + value.substring(6);
        }
        if (value.length() == 7) {
            return value.substring(0, 3) + "-" + value.substring(3);
        }
        LOGGER.warn("Invalid phone number: %s", value);
        return value;
    }
}
