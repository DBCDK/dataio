package dk.dbc.dataio.commons.utils.invariant;

/**
 * Utility class for the enforcing of invariants.
 *
 * Due to GWT compatibility no java7 constructs are allowed,
 */
public class InvariantUtil {
    private InvariantUtil() { }

    /**
     * Simple convenience method to check an argument for equality
     * with {@code null}. If the argument is {@code null} then a
     * NullPointerException is thrown. If the argument is other than
     * {@code null} the object is returned as is.
     *
     * @param object object to test for {@code null}.
     * @param parameterName name of the tested parameter. This name is used in the exception message.
     *
     * @throws NullPointerException if {@code obj} is {@code null}.
     */
    public static <T> T checkNotNullOrThrow(T object, String parameterName)
            throws NullPointerException {
        if (object == null) {
            final String message = "Parameter '" + parameterName + "' cannot be null";
            throw new NullPointerException(message);
        }
        return object;
    }

    /**
     * Simple convenience method to check that a string is not null or empty
     * If the string is {@code null} then a NullPointerException is thrown.
     * If the string is empty  then a IllegalArgumentException is thrown.
     * If the argument is other than that the string is returned as is.
     *
     * @param stringObject string to test for {@code null} or empty.
     * @param parameterName name of the tested parameter. This name is used in the exception message.
     *
     * @throws NullPointerException if {@code stringObject} is {@code null}.
     * @throws IllegalArgumentException if {@code stringObject} is empty
     */
    public static String checkNotNullNotEmptyOrThrow(String stringObject, String parameterName)
            throws NullPointerException, IllegalArgumentException {
        checkNotNullOrThrow(stringObject, parameterName);
        if (stringObject.isEmpty()) {
            final String message = "Parameter '" + parameterName + "' cannot be empty";
            throw new IllegalArgumentException(message);
        }
        return stringObject;
    }
}
