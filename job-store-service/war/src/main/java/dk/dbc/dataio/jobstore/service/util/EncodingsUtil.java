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

package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.jobstore.types.InvalidEncodingException;

import java.nio.charset.Charset;

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

    public static Charset toEncoding(String encoding) throws InvalidEncodingException {
        try {
            return Charset.forName(encoding.trim());
        } catch (Exception e) {
            throw new InvalidEncodingException(String.format("Unable to create encoding from given name '%s'", encoding), e);
        }
    }

    private static String normalizeEncoding(String encoding) {
        return encoding.replaceAll("-", "").trim().toLowerCase();
    }
}
