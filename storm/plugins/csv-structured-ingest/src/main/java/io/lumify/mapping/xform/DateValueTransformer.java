package io.lumify.mapping.xform;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * This transformer uses a provided format to parse the input string
 * as a Date, then returns the Long value representing the time in
 * milliseconds from the epoch (Date.getTime()).  If no format is
 * provided, the transformer will attempt to apply the following default
 * formats in the order they are shown.  If the input string cannot be
 * parsed, this transformer will return null.
 * <p>
 * Default Formats:
 * <ul>
 *   <li>MM/dd/yyyy HH:mm:ss</li>
 *   <li>MM/dd/yyyy</li>
 * </ul>
 */
@JsonTypeName("date")
public class DateValueTransformer implements ValueTransformer<Long> {
    /**
     * The default date formats, in the order they will be attempted.
     * <ul>
     *   <li>MM/dd/yyyy HH:mm:ss</li>
     *   <li>MM/dd/yyyy</li>
     * </ul>
     */
    public static final List<String> DEFAULT_DATE_FORMATS = Collections.unmodifiableList(Arrays.asList(
            "MM/dd/yyyy HH:mm:ss",
            "MM/dd/yyyy"
    ));

    /**
     * The date format.
     */
    private final String format;

    /**
     * Create a new DateValueTransformer using the default formats.
     */
    public DateValueTransformer() {
        this(null);
    }

    /**
     * Create a new DateValueTransformer.
     * @param fmt optionally, the date format for this column
     */
    public DateValueTransformer(final String fmt) {
        this.format = fmt != null && !fmt.trim().isEmpty() ? fmt.trim() : null;
    }

    public String getFormat() {
        return format;
    }

    @Override
    public Long transform(final String input) {
        Date date = null;
        if (input != null && !input.trim().isEmpty()) {
            if (format == null) {
                for (String fmt : DEFAULT_DATE_FORMATS) {
                    try {
                        date = new SimpleDateFormat(fmt).parse(input);
                        break;
                    } catch (ParseException pe) {
                        date = null;
                    }
                }
            } else {
                try {
                    date = new SimpleDateFormat(format).parse(input);
                } catch (ParseException pe) {
                    date = null;
                }
            }
        }
        return date != null ? date.getTime() : null;
    }
}
