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

package dk.dbc.dataio.marc.reader;

import dk.dbc.dataio.marc.binding.DataField;
import dk.dbc.dataio.marc.binding.MarcRecord;
import dk.dbc.dataio.marc.binding.SubField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reader implementation for the line format for danMARC2 with the following additional assumptions:
 *   {@literal @} characters in data are escaped with {@literal @}{@literal @}
 *   * characters outside of subfield codes are escaped with {@literal @}*
 * see also http://www.danbib.dk/index.php?doc=linjeformat
 */
public class DanMarc2LineFormatReader implements MarcReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(DanMarc2LineFormatReader.class);

    private static final String LINE_CONTINUATION = "    ";
    private static final String END_OF_RECORD = "$";
    private static final int ESCAPE_CHAR = 64;      // @
    private static final int SUBFIELD_MARKER = 42;  // *

    private final BufferedReader reader;
    private final Pattern validLinePattern =
            Pattern.compile("(\\p{Alnum}{3}) (\\p{Alnum})(\\p{Alnum}) (\\*\\p{IsLatin}.+$)");

    private boolean looksLikeLineFormat = false;
    private int currentLineNo = 0;

    /**
     * Creates new danMARC2 line format reader
     * @param inputStream stream containing line format records
     * @param encoding line format records encoding
     */
    public DanMarc2LineFormatReader(InputStream inputStream, Charset encoding) {
        reader = new BufferedReader(new InputStreamReader(inputStream, encoding));
    }

    /**
     * Reads next record from input stream, skipping the record entirely in
     * case of MarcReaderInvalidRecordException
     * @return record as MarcRecord instance or null if no record could be read
     * @throws MarcReaderException on unrecoverable error
     */
    @Override
    public MarcRecord read() throws MarcReaderException {
        final List<DataField> fields = new ArrayList<>();

        // Process all lines comprising a single record.
        // Record end is indicated by the END_OF_RECORD marker
        // or by end of stream.

        try {
            String line = getNextLine();
            while (!END_OF_RECORD.equals(line) && line != null) {
                final StringBuilder buffer = new StringBuilder(line);
                while (isNextLineContinuation()) {
                    buffer.append(getNextLine());
                }
                fields.add(getDataField(buffer));
                line = getNextLine();
            }
        } catch (MarcReaderInvalidRecordException e) {
            handleAndRethrow(e);
        }
        return getMarcRecord(fields);
    }

    private DataField getDataField(StringBuilder buffer) throws MarcReaderInvalidRecordException {
        final Matcher validLine = validLinePattern.matcher(buffer.toString());
        if (validLine.find()) {
            looksLikeLineFormat = true;
            return new DataField()
                    .setTag(validLine.group(1))
                    .setInd1(validLine.group(2).charAt(0))
                    .setInd2(validLine.group(3).charAt(0))
                    .addAllSubFields(getSubFields(validLine.group(4)));
        } else {
            throw new MarcReaderInvalidRecordException(String.format(
                    "Format of line %d does not match '%s'", currentLineNo, validLinePattern.toString()));
        }
    }

    /* This method is able to interpret subfields even if they
       contain characters outside of the BMP (Unicode Basic Multilingual Plane),
       i.e. code points that are outside of the u0000-uFFFF range. This will
       only happen rarely, since the code points outside this are mostly assigned
       to dead languages, but there are some useful code points used for mathematical
       notation, and proper encoding of names in Chinese. */
    private List<SubField> getSubFields(String buffer) throws MarcReaderInvalidRecordException {
        final ArrayList<SubField> subFields = new ArrayList<>();
        final StringBuilder accumulator = new StringBuilder();
        SubField subField = null;

        int offset = 0;
        int bufferLength = buffer.length();
        boolean isPreviousEscapeChar = false;
        boolean isPreviousSubfieldMarker = false;
        while (offset < bufferLength) {
            int currentChar = buffer.codePointAt(offset);
            offset += Character.charCount(currentChar);

            switch (currentChar) {
                case SUBFIELD_MARKER:
                    if (isPreviousEscapeChar) {
                        accumulator.appendCodePoint(currentChar);
                        isPreviousEscapeChar = false;
                    } else {
                        isPreviousSubfieldMarker = true;
                    }
                    break;

                case ESCAPE_CHAR:
                    if (isPreviousEscapeChar) {
                        accumulator.appendCodePoint(currentChar);
                        isPreviousEscapeChar = false;
                    } else {
                        isPreviousEscapeChar = true;
                    }
                    break;

                default:
                    if (isPreviousSubfieldMarker) {
                        if (subField != null) {
                            subField.setData(accumulator.toString());
                            subFields.add(subField);
                        }
                        isPreviousSubfieldMarker = false;
                        accumulator.setLength(0);
                        subField = new SubField()
                                .setCode((char) currentChar);
                    } else if (isPreviousEscapeChar) {
                        throw new MarcReaderInvalidRecordException(String.format(
                                "Illegal escape sequence '@%s' at line %d", (char) currentChar, currentLineNo));
                    } else {
                        accumulator.appendCodePoint(currentChar);
                    }
            }
        }

        if (isPreviousEscapeChar) {
            throw new MarcReaderInvalidRecordException(String.format(
                    "Illegal escape character '%s' at end of line %d", (char) ESCAPE_CHAR, currentLineNo));
        }
        if (isPreviousSubfieldMarker) {
            throw new MarcReaderInvalidRecordException(String.format(
                    "Illegal subfield marker '%s' at end of line %d", (char) SUBFIELD_MARKER, currentLineNo));
        }

        if (subField != null) {
            subField.setData(accumulator.toString());
            subFields.add(subField);
        }
        return subFields;
    }

    private MarcRecord getMarcRecord(List<DataField> fields) {
        if (fields.isEmpty())
            return null;

        return new MarcRecord()
                .addAllFields(fields);
    }

    private String getNextLine() throws MarcReaderException {
        try {
            currentLineNo += 1;
            return reader.readLine();
        } catch (IOException e) {
            throw new MarcReaderException(String.format(
                    "Reader caught unrecoverable exception while reading line %d", currentLineNo), e);
        }
    }

    private boolean isNextLineContinuation() throws MarcReaderException {
        try {
            // Peek at the next 4 characters to determine
            // if they indicate a line continuation.

            reader.mark(4);
            final StringBuilder buffer = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                final int c = reader.read();
                if (c == -1)
                    break;
                buffer.append((char) c);
            }
            if (LINE_CONTINUATION.equals(buffer.toString())) {
                return true;
            }
            reader.reset();
            return false;
        } catch (IOException e) {
            throw new MarcReaderException(String.format(
                    "Reader caught unrecoverable exception while testing for line continuation at line %d", currentLineNo), e);
        }
    }

    private void skipRecord() {
        LOGGER.debug("Attempting to skip record from line %d and onwards", currentLineNo);

        String line = null;
        try {
            line = getNextLine();
        } catch (MarcReaderException e) {
            LOGGER.warn("Caught exception while skipping line %d", currentLineNo, e);
        }
        while (!END_OF_RECORD.equals(line) && line != null) {
            try {
                line = getNextLine();
            } catch (MarcReaderException e) {
                LOGGER.warn("Caught exception while skipping line %d", currentLineNo, e);
            }
        }
    }

    private void handleAndRethrow(MarcReaderInvalidRecordException e) throws MarcReaderException {
        if (looksLikeLineFormat) {
            skipRecord();
            throw e;
        } else {
            throw new MarcReaderException("Not recognised as line format");
        }
    }
}
