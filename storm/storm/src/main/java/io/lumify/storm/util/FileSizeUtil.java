package io.lumify.storm.util;

import java.io.File;

public class FileSizeUtil {

    public static Integer extractFileSize(File file){
        Long fileSizeLong = file.length();
        if (fileSizeLong != null && fileSizeLong > 0 && fileSizeLong < Integer.MAX_VALUE) {
            Integer fileSize = fileSizeLong.intValue();
            return fileSize;
        }

        return null;
    }
}
