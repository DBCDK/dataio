package dk.dbc.dataio.sink.diff;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import static dk.dbc.dataio.sink.diff.Kind.detect;

public class AddiDiffGenerator {
    private final DiffGenerator diffGenerator;

    public AddiDiffGenerator(DiffGenerator diffGenerator) {
        this.diffGenerator = diffGenerator;
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
    public String getDiff(byte[] current, byte[] next) throws DiffGeneratorException, InvalidMessageException {
        AddiReader currentAddiReader = new AddiReader(new ByteArrayInputStream(current));
        AddiReader nextAddiReader = new AddiReader(new ByteArrayInputStream(next));

        StringBuilder diff = new StringBuilder();
        try {
            while (currentAddiReader.hasNext()) {
                AddiRecord currentAddiRecord = currentAddiReader.next();
                AddiRecord nextAddiRecord = nextAddiReader.next();
                if (nextAddiRecord == null) {
                    nextAddiRecord = new AddiRecord(new byte[0], new byte[0]);
                }
                diff.append(new AddiRecordDiff(currentAddiRecord, nextAddiRecord));
            }
            while (nextAddiReader.hasNext()) {
                AddiRecord currentAddiRecord = new AddiRecord(new byte[0], new byte[0]);
                AddiRecord nextAddiRecord = nextAddiReader.next();
                diff.append(new AddiRecordDiff(currentAddiRecord, nextAddiRecord));
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

        private AddiRecordDiff(AddiRecord current, AddiRecord next) throws DiffGeneratorException, InvalidMessageException {
            metaDiff = getDiff(current.getMetaData(), next.getMetaData());
            contentDiff = getDiff(current.getContentData(), next.getContentData());
        }

        @Override
        public String toString() {
            return metaDiff + "\n" + contentDiff;
        }

        private String getDiff(byte[] current, byte[] next) throws DiffGeneratorException, InvalidMessageException {
            if (!Arrays.equals(current, next)) {
                Kind currentKind = detect(current);
                Kind nextKind = detect(next);
                if (currentKind == nextKind) {
                    return diffGenerator.getDiff(currentKind, current, next);
                }
                return diffGenerator.getDiff(Kind.PLAINTEXT, current, next);
            }
            return NO_DIFF;
        }
    }
}
