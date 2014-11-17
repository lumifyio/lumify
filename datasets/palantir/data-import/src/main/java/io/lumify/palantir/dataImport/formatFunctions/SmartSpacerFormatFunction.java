package io.lumify.palantir.dataImport.formatFunctions;

public class SmartSpacerFormatFunction extends FormatFunctionBase {
    @Override
    public String format(String value) {
        return value + " ";
    }
}
