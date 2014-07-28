package io.lumify.imageMetadataHelper;

import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

public class LeftoverMetadataExtractor {
    public static String getAllMetadata(Metadata metadata){
        String temp = "";
        //Print the metadata to System.out.
        for (Directory directory : metadata.getDirectories()) {
            for (Tag tag : directory.getTags()) {
                System.out.println(tag);
                String tagDirectoryName = "\n[" + tag.getDirectoryName() + "]";
                String tagNameAndValue = " " + tag.getTagName() + " - " + tag.getDescription();
                temp = temp + tagDirectoryName + tagNameAndValue;
            }
        }
        return temp;
    }
}