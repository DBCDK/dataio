/*
 * DataIO - Data IO
 *
 * Copyright (C) 2018 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.dataio.gatekeeper;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Detects encoding of character data
 */
public class EncodingDetector {
    private final CharsetDetector charsetDetector;

    public EncodingDetector() {
        charsetDetector = new CharsetDetector();
    }

    /**
     * Detects the charset that best matches the supplied text file
     * @param file path to text file of unknown encoding
     * @return charset or empty
     */
    public Optional<Charset> detect(Path file) {
        try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(file))) {
            charsetDetector.setText(inputStream);
            final CharsetMatch match = charsetDetector.detect();
            if (match != null) {
                return Optional.of(Charset.forName(match.getName()));
            }
            return Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Detects the charset that best matches the supplied input data
     * @param bytes the input text of unknown encoding
     * @return charset or empty
     */
    public Optional<Charset> detect(byte[] bytes) {
        charsetDetector.setText(bytes);
        final CharsetMatch match = charsetDetector.detect();
        if (match != null) {
            return Optional.of(Charset.forName(match.getName()));
        }
        return Optional.empty();
    }
}
