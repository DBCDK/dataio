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
import dk.dbc.dataio.commons.utils.lang.StringUtil;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.Arrays;

@Stateless
public class AddiDiffGenerator {
    @EJB
    ExternalToolDiffGenerator externalToolDiffGenerator;

    /**
     * Creates diff string through XmlDiff.
     * NOTE. The addi diff assumes content is xml!
     *
     * no diff as empty string          if both meta data and content data of the two addi records
     *                                  are identical or semantic identical.
     * diff with meta data              if meta data of the two addi records differs.
     * diff with content data           if content data of the two addi records differs.
     * diff with content and meta data  if both content data and meta data of the two addi records differs.
     *
     * @param current containing the current addi record
     * @param next containing the next addi record
     * @return the diff string
     *
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
            metaDiff = determineMetadataDiff(current, next);
            contentDiff = determineContentDiff(current, next);
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

        private String determineMetadataDiff(AddiRecord current, AddiRecord next) throws DiffGeneratorException {
            if (!Arrays.equals(current.getMetaData(), next.getMetaData())) {
                if (isXml(current.getMetaData()) && isXml(next.getMetaData())) {
                    return externalToolDiffGenerator.getDiff(current.getMetaData(), next.getMetaData());
                }
                return StringUtil.asString(current.getMetaData()) + "\n" + StringUtil.asString(next.getMetaData());
            }
            return NO_DIFF;
        }

        private String determineContentDiff(AddiRecord current, AddiRecord next) throws DiffGeneratorException {
            return externalToolDiffGenerator.getDiff(current.getContentData(), next.getContentData());
        }

        private boolean isXml(byte[] data) {
            return data.length > 0 && data[0] == '<';
        }
    }
}
