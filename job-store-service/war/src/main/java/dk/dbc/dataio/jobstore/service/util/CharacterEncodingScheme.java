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
import dk.dbc.marc.Marc8Charset;

import java.nio.charset.Charset;

public class CharacterEncodingScheme {
    private CharacterEncodingScheme() {}

    /**
     * Resolves {@link Charset} from given character set name (or alias)
     * @param name character set name (is automatically lowercased and trimmed)
     * @return {@link Charset} instance
     * @throws InvalidEncodingException if unable to resolve into {@link Charset}
     */
    public static Charset charsetOf(String name) throws InvalidEncodingException {
        try {
            final String normalizedEncodingName = normalizeEncodingName(name);
            if ("marc8".equals(normalizedEncodingName)) {
                return new Marc8Charset();
            }
            return Charset.forName(normalizedEncodingName);
        } catch (RuntimeException e) {
            throw new InvalidEncodingException(String.format("Unable to create charset from given name '%s'", name), e);
        }
    }

    private static String normalizeEncodingName(String name) {
        final String normalized = name.trim().toLowerCase();
        if ("latin-1".equals(normalized)) {
            return "latin1";
        }
        if ("marc-8".equals(normalized)) {
            return "marc8";
        }
        return normalized;
    }
}
