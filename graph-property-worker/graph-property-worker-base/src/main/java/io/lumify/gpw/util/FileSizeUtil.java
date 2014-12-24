package io.lumify.gpw.util;

import java.io.File;

public class FileSizeUtil {

    public static Integer getSize(File file){
        if (file == null){
            return null;
        }

        Long length = file.length();
        if (length <= Integer.MAX_VALUE) {
            return length.intValue();
        }

        return null;
    }
}
