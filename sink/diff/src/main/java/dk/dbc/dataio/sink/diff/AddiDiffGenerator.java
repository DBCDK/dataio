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

package dk.dbc.dataio.sink.diff;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

@Stateless
public class AddiDiffGenerator {
    @EJB
    ExternalToolDiffGenerator externalToolDiffGenerator;

    /**
     * Creates diff string through external diff tool, returning
     * no diff as empty string   : if both metadata and content of the two addi records
     *                             are identical or semantically equivalent.
     * metadata diff             : if metadata of the two addi records differ.
     * content diff              : if content data of the two addi records differ.
     * content and metadata diff : if both content and metadata of the two addi records differ.
     * @param current containing the current addi record
     * @param next containing the next addi record
     * @return the diff string
     * @throws DiffGeneratorException on failure to create diff
     */
    public String getDiff(byte[] current, byte[] next) throws DiffGeneratorException {
        final AddiReader currentAddiReader = new AddiReader(new ByteArrayInputStream(current));
        final AddiReader nextAddiReader = new AddiReader(new ByteArrayInputStream(next));

        final StringBuilder diff = new StringBuilder();
        try {
            while (currentAddiReader.hasNext()) {
                final AddiRecord currentAddiRecord = currentAddiReader.next();
                AddiRecord nextAddiRecord = nextAddiReader.next();
                if (nextAddiRecord == null) {
                    nextAddiRecord = new AddiRecord(new byte[0], new byte[0]);
                }
                diff.append(new AddiRecordDiff(currentAddiRecord, nextAddiRecord).toString());
            }
            while (nextAddiReader.hasNext()) {
                final AddiRecord currentAddiRecord = new AddiRecord(new byte[0], new byte[0]);
                final AddiRecord nextAddiRecord = nextAddiReader.next();
                diff.append(new AddiRecordDiff(currentAddiRecord, nextAddiRecord).toString());
            }
        } catch (RuntimeException | IOException e) {
            throw new IllegalArgumentException("byte array can not be converted to ADDI", e);
        }

        return diff.toString();
    }

    private class AddiRecordDiff {
        private static final String NO_DIFF = "";

        private final String metaDiff;
        private final String contentDiff;

        private AddiRecordDiff(AddiRecord current, AddiRecord next) throws DiffGeneratorException {
            metaDiff = getDiff(current.getMetaData(), next.getMetaData());
            contentDiff = getDiff(current.getContentData(), next.getContentData());
        }

        @Override
        public String toString() {
            return metaDiff + contentDiff;
        }

        private String getDiff(byte[] current, byte[] next) throws DiffGeneratorException {
            if (!Arrays.equals(current, next)) {
                final ExternalToolDiffGenerator.Kind currentKind =
                        DiffKindDetector.getKind(current);
                final ExternalToolDiffGenerator.Kind nextKind =
                        DiffKindDetector.getKind(next);
                if (currentKind == nextKind) {
                    return externalToolDiffGenerator.getDiff(currentKind, current, next);
                }
                return externalToolDiffGenerator.getDiff(
                        ExternalToolDiffGenerator.Kind.PLAINTEXT, current, next);
            }
            return NO_DIFF;
        }
    }
}
