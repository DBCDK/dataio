/*
 *
 * Utility functions for formatting texts to be displayed
 *
 */

package dk.dbc.dataio.gui.client.util;

import com.google.gwt.i18n.shared.DateTimeFormat;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Format class
 */
public final class Format {
    public final static String LONG_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private final static String DATAIO_GUI_VALID_INPUT_PATTERN = "[^ÆØÅæøåa-zA-z0-9+ _-]";

    // Private constructor to avoid instantiation
    private Format() {
    }

    /**
     * comma separates a list of String
     *
     * @param parameters The list of strings
     * @return The comma separated list
     */
    public static String commaSeparate(List<String> parameters) {
        StringBuilder result = new StringBuilder();
        for (String parameter : parameters) {
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
     * @param mainString       The Main string
     * @param inBracketsString The InBracket string
     * @return A String Pair in the form: 'mainString (inBracketsString)'
     */
    public static String inBracketsPairString(String mainString, String inBracketsString) {
        return mainString + " (" + inBracketsString + ")";
    }

    /**
     * Formats a long date value to a text String
     *
     * @param date long value, the date that should be converted into longDateString format
     * @return A string representation of date
     */
    public static String formatLongDate(long date) {
        return formatDate(new Date(date), LONG_DATE_TIME_FORMAT);
    }

    /**
     * Formats a Date value to a text String
     *
     * @param date Date value, the date that should be converted into longDateString format
     * @return A string representation of date
     */
    public static String formatLongDate(Date date) {
        return formatDate(date, LONG_DATE_TIME_FORMAT);
    }

    /**
     * @param date A textual representation of the date
     * @return A long representation of date
     */
    public static long parseLongDateAsLong(String date) {
        return parseDate(date, LONG_DATE_TIME_FORMAT);
    }

    /**
     * @param date A textual representation of the date
     * @return A long representation of date
     */
    public static Date parseLongDateAsDate(String date) {
        return new Date(parseDate(date, LONG_DATE_TIME_FORMAT));
    }

    /**
     * Matches an input string towards a predefined dataio pattern:
     * A-Å, 0-9, - (minus), + (plus), _(underscore)
     *
     * @param input the string to match
     * @return a list containing the matches found, empty list if no matches found
     */
    public static List<String> getDataioPatternMatches(String input) {
        return getPatternMatches(input, DATAIO_GUI_VALID_INPUT_PATTERN);
    }

    /**
     * Matches an input string with the string pattern given as input
     *
     * @param input   the string to match
     * @param pattern the pattern to which the string is matched
     * @return a list containing the matches found, empty list if no matches found
     */
    public static List<String> getPatternMatches(String input, String pattern) {
        final List<String> matches = new ArrayList<>();
        RegExp regExp = RegExp.compile(pattern, "g");
        for (MatchResult matcher = regExp.exec(input); matcher != null; matcher = regExp.exec(input)) {
            matches.add(matcher.getGroup(0));
        }
        return matches;
    }

    /**
     * Inserts the parameter in the targetString, using Macro substitution<br>
     * Example<br>
     * targetString: "Mary had a @SIZE@ lamb"<br>
     * parameterName: "SIZE"<br>
     * parameterValue: "little"<br>
     * ... gives: "Mary had a little lamb"
     *
     * @param targetString   The string, where the parameter should be put into
     * @param parameterName  The name of the parameter
     * @param parameterValue The value of the parameter
     * @return The composed string
     */
    public static String macro(String targetString, String parameterName, String parameterValue) {
        return targetString.replaceAll("@" + parameterName + "@", parameterValue);
    }

    /**
     * Capitalizes a text consisting of space delimited words - ie. make first letter of each word uppercase, remaining letters lowercase<br>
     * All whitespace character are converted to a single space character
     *
     * @param text The text to capitalize
     * @return The capitalized word
     */
    public static String capitalize(String text) {
        String sentence = text == null ? "" : text.trim();
        return Arrays.stream(sentence.split("\\s+")).map(
                w -> w.length() < 1 ? w : w.substring(0, 1).toUpperCase() + w.substring(1, w.length()).toLowerCase()
        ).collect(Collectors.joining(" "));
    }

    /*
     * Private methods
     */
    private static String formatDate(Date date, String format) {
        DateTimeFormat dateTimeFormat = DateTimeFormat.getFormat(format);
        if (date == null) {
            return "";
        }
        return dateTimeFormat.format(date);
    }

    private static long parseDate(String date, String format) {
        DateTimeFormat dateTimeFormat = DateTimeFormat.getFormat(format);
        return dateTimeFormat.parse(date).getTime();
    }
}
