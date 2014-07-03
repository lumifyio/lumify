package io.lumify.tikaTextExtractor;

import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class GenericDateExtractor {

    public static Date extractSingleDate(String dateString) {
        Parser parser = new Parser();
        Date extractedDate;
        List<DateGroup> groups = parser.parse(dateString);

        // quick and dirty: assume first date of first group
        List<Date> dates = groups.isEmpty() ? Collections.<Date>emptyList()
                : groups.get(0).getDates();

        extractedDate = dates.isEmpty() ? null : dates.get(0);

        return extractedDate;
    }
}
