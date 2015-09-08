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
