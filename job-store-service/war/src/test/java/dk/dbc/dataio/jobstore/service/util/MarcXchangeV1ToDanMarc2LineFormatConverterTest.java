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

package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.DiagnosticBuilder;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.marc.binding.ControlField;
import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.Leader;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.binding.SubField;
import dk.dbc.marc.reader.DanMarc2LineFormatReader;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.marc.writer.MarcXchangeV1Writer;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

public class MarcXchangeV1ToDanMarc2LineFormatConverterTest {
    private final List<Diagnostic> diagnostics = Collections.emptyList();

    private final String endTag = "$\n";
    private final String expectedRecordAsLineFormat =
            "245 12 *aA @*programmer is born*beveryday@@dbc\n" +
            "530 00 *ithis is to be used in test\n";


    private final String e0100a = "e01 00 *a";
    private final String diagnosticMessage = "This a diagnostic FATAL message";

    private MarcXchangeV1ToDanMarc2LineFormatConverter converter;

    @Before
    public void newInstance() {
        converter = new MarcXchangeV1ToDanMarc2LineFormatConverter();
    }

    @Test
    public void convert_invalidMarc_throws() {
        final ChunkItem chunkItem = buildChunkItem("invalid", ChunkItem.Status.FAILURE);
        try {
            // Subject under test
            converter.convert(chunkItem, StandardCharsets.UTF_8, diagnostics);
            fail("No JobStoreException thrown");
        } catch (JobStoreException e) {
            // Verification
            assertThat(e.getCause() instanceof MarcReaderException, is(true));
        }
    }

    @Test
    public void convert_chunkItemWithDiagnostics_returnsDanmarc2LineFormatWithDiagnosticMessagesInSubfields() throws JobStoreException {
        final String firstDiagnosticMessage = "This is the first diagnostic FATAL message";
        final String secondDiagnosticMessage = "This is the second diagnostic WARNING message";
        final String thirdDiagnosticMessage = "This is the third diagnostic FATAL message";

        final ChunkItem chunkItem = buildChunkItem(asMarcXchange(getMarcRecord()), ChunkItem.Status.FAILURE);
        final List<Diagnostic> diagnostics = Arrays.asList(
                new DiagnosticBuilder().setMessage(firstDiagnosticMessage).build(),
                new DiagnosticBuilder().setLevel(Diagnostic.Level.WARNING).setMessage(secondDiagnosticMessage).build(),
                new DiagnosticBuilder().setMessage(thirdDiagnosticMessage).build());

        // Subject under test
        byte[] danmarc2LineFormat = converter.convert(chunkItem, StandardCharsets.UTF_8, diagnostics);

        // Verification
        final String expectedDiagnosticSubfieldsAsLineFormat =
                e0100a + firstDiagnosticMessage + "\n" +
                e0100a + secondDiagnosticMessage + "\n" +
                e0100a + thirdDiagnosticMessage + "\n";

        assertThat(StringUtil.asString(danmarc2LineFormat), is(expectedRecordAsLineFormat + expectedDiagnosticSubfieldsAsLineFormat + endTag));
    }

    @Test
    public void convert_chunkItemWithDiagnosticsAndATagField_returnsDanmarc2LineFormatWithDiagnosticMessagesInSubfields() throws JobStoreException {
        final ChunkItem chunkItem = buildChunkItem(asMarcXchange(getMarcRecord()), ChunkItem.Status.FAILURE);
        final List<Diagnostic> diagnostics = Collections.singletonList(
                new DiagnosticBuilder().setMessage(diagnosticMessage).setTag("Tag Field").setAttribute("").build());

        // Subject under test
        byte[] danmarc2LineFormat = converter.convert(chunkItem, StandardCharsets.UTF_8, diagnostics);

        // Verification
        final String expectedDiagnosticTagFieldAsLineFormat = e0100a + diagnosticMessage + "*bTag Field*c\n";
        assertThat(StringUtil.asString(danmarc2LineFormat), is(expectedRecordAsLineFormat + expectedDiagnosticTagFieldAsLineFormat + endTag));
    }

    @Test
    public void convert_chunkItemWithDiagnosticsAndAnAttributeField_returnsDanmarc2LineFormatWithDiagnosticMessagesInSubfields() throws JobStoreException {
        final ChunkItem chunkItem = buildChunkItem(asMarcXchange(getMarcRecord()), ChunkItem.Status.FAILURE);
        final List<Diagnostic> diagnostics = Collections.singletonList(
                new DiagnosticBuilder().setMessage(diagnosticMessage).setTag("").setAttribute("Att Field").build());

        // Subject under test
        byte[] danmarc2LineFormat = converter.convert(chunkItem, StandardCharsets.UTF_8, diagnostics);

        // Verification
        final String expectedDiagnosticAttributeFieldAsLineFormat = e0100a + diagnosticMessage + "*b*cAtt Field\n";
        assertThat(StringUtil.asString(danmarc2LineFormat), is(expectedRecordAsLineFormat + expectedDiagnosticAttributeFieldAsLineFormat + endTag));
    }

    @Test
    public void convert_chunkItemWithDiagnosticsAndBothTagAndAttributeFields_returnsDanmarc2LineFormatWithDiagnosticMessagesInSubfields() throws JobStoreException {
        final ChunkItem chunkItem = buildChunkItem(asMarcXchange(getMarcRecord()), ChunkItem.Status.FAILURE);
        final List<Diagnostic> diagnostics = Collections.singletonList(
                new DiagnosticBuilder().setMessage(diagnosticMessage).setTag("Tag Field").setAttribute("Att Field").build());

        // Subject under test
        byte[] danmarc2LineFormat = converter.convert(chunkItem, StandardCharsets.UTF_8, diagnostics);

        // Verification
        final String expectedDiagnosticTagAndAttributeFieldAsLineFormat = e0100a + diagnosticMessage + "*bTag Field*cAtt Field\n";
        assertThat(StringUtil.asString(danmarc2LineFormat), is(expectedRecordAsLineFormat + expectedDiagnosticTagAndAttributeFieldAsLineFormat + endTag));
    }

    @Test
    public void convert_chunkItemWithoutDiagnostics_returnsDanmarc2LineFormatWithoutDiagnosticMessagesInSubfields() throws JobStoreException {
        final ChunkItem chunkItem = buildChunkItem(asMarcXchange(getMarcRecord()), ChunkItem.Status.SUCCESS);

        // Subject under test
        byte[] danmarc2LineFormat = converter.convert(chunkItem, StandardCharsets.UTF_8, diagnostics);

        // Verification
        assertThat(StringUtil.asString(danmarc2LineFormat), is(expectedRecordAsLineFormat + endTag));
    }

    @Test
    public void marcRecordContainsControlFields() throws JobStoreException {
        final MarcRecord marcRecord = getMarcRecord();
        marcRecord.getFields().add(0, new ControlField().setTag("100").setData("00"));
        marcRecord.getFields().add(new ControlField().setTag("999").setData("00"));
        final ChunkItem chunkItem = buildChunkItem(asMarcXchange(marcRecord), ChunkItem.Status.SUCCESS);
        final byte[] danmarc2LineFormat = converter.convert(chunkItem, StandardCharsets.UTF_8, diagnostics);
        assertThat(StringUtil.asString(danmarc2LineFormat), is(expectedRecordAsLineFormat +
                "e01 00 *afelt '100' mangler delfelter*bfelt '100'\n" +
                "e01 00 *afelt '999' mangler delfelter*bfelt '999'\n" +
                endTag));
    }

    private ChunkItem buildChunkItem(String data, ChunkItem.Status status) {
        return new ChunkItemBuilder().setData(data.getBytes()).setStatus(status).build();
    }

    private MarcRecord getMarcRecord() {
        final DataField dataField245 = new DataField()
                .setTag("245")
                .setInd1('1')
                .setInd2('2')
                .addSubfield(new SubField()
                        .setCode('a')
                        .setData("A *programmer is born"))
                .addSubfield(new SubField()
                        .setCode('b')
                        .setData("everyday@dbc"));
        final DataField dataField530 = new DataField()
                .setTag("530")
                .addSubfield(new SubField()
                        .setCode('i')
                        .setData("this is to be used in test"));
        return new MarcRecord()
                .setLeader(new Leader().setData(DanMarc2LineFormatReader.DEFAULT_LEADER))
                .addAllFields(Arrays.asList(dataField245, dataField530));
    }

    private MarcRecord getMarcRecordWithControlFields() {
        final ControlField f001 = new ControlField()
                .setTag("001")
                .setData("5 047 389 9");
        final Leader leader = new Leader()
                .setData("01471cjm a2200349 a 4500");
        final MarcRecord marcRecord = new MarcRecord()
                .setLeader(leader);
        marcRecord.getFields().addAll(Collections.singletonList(f001));
        return marcRecord;
    }

    private String asMarcXchange(MarcRecord record) {
        final MarcXchangeV1Writer writer = new MarcXchangeV1Writer()
                .setProperty(MarcXchangeV1Writer.Property.ADD_XML_DECLARATION, Boolean.FALSE);
        return new String(writer.write(record, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
}
