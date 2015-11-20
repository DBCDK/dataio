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

import dk.dbc.dataio.marc.binding.ControlField;
import dk.dbc.dataio.marc.binding.DataField;
import dk.dbc.dataio.marc.binding.Leader;
import dk.dbc.dataio.marc.binding.MarcRecord;
import dk.dbc.dataio.marc.binding.SubField;
import dk.dbc.dataio.marc.writer.MarcXchangeV1Writer;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class MarcXchangeV1ReaderTest {
    private final MarcRecord simpleRecord = getSimpleRecord();

    @Test
    public void read_invalidXml_throws() throws MarcReaderException {
        final MarcXchangeV1Reader reader = newReader(toInputStream("<element>"));
        try {
            reader.read();
            fail("No MarcReaderException thrown");
        } catch (MarcReaderException e) {
        }
    }

    @Test
    public void read() throws MarcReaderException {
        final MarcXchangeV1Reader reader = newReader(toInputStream(asMarcXchange(simpleRecord)));
        assertThat("First read gets record", reader.read(), is(simpleRecord));
        assertThat("Second read gets null", reader.read(), is(nullValue()));
    }

    @Test
    public void read_inputNotInMarcXchangeNamespace_returnsNull() throws MarcReaderException {
        final MarcXchangeV1Reader reader = newReader(toInputStream("<record><leader>123</leader></record>"));
        assertThat(reader.read(), is(nullValue()));
    }

    @Test
    public void read_inputWrappedInNonMarcXchangeElements_returnsRecords() throws MarcReaderException {
        final StringBuilder inputBuffer = new StringBuilder()
            .append("<data-container>")
            .append(asMarcXchange(simpleRecord))
            .append(asMarcXchange(simpleRecord))
            .append("</data-container>");

        final MarcXchangeV1Reader reader = newReader(toInputStream(inputBuffer));
        assertThat("First read gets record", reader.read(), is(simpleRecord));
        assertThat("Second read gets record", reader.read(), is(simpleRecord));
        assertThat("Third read gets null", reader.read(), is(nullValue()));
    }

    private String asMarcXchange(MarcRecord record) {
        final MarcXchangeV1Writer writer = new MarcXchangeV1Writer()
                .setProperty(MarcXchangeV1Writer.Property.ADD_XML_DECLARATION, Boolean.FALSE);
        return new String(writer.write(record, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    private BufferedInputStream toInputStream(StringBuilder sb) {
        return toInputStream(sb.toString());
    }

    private BufferedInputStream toInputStream(String s) {
        return new BufferedInputStream(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)));
    }

    private MarcXchangeV1Reader newReader(BufferedInputStream is) {
        try {
            return new MarcXchangeV1Reader(is, StandardCharsets.UTF_8);
        } catch (MarcReaderException e) {
            throw new IllegalStateException(e);
        }
    }

    private MarcRecord getSimpleRecord() {
        final ControlField f001 = new ControlField()
                .setTag("001")
                .setData("123456");
        final ControlField f003 = new ControlField()
                .setTag("003")
                .setData("identifier");
        final DataField f245 = new DataField()
                .setTag("245")
                .setInd1('0')
                .setInd2('0')
                .addSubfield(new SubField()
                        .setCode('a')
                        .setData("A *programmer is born"))
                .addSubfield(new SubField()
                        .setCode('b')
                        .setData("> life"));
        return new MarcRecord()
                .setLeader(new Leader().setData(DanMarc2LineFormatReader.DEFAULT_LEADER))
                .addAllFields(Arrays.asList(f001, f003, f245));
    }
}