/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.commons.utils.test;

public final class Assert {
    protected Assert() {}

    /**
     * Asserts that <code>block</code> throws exception specified by
	 * <code>exceptionClass</code>. If not, an {@link AssertionError} is thrown with
	 * the reason and information about the failure. Example:
	 * <pre>
	 *   assertThat(&quot;NPE expected&quot;, () -> {}, isThrowing(NullPointerException.class)); // fails:
	 *   assertThat(&quot;NPE expected&quot;, () -> {String s = null; if (s.isEmpty()) {}}, isThrowing(NullPointerException.class) // passes
	 * </pre>
     * @param reason additional information about the error
     * @param block code block expected to throw exception as a lambda
     * @param exceptionClass class of expected exception
     * @param <T> static type of expected exception
     * @return exception caught
     */
    public static <T extends Exception> T assertThat(String reason, ThrowingCodeBlock block, Class<T> exceptionClass) {
        String description = "";
        if (reason != null && !reason.trim().isEmpty()) {
            description += reason + " : ";
        }
        description += "expected " + exceptionClass.getName() + " to be thrown";
        try {
            block.apply();
            throw new AssertionError(description);
        } catch (Exception e) {
            if (!exceptionClass.isInstance(e)) {
                description += ", was " + e.getClass().getName();
                throw new AssertionError(description);
            }
            @SuppressWarnings("unchecked")
            final T exceptionInstance = (T) e;
            return exceptionInstance;
        }
    }

    /**
     * Asserts that <code>block</code> throws exception specified by
	 * <code>exceptionClass</code>. If not, an {@link AssertionError} is thrown with
	 * information about the failure. Example:
	 * <pre>
	 *   assertThat(() -> {}, isThrowing(NullPointerException.class)); // fails:
	 *   assertThat(() -> {String s = null; if (s.isEmpty()) {}}, isThrowing(NullPointerException.class) // passes
	 * </pre>
     * @param block code block expected to throw exception as a lambda
     * @param exceptionClass class of expected exception
     * @param <T> static type of expected exception
     * @return exception caught
     */
    public static <T extends Exception> T assertThat(ThrowingCodeBlock block, Class<T> exceptionClass) {
        return assertThat(null, block, exceptionClass);
    }

    /**
     * Syntactic sugar for specifying class of expected exception
     * @param exceptionClass class of expected exception
     * @param <T> static type of expected exception
     * @return class of expected exception
     */
    public static <T extends Exception> Class<T> isThrowing(Class<T> exceptionClass) {
        return exceptionClass;
    }

    /**
     * Represents a code block throwing an exception
     */
    @FunctionalInterface
    public interface ThrowingCodeBlock {
        void apply() throws Exception;
    }
}
