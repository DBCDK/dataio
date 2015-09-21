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

import org.xmlunit.XMLUnitException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.ComparisonController;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;


public class XmlDiffGenerator {
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
    public String getDiff(final byte[] current, final byte[] next) throws DiffGeneratorException {
        try {
            final Diff diffResult = DiffBuilder.compare(Input.fromByteArray(current))
                    .withTest(Input.fromByteArray(next))
                    .checkForSimilar()
                    .ignoreWhitespace()
                    .withComparisonController(new ComparisonController() {
                        @Override
                        public boolean stopDiffing(final Difference difference) {
                            return false; // we want all differences
                        }
                    })
                    .build();
            if (!diffResult.hasDifferences()) {
                return "";
            }

            return buildResultString(diffResult);
        } catch ( XMLUnitException e) {
           throw new DiffGeneratorException("XmlDiff Failed to compare input", e);
        }
    }

    String buildResultString( final Diff diff) {
        final Iterable<Difference> differences = diff.getDifferences();

        final StringBuilder result = new StringBuilder();
        int changeNumber=1;
        for (final Difference difference : differences) {
            result.append("Change nr ").append(changeNumber++).append(" ----\n");
            result.append(difference.toString()); // Default output format
            result.append("----\n");
        }

        return result.toString();
    }

}
