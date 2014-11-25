/*
 *
 * Utility functions for formatting texts to be displayed
 *
 */

package dk.dbc.dataio.gui.client.util;

import com.google.gwt.i18n.shared.DateTimeFormat;

import java.util.Date;
import java.util.List;

/**
 *
 * Format class
 *
 */
public final class Format {
    private final static String LONG_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    // Private constructor to avoid instantiation
    private Format() {
    }

    /**
     * comma separates a list of String
     *
     * @param parameters The list of strings
     * @return The comma separated list
     *
     */
    public static String commaSeparate(List<String> parameters) {
        StringBuilder result = new StringBuilder();
        for (String parameter: parameters) {
            if (result.length() == 0) {
                result.append(parameter);
            } else {
                result.append(", ").append(parameter);
            }
        }
        return result.toString();
    }

    /**
     * Pair two Strings in the form: 'mainString (inBracketsString)'
     *
     * @param mainString The Main string
     * @param inBracketsString The InBracket string
     * @return A String Pair in the form: 'mainString (inBracketsString)'
     */
    public static String inBracketsPairString(String mainString, String inBracketsString) {
        return new StringBuilder().append(mainString).append(" (").append(inBracketsString).append(")").toString();
    }

    /**
     * Formats a long date value to a text String
     * @param date long value, the date that should be converted into longDateString format
     * @return A string representation of date
     */
    public static String getLongDateTimeFormat(long date){
        return formatDate(date, LONG_DATE_TIME_FORMAT);
    }

    /**
     *
     * @param date A textual representation of the date
     * @return A long representation of date
     */
    public static long parseLongDate(String date){
        return parseDate(date, LONG_DATE_TIME_FORMAT);
    }


    /*
     * Private methods
     */
    private static String formatDate(long date, String format) {
        DateTimeFormat dateTimeFormat = DateTimeFormat.getFormat(format);
        return dateTimeFormat.format(new Date(date));
    }

    private static long parseDate(String date, String format) {
        DateTimeFormat dateTimeFormat = DateTimeFormat.getFormat(format);
        return dateTimeFormat.parse(date).getTime();
    }
}
