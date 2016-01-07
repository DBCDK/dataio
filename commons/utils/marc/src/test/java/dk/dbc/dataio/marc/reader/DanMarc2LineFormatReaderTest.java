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
import dk.dbc.dataio.marc.binding.Leader;
import dk.dbc.dataio.marc.binding.MarcRecord;
import dk.dbc.dataio.marc.binding.SubField;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class DanMarc2LineFormatReaderTest {
    private String endOfRecord = "$\n";
    private String simpleRecordInLineFormat = "245 00 *aA @*programmer is born*beveryday@@dbc\n";
    private String complexRecordInLineFormat = simpleRecordInLineFormat +
            "610 00 *0*aGoogle*2DBC\n" +
            "620 00 *a\n" +
            "s10 00 *aDBC\n";
    private MarcRecord simpleRecord = getSimpleMarcRecord();

    @Test(expected = IllegalArgumentException.class)
    public void constructor_inputStreamIsOfTypeBufferedInputStream_throws() {
        newReader(new BufferedInputStream(toInputStream("")));
    }

    @Test
    public void read_inputStreamIsEmpty_returnsNull() throws MarcReaderException {
        final DanMarc2LineFormatReader reader = newReader(toInputStream(""));
        assertThat(reader.read(), is(nullValue()));
    }

    @Test
    public void read_noEndOfRecord_returnsRecord() throws MarcReaderException {
        final DanMarc2LineFormatReader reader = newReader(toInputStream(simpleRecordInLineFormat));
        assertThat(reader.read(), is(simpleRecord));
    }

    @Test
    public void read_lineContinuation_returnsRecord() throws MarcReaderException {
        final String recordWithLineContinuation = "245 00 *aA @*program\n    mer is born*beveryday@@dbc\n$";
        final DanMarc2LineFormatReader reader = newReader(toInputStream(recordWithLineContinuation));
        assertThat(reader.read(), is(simpleRecord));
    }

    @Test
    public void read_illegalLineFormat_throws() throws MarcReaderException {
        final String recordWithIllegalLine = "2 0 0 test";
        final DanMarc2LineFormatReader reader = newReader(toInputStream(recordWithIllegalLine));
        try {
            reader.read();
            fail("No MarcReaderException thrown");
        } catch (MarcReaderException e) {
            assertThat(e instanceof MarcReaderInvalidRecordException, is(not(true)));
            assertThat(e.getMessage(), containsString("Not recognised as line format"));
        }
    }

    @Test
    public void read_illegalEscape_throws() throws MarcReaderException {
        final String recordWithIllegalEscape = "245 00 *aA @*programmer is @born";
        final DanMarc2LineFormatReader reader = newReader(toInputStream(recordWithIllegalEscape));
        try {
            reader.read();
            fail("No MarcReaderInvalidRecordException thrown");
        } catch (MarcReaderInvalidRecordException e) {
            assertThat(e.getBytesRead(), not(nullValue()));
            assertThat(new String(e.getBytesRead(), StandardCharsets.UTF_8).contains(recordWithIllegalEscape), is(true));
            assertThat(e.getMessage(), containsString("'@b'"));
        }
    }

    @Test
    public void read_escapeAtEndOfLine_throws() throws MarcReaderException {
        final String recordWithEscapeAtEndOfLine = "245 00 *aA @*programmer is @";
        final DanMarc2LineFormatReader reader = newReader(toInputStream(recordWithEscapeAtEndOfLine));
        try {
            reader.read();
            fail("No MarcReaderInvalidRecordException thrown");
        } catch (MarcReaderInvalidRecordException e) {
            assertThat(e.getBytesRead(), not(nullValue()));
            assertThat(new String(e.getBytesRead(), StandardCharsets.UTF_8).contains(recordWithEscapeAtEndOfLine), is(true));
            assertThat(e.getMessage(), containsString("'@'"));
        }
    }

    @Test
    public void read_subfieldMarkerAtEndOfLine_throws() throws MarcReaderException {
        final String recordWithSubfieldMarkerAtEndOfLine = "245 00 *aA @*programmer is *";
        final DanMarc2LineFormatReader reader = newReader(toInputStream(recordWithSubfieldMarkerAtEndOfLine));
        try {
            reader.read();
            fail("No MarcReaderInvalidRecordException thrown");
        } catch (MarcReaderInvalidRecordException e) {
            assertThat(e.getBytesRead(), not(nullValue()));
            assertThat(new String(e.getBytesRead(), StandardCharsets.UTF_8).contains(recordWithSubfieldMarkerAtEndOfLine), is(true));
            assertThat(e.getMessage(), containsString("'*'"));
        }
    }

    @Test
    public void read_skipsInvalidRecords() throws MarcReaderException {
        final String invalidRecord = "245 00 *aA good beginning\n260 00 *atest*b@dbc\ninvalid\ninvalid\n$\n";
        final String records =
                simpleRecordInLineFormat + endOfRecord +
                        invalidRecord +
                        simpleRecordInLineFormat + endOfRecord;
        final DanMarc2LineFormatReader reader = newReader(toInputStream(records));
        assertThat("First record returned", reader.read(), is(simpleRecord));
        try {
            reader.read();
            fail("No MarcReaderInvalidRecordException thrown");
        } catch (MarcReaderInvalidRecordException e) {
            assertThat("Second record skipped", e.getMessage(), containsString("'@d'"));
            assertThat(e.getBytesRead(), not(nullValue()));
            final String linesRead = new String(e.getBytesRead(), StandardCharsets.UTF_8);
            assertThat(linesRead.contains(invalidRecord), is(true));
            assertThat(linesRead.contains(simpleRecordInLineFormat), is(false));

        }
        assertThat("Third record returned", reader.read(), is(simpleRecord));
        assertThat("No more records", reader.read(), is(nullValue()));
    }

    @Test
    public void read() throws MarcReaderException {
        final String record = complexRecordInLineFormat + endOfRecord;
        final DanMarc2LineFormatReader reader = newReader(toInputStream(record));
        assertThat(reader.read(), is(getComplexRecord()));
    }

    @Test
    public void hasNext_inputStreamIsEmpty_returnsFalse() throws MarcReaderException {
        final DanMarc2LineFormatReader reader = newReader(toInputStream(""));
        assertThat(reader.hasNext(), is(false));
    }

    @Test
    public void hasNext() throws MarcReaderException {
        final String record = complexRecordInLineFormat + endOfRecord;
        final DanMarc2LineFormatReader reader = newReader(toInputStream(record));
        assertThat(reader.hasNext(), is(true));
        assertThat(reader.hasNext(), is(true));
        reader.read();
        assertThat(reader.hasNext(), is(false));
    }

    private ByteArrayInputStream toInputStream(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

    private DanMarc2LineFormatReader newReader(InputStream is) {
        return new DanMarc2LineFormatReader(is, StandardCharsets.UTF_8);
    }

    private MarcRecord getSimpleMarcRecord() {
        final SubField subFieldA = new SubField()
                .setCode('a')
                .setData("A *programmer is born");
        final SubField subFieldB = new SubField()
                .setCode('b')
                .setData("everyday@dbc");
        final DataField dataField245 = new DataField()
                .setTag("245")
                .setInd1('0')
                .setInd2('0')
                .addAllSubFields(Arrays.asList(subFieldA, subFieldB));
        return new MarcRecord()
                .setLeader(new Leader().setData(DanMarc2LineFormatReader.DEFAULT_LEADER))
                .addField(dataField245);
    }

    private MarcRecord getComplexRecord() {
        // Numeric subfield codes
        final DataField dataField610 = new DataField()
                .setTag("610")
                .setInd1('0')
                .setInd2('0')
                .addSubfield(new SubField()
                        .setCode('0')
                        .setData(""))
                .addSubfield(new SubField()
                        .setCode('a')
                        .setData("Google"))
               .addSubfield(new SubField()
                        .setCode('2')
                        .setData("DBC"));
        // Empty subfields
        final DataField dataField620 = new DataField()
                .setTag("620")
                .setInd1('0')
                .setInd2('0')
                .addSubfield(new SubField()
                        .setCode('a')
                        .setData(""));
        // Non-numeric tag names
        final DataField dataFields01 = new DataField()
                .setTag("s01")
                .setInd1('0')
                .setInd2('0')
                .addSubfield(new SubField()
                        .setCode('a')
                        .setData("DBC"));

        return getSimpleMarcRecord()
                .addField(dataField610)
                .addField(dataField620)
                .addField(dataFields01);
    }
}