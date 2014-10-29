package io.lumify.palantir.dataImport.formatFunctions;

public class LowercaseFormatFunction extends FormatFunctionBase {
    @Override
    public String format(String value) {
        return value.toLowerCase();
    }
}
