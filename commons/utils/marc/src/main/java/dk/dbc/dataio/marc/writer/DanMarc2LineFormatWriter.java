package dk.dbc.dataio.marc.writer;

import dk.dbc.dataio.marc.binding.ControlField;
import dk.dbc.dataio.marc.binding.DataField;
import dk.dbc.dataio.marc.binding.Field;
import dk.dbc.dataio.marc.binding.MarcRecord;
import dk.dbc.dataio.marc.binding.SubField;
import dk.dbc.marc.DanMarc2Charset;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.stream.Collectors;

/**
 * MarcWriter implementation for transforming MarcRecord instances into
 * DanMarc2 line format representations (see also http://www.danbib.dk/index.php?doc=linjeformat).
 *
 * Note that no DanMarc2 charset variants are currently supported as
 * output encoding and will cause an UnsupportedCharsetException to be
 * thrown.
 *
 * This class is thread safe.
 */
public class DanMarc2LineFormatWriter implements MarcWriter {
    private static final String BLANK = " ";
    private static final String LF = "\n";
    private static final String END_OF_RECORD = "$" + LF;
    private static final String LINE_CONTINUATION = BLANK + BLANK + BLANK + BLANK;
    private static final int LINE_MAX_LENGTH = 79;

    /**
     * Writes given record as line format in specified encoding
     * @param marcRecord record to be written
     * @param encoding output encoding
     * @return bytes written
     * @throws UnsupportedCharsetException in case of DanMarc2 charset output encoding
     * @throws MarcWriterException on general failure to write output bytes of if given record
     * contains control fields
     */
    @Override
    public byte[] write(MarcRecord marcRecord, Charset encoding) throws UnsupportedCharsetException, MarcWriterException {
        if (encoding instanceof DanMarc2Charset) {
            throw new UnsupportedCharsetException(encoding.name());
        }
        return new OutputBuffer(marcRecord, encoding).get();
    }

    private static class OutputBuffer {
        private final MarcRecord marcRecord;
        private final Charset encoding;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        public OutputBuffer(MarcRecord marcRecord, Charset encoding) {
            this.marcRecord = marcRecord;
            this.encoding = encoding;
        }

        public byte[] get() throws MarcWriterException {
            try {
                marcRecord.getFields().stream().forEach(this::addField);
                buffer.write(END_OF_RECORD.getBytes(encoding));
            } catch (Exception e) {
                throw new MarcWriterException("Exception caught while writing record", e);
            }
            return buffer.toByteArray();
        }

        private void addField(Field field) throws IllegalArgumentException, UncheckedIOException {
            if (field instanceof ControlField) {
                throw new IllegalArgumentException(String.format("Record contains %s control field", field.getTag()));
            }

            final DataField dataField = (DataField) field;
            final StringBuilder fieldBuffer = new StringBuilder()
                .append(dataField.getTag()).append(BLANK).append(getInd1(dataField)).append(getInd2(dataField)).append(BLANK)
                .append(dataField.getSubfields().stream()
                    .map(this::subfieldToString)
                    .collect(Collectors.joining("")))
                .append(LF);

            try {
                final int fieldLength = fieldBuffer.length();
                if (fieldLength <= LINE_MAX_LENGTH) {
                    buffer.write(fieldBuffer.toString().getBytes(encoding));
                } else {
                    final int increment = LINE_MAX_LENGTH - LINE_CONTINUATION.length();
                    buffer.write(fieldBuffer.substring(0, LINE_MAX_LENGTH).getBytes(encoding));
                    for (int startPos = LINE_MAX_LENGTH; startPos < fieldLength; startPos += increment) {
                        buffer.write(LF.getBytes(encoding));
                        buffer.write(LINE_CONTINUATION.getBytes(encoding));
                        buffer.write(fieldBuffer.substring(startPos, Math.min(fieldBuffer.length(), startPos + increment)).getBytes(encoding));
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private String subfieldToString(SubField subfield) {
            return String.format("*%s%s", subfield.getCode(), escapeLineFormat(subfield.getData()));
        }

        private String escapeLineFormat(String s) {
            final String tmp = s.replaceAll("@", "@@");
            return tmp.replaceAll("\\*", "@*");
        }

        private Character getInd1(DataField dataField) {
            final Character ind1 = dataField.getInd1();
            if (ind1 == null) {
                return '0';
            }
            return ind1;
        }

        private Character getInd2(DataField dataField) {
            final Character ind2 = dataField.getInd2();
            if (ind2 == null) {
                return '0';
            }
            return ind2;
        }
    }
}
