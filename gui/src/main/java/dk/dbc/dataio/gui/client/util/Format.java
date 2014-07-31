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
     * @param mainString
     * @param inBracketsString
     * @return A String Pair in the form: 'mainString (inBracketsString)'
     */
    public static String inBracketsPairString(String mainString, String inBracketsString) {
        return new StringBuilder().append(mainString).append(" (").append(inBracketsString).append(")").toString();
    }

    /**
     *
     * @param time long value, the date that should be converted into longDateString format
     * @return A string representation of date
     */
    public static String getLongDateTimeFormat(long time){
        DateTimeFormat dateTimeFormat = DateTimeFormat.getFormat("yyyy-MM-dd HH:mm:ss");
        return dateTimeFormat.format(new Date(time));
    }
}
