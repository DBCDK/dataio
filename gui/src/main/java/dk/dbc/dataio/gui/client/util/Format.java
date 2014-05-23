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
     * Creates a Submitter Pair String in the form: 'id (name)'
     *
     * @param submitterId
     * @param submitterName
     * @return A Submitter Pair String in the form: 'id (name)'
     */
    public static String submitterPairString(Long submitterId, String submitterName) {
        return new StringBuilder().append(submitterId).append(" (").append(submitterName).append(")").toString();
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

    /**
     * Creates a list of Submitter Pair Strings in the form: 'id (name)'
     *
     * @param submitterIds A list of Submitter Id's
     * @param submitterNames A list of Submitter Names
     * @return A list of Submitter Pair Strings in the form: 'id (name)'
     * @throws IllegalArgumentException if the two input lists have different lengths
     */
//    public static List<String> submitterPairStrings(List<Long> submitterIds, List<String> submitterNames) {
//        if (submitterIds.size() != submitterNames.size()) {
//            throw new IllegalArgumentException();
//        }
//        List<String> result = new ArrayList<String>();
//        Iterator<Long> idIterator = submitterIds.iterator();
//        Iterator<String> nameIterator = submitterNames.iterator();
//        while (idIterator.hasNext()) {
//            result.add(submitterPairString(idIterator.next(), nameIterator.next()));
//        }
//        return result;
//    }

}
