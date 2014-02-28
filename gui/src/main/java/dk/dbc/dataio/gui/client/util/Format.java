/*
 *
 * Utility functions for formatting texts to be displayed
 *
 */

package dk.dbc.dataio.gui.client.util;

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
        String result = "";
        for (String parameter: parameters) {
            if (result.isEmpty()) {
                result = parameter;
            } else {
                result += ", " + parameter;
            }
        }
        return result;
    }

}
