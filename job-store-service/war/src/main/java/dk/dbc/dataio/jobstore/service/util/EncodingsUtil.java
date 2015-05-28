package dk.dbc.dataio.jobstore.service.util;

/**
 * Utility class for Encodings equivalence tests
 */
public class EncodingsUtil {
    private EncodingsUtil() {}

    /**
     * Tests two encoding strings for equivalence.
     * <pre>
     * {@code
     *
     * isEquivalent("utf-8", "UTF8");           // true
     * isEquivalent("ISO-8859-1", "latin1");    // false
     * }
     * </pre>
     * @param encoding1 first encoding to be compared
     * @param encoding2 second encoding to be compared
     * @return true if the two encodings are deemed equivalent false if not (or if any of the given strings are null)
     */
    public static boolean isEquivalent(String encoding1, String encoding2) {
        return !(encoding1 == null || encoding2 == null)
                && normalizeEncoding(encoding1).equals(normalizeEncoding(encoding2));
    }

    private static String normalizeEncoding(String encoding) {
        return encoding.replaceAll("-", "").toLowerCase();
    }
}
