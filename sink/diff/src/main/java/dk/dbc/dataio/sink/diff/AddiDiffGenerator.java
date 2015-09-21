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

public class AddiDiffGenerator {

    private static final String EMPTY = "";

    /**
     * Creates diff string through XmlDiff.
     * NOTE. The addi diff only works with content as xml!
     *
     * Diff as empty string             if both meta data and content data of the two addi records
     *                                  are identical or semantic identical.
     * Diff with meta data              if meta data of the two addi records differs.
     * Diff with content data           if content data of the two addi records differs.
     * Diff with content and meta data  if both content data and meta data of the two addi records differs.
     *
     * @param current containing the current addi record
     * @param next containing the next addi record
     * @return the diff string
     *
     * @throws DiffGeneratorException on failure to create diff
     */
    public String getDiff(AddiRecord current, AddiRecord next) throws DiffGeneratorException {

        final XmlDiffGenerator xmlDiffGenerator = new XmlDiffGenerator();
        final String metaDiff = xmlDiffGenerator.getDiff(current.getMetaData(), next.getMetaData());
        final String contentDiff = xmlDiffGenerator.getDiff(current.getContentData(), next.getContentData());

        // Meta data NOT identical => content data NOT identical
        if(!metaDiff.isEmpty() && !contentDiff.isEmpty()) {
            return metaDiff + System.lineSeparator() + contentDiff;

        // Meta data NOT identical => content data identical
        } else if(!metaDiff.isEmpty() && contentDiff.isEmpty()) {
            return metaDiff;

        // Meta data identical => content data NOT identical
        } else if(metaDiff.isEmpty() &&!contentDiff.isEmpty()) {
            return contentDiff;

        // Content data and meta data identical
        } else {
            return EMPTY;
        }
    }
}