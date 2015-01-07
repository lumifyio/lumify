package io.lumify.palantir.dataImport.formatFunctions;

public class UppercaseFormatFunction extends FormatFunctionBase {
    @Override
    public String format(String value) {
        return value.toUpperCase();
    }
}
