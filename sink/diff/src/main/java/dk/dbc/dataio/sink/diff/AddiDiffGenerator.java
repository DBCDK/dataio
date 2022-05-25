package dk.dbc.dataio.sink.diff;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

public class AddiDiffGenerator {
    private final ExternalToolDiffGenerator externalToolDiffGenerator;

    public AddiDiffGenerator(ExternalToolDiffGenerator externalToolDiffGenerator) {
        this.externalToolDiffGenerator = externalToolDiffGenerator;
    }

    /**
     * Compares two ADDI byte streams containing current and next output respectively.
     * <p>
     * When a difference is detected, then for each corresponding ADDI record in
     * the streams the metadata and content blocks are compared separately using
     * the diff tool deemed most suitable.
     *
     * @param current current ADDI stream
     * @param next    next ADDI stream
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
