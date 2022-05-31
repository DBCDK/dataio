package dk.dbc.dataio.gui.client.helpers;

import dk.dbc.dataio.gui.client.util.Format;

/**
 * Helper methods for the Jobs Show View Class
 */
public class SortHelper {
    /**
     * Validates two objects. If any of the two objects are null pointers, the method returns false
     *
     * @param o1 The first object
     * @param o2 The second object
     * @return True if none of the two objects are null, false otherwise
     */
    public static boolean validateObjects(Object o1, Object o2) {
        return o1 != null && o2 != null;
    }

    /**
     * Compares two strings as the numbers they represent
     *
     * @param s1 String containing first number
     * @param s2 String containing second number
     * @return 0 if equal, negative if s1 is smaller than s2, positive if s1 is greater than s2
     */
    public static int compareStringsAsLongs(String s1, String s2) {
        Long l1 = Long.parseLong(s1);
        Long l2 = Long.parseLong(s2);
        return l1.compareTo(l2);
    }

    /**
     * Compares two strings as an alphanumeric ordering
     *
     * @param s1 String containing first string
     * @param s2 String containing second string
     * @return 0 if equal, negative if s1 is smaller than s2, positive if s1 is greater than s2
     */
    public static int compareStrings(String s1, String s2) {
        return s1.compareTo(s2);
    }

    /**
     * Compares two dates (represented in strings)
     *
     * @param s1 String containing first date
     * @param s2 String containing second date
     * @return 0 if equal, negative if s1 is smaller than s2, positive if s1 is greater than s2
     */
    public static int compareLongDates(String s1, String s2) {
        Long l1 = Format.parseLongDateAsLong(s1);
        Long l2 = Format.parseLongDateAsLong(s2);
        return l1.equals(l2) ? 0 : (l1 < l2) ? -1 : 1;
    }

    /**
     * Compares two longs
     *
     * @param l1 The first long
     * @param l2 The second long
     * @return 0 if equal, negative if s1 is smaller than s2, positive if s1 is greater than s2
     */
    public static int compareLongs(long l1, long l2) {
        return new Long(l1).compareTo(l2);
    }

}
