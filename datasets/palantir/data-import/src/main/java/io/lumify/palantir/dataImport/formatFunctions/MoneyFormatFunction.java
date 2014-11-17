package io.lumify.palantir.dataImport.formatFunctions;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class MoneyFormatFunction extends FormatFunctionBase {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(MoneyFormatFunction.class);
    private static final DecimalFormat DECIMAL_NUMBER_FORMAT;
    private static final DecimalFormat INTEGER_NUMBER_FORMAT;

    static {
        DECIMAL_NUMBER_FORMAT = (DecimalFormat) NumberFormat.getInstance();
        DECIMAL_NUMBER_FORMAT.setGroupingUsed(true);
        DECIMAL_NUMBER_FORMAT.setMaximumFractionDigits(2);
        DECIMAL_NUMBER_FORMAT.setMinimumFractionDigits(2);

        INTEGER_NUMBER_FORMAT = (DecimalFormat) NumberFormat.getInstance();
        INTEGER_NUMBER_FORMAT.setGroupingUsed(true);
        INTEGER_NUMBER_FORMAT.setMaximumFractionDigits(0);
    }

    @Override
    public String format(String value) {
        try {
            if (value.indexOf('.') >= 0) {
                double d = Double.parseDouble(value);
                return DECIMAL_NUMBER_FORMAT.format(d);
            }

            int i = Integer.parseInt(value);
            return INTEGER_NUMBER_FORMAT.format(i);
        } catch (Exception ex) {
            LOGGER.error("Could not format number: %s", value, ex);
            return value;
        }
    }
}
