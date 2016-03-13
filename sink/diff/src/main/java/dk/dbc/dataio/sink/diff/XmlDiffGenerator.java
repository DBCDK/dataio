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

import dk.dbc.xmldiff.XmlDiff;
import dk.dbc.xmldiff.XmlDiffTextWriter;
import dk.dbc.xmldiff.XmlDiffWriter;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;


public class XmlDiffGenerator {
    private static final String EMPTY = "";

        // Tags marking difference in current
        private static final String OPEN_CURRENT = "CURRENT[";
        private static final String CLOSE_CURRENT = "]CURRENT , ";

        // Tags marking differences in next
        private static final String OPEN_NEXT = "NEXT[";
        private static final String CLOSE_NEXT = "]NEXT";

        // Namespace url changed (URI) => name is unchanged
        private static final String OPEN_URI = "URI CHANGED[";
        private static final String CLOSE_URI = "]URI CHANGED";
    /**
     * Creates diff string through XmlDiff.
     *
     * Diff as empty string     : if the two input parameters are identical or semantic identical.
     * Diff with xml as string  : if the two input parameters are different from one another.
     *
     * @param current the current item data
     * @param next the next item data
     * @return the diff string
     *
     * @throws DiffGeneratorException on failure to create diff
     */
    public String getDiff(byte[] current, byte[] next) throws DiffGeneratorException {
        final XmlDiffWriter writer = new XmlDiffTextWriter(OPEN_CURRENT, CLOSE_CURRENT, OPEN_NEXT, CLOSE_NEXT, OPEN_URI, CLOSE_URI);
        try {
            XmlDiff xmldiff = XmlDiff.builder().normalize(true).build();
            boolean isEquivalent=xmldiff.compare(
                    new ByteArrayInputStream(current),
                    new ByteArrayInputStream(next),
                    writer);
            if ( ! isEquivalent ) {
                return writer.toString();
            } else {
                return EMPTY;
            }
        } catch (XPathExpressionException | SAXException | IOException e) {
            throw new DiffGeneratorException("XmlDiff Failed to compare input", e);

        }
    }

}
