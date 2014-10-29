package io.lumify.palantir.dataImport.formatFunctions;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;

public class AddSsnDashesFormatFunction extends FormatFunctionBase {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(AddSsnDashesFormatFunction.class);

    @Override
    public String format(String value) {
        if (value.length() == 9) {
            return value.substring(0, 3) + "-" + value.substring(3, 5) + "-" + value.substring(5);
        }
        LOGGER.warn("Invalid SSN to add ssn dashes to: %s", value);
        return value;
    }
}
