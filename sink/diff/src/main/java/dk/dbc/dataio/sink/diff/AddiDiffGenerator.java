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

import dk.dbc.commons.addi.AddiRecord;

import javax.ejb.EJB;
import javax.ejb.Stateless;
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
    public String getDiff(AddiRecord current, AddiRecord next) throws DiffGeneratorException {
        return new AddiDiff(current, next).toString();
    }

    private class AddiDiff {
        private static final String NO_DIFF = "";

        private final String metaDiff;
        private final String contentDiff;

        AddiDiff(AddiRecord current, AddiRecord next) throws DiffGeneratorException {
            metaDiff = getDiff(current.getMetaData(), next.getMetaData());
            contentDiff = getDiff(current.getContentData(), next.getContentData());
        }

        @Override
        public String toString() {
            if (hasMetaAndContentDiff()) {
                return metaDiff + "\n" + contentDiff;
            }
            if (hasMetaDiffOnly()) {
                return metaDiff;
            }
            if (hasContentDiffOnly()) {
                return contentDiff;
            }
            return NO_DIFF;
        }

        private boolean hasMetaAndContentDiff() {
            return !metaDiff.isEmpty() && !contentDiff.isEmpty();
        }

        private boolean hasMetaDiffOnly() {
            return !metaDiff.isEmpty() && contentDiff.isEmpty();
        }

        private boolean hasContentDiffOnly() {
            return metaDiff.isEmpty() &&!contentDiff.isEmpty();
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
