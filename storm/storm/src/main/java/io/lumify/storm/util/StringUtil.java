package io.lumify.storm.util;

import java.util.ArrayList;
import java.util.List;

public class StringUtil {
    public static String[] createStringArrayFromList(List<String> list) {
        String[] stringArray = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            stringArray[i] = list.get(i);
        }
        return stringArray;
    }

    public static String[] removeNullsFromStringArray(String[] array) {
        ArrayList<String> arrayList = new ArrayList<String>();
        for (int i = 0; i < array.length; i++) {
            if (array[i] != null && !array[i].equals("")) {
                arrayList.add(array[i]);
            }
        }
        return createStringArrayFromList(arrayList);
    }
}
