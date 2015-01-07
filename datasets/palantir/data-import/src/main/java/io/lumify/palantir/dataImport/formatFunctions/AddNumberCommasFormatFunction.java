package io.lumify.palantir.dataImport.formatFunctions;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class AddNumberCommasFormatFunction extends FormatFunctionBase {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(AddNumberCommasFormatFunction.class);
    private static final DecimalFormat NUMBER_FORMAT;

    static {
        NUMBER_FORMAT = (DecimalFormat) NumberFormat.getInstance();
        NUMBER_FORMAT.setGroupingUsed(true);
        NUMBER_FORMAT.setMaximumFractionDigits(10);
    }

    @Override
    public String format(String value) {
        try {
            if (value.indexOf('.') >= 0) {
                double d = Double.parseDouble(value);
                return NUMBER_FORMAT.format(d);
            }

            int i = Integer.parseInt(value);
            return NUMBER_FORMAT.format(i);
        } catch (Exception ex) {
            LOGGER.error("Could not format number: %s", value, ex);
            return value;
        }
    }
}
