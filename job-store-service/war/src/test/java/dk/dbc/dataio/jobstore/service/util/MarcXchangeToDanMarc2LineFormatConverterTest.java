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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class MarcXchangeToDanMarc2LineFormatConverterTest {
    private final List<Diagnostic> diagnostics = Collections.emptyList();

    private final String endTag = "$\n";
    private final String expectedRecordAsLineFormat =
            "245 12 *aA @*programmer is born*beveryday@@dbc\n" +
                    "530 00 *ithis is to be used in test* testing blank subfield code\n";

    private final ChunkItem chunkItemFailed = buildChunkItem(
            asMarcXchange(getMarcRecord()), ChunkItem.Status.FAILURE);

    private final String e0100 = "e01 00 ";
    private final String diagnosticMessage = "This a diagnostic FATAL message";

    private MarcXchangeToDanMarc2LineFormatConverter converter;

    @BeforeEach
    public void newInstance() {
        converter = new MarcXchangeToDanMarc2LineFormatConverter();
    }

    @Test
    public void convertInvalidMarc() {
        ChunkItem chunkItem = buildChunkItem("invalid", ChunkItem.Status.FAILURE);
        try {
            converter.convert(chunkItem, StandardCharsets.UTF_8, diagnostics);
            Assertions.fail("No JobStoreException thrown");
        } catch (JobStoreException e) {
            assertThat(e.getCause() instanceof MarcReaderException, is(true));
        }
    }

    @Test
    public void convertChunkItemWithMultipleDiagnostics() throws JobStoreException {
        final String firstDiagnosticMessage = "This is the first diagnostic FATAL message";
        final String secondDiagnosticMessage = "This is the second diagnostic WARNING message";
        final String thirdDiagnosticMessage = "This is the third diagnostic FATAL message";

        List<Diagnostic> diagnostics = Arrays.asList(
                new DiagnosticBuilder().setMessage(firstDiagnosticMessage).build(),
                new DiagnosticBuilder().setMessage(secondDiagnosticMessage)
                        .setLevel(Diagnostic.Level.WARNING).build(),
                new DiagnosticBuilder().setMessage(thirdDiagnosticMessage).build());

        byte[] danmarc2LineFormat = converter.convert(chunkItemFailed, StandardCharsets.UTF_8, diagnostics);

        final String e01Expected =
                e0100 + "*a" + firstDiagnosticMessage + "\n" +
                        e0100 + "*a" + secondDiagnosticMessage + "\n" +
                        e0100 + "*a" + thirdDiagnosticMessage + "\n";
        assertThat(StringUtil.asString(danmarc2LineFormat),
                is(expectedRecordAsLineFormat + e01Expected + endTag));
    }

    @Test
    public void convertChunkItemWithDiagnosticsWithTagField() throws JobStoreException {
        List<Diagnostic> diagnostics = Collections.singletonList(
                new DiagnosticBuilder().setMessage(diagnosticMessage).setTag("field").build());

        byte[] danmarc2LineFormat = converter.convert(chunkItemFailed, StandardCharsets.UTF_8, diagnostics);

        final String e01Expected = e0100 + "*bfield*a" + diagnosticMessage + "\n";
        assertThat(StringUtil.asString(danmarc2LineFormat),
                is(expectedRecordAsLineFormat + e01Expected + endTag));
    }

    @Test
    public void convertChunkItemWithDiagnosticsWithAttributeField() throws JobStoreException {
        List<Diagnostic> diagnostics = Collections.singletonList(
                new DiagnosticBuilder().setMessage(diagnosticMessage).setAttribute("subfield").build());

        byte[] danmarc2LineFormat = converter.convert(chunkItemFailed, StandardCharsets.UTF_8, diagnostics);

        final String e01Expected = e0100 + "*csubfield*a" + diagnosticMessage + "\n";
        assertThat(StringUtil.asString(danmarc2LineFormat),
                is(expectedRecordAsLineFormat + e01Expected + endTag));
    }

    @Test
    public void convertChunkItemWithDiagnosticsWithTagAndAttributeFields() throws JobStoreException {
        List<Diagnostic> diagnostics = Collections.singletonList(
                new DiagnosticBuilder().setMessage(diagnosticMessage).setTag("field").setAttribute("subfield").build());

        byte[] danmarc2LineFormat = converter.convert(chunkItemFailed, StandardCharsets.UTF_8, diagnostics);

        final String e01Expected = e0100 + "*bfield*csubfield*a" + diagnosticMessage + "\n";
        assertThat(StringUtil.asString(danmarc2LineFormat),
                is(expectedRecordAsLineFormat + e01Expected + endTag));
    }

    @Test
    public void convertChunkItemWithoutDiagnostics() throws JobStoreException {
        ChunkItem chunkItem = buildChunkItem(asMarcXchange(getMarcRecord()), ChunkItem.Status.SUCCESS);

        byte[] danmarc2LineFormat = converter.convert(chunkItem, StandardCharsets.UTF_8, diagnostics);
        assertThat(StringUtil.asString(danmarc2LineFormat),
                is(expectedRecordAsLineFormat + endTag));
    }

    @Test
    public void marcRecordContainsControlFields() throws JobStoreException {
        MarcRecord marcRecord = getMarcRecord();
        marcRecord.getFields().add(0, new ControlField().setTag("100").setData("00"));
        marcRecord.getFields().add(new ControlField().setTag("999").setData("00"));
        ChunkItem chunkItem = buildChunkItem(asMarcXchange(marcRecord), ChunkItem.Status.SUCCESS);
        byte[] danmarc2LineFormat = converter.convert(chunkItem, StandardCharsets.UTF_8, diagnostics);
        assertThat(StringUtil.asString(danmarc2LineFormat), is(expectedRecordAsLineFormat +
                "e01 00 *bfelt '100'*afelt '100' mangler delfelter\n" +
                "e01 00 *bfelt '999'*afelt '999' mangler delfelter\n" +
                endTag));
    }

    static ChunkItem buildChunkItem(String data, ChunkItem.Status status) {
        return new ChunkItemBuilder().setData(data.getBytes()).setStatus(status).build();
    }

    static MarcRecord getMarcRecord() {
        DataField dataField245 = new DataField()
                .setTag("245")
                .setInd1('1')
                .setInd2('2')
                .addSubField(new SubField()
                        .setCode('a')
                        .setData("A *programmer is born"))
                .addSubField(new SubField()
                        .setCode('b')
                        .setData("everyday@dbc"));
        DataField dataField530 = new DataField()
                .setTag("530")
                .addSubField(new SubField()
                        .setCode('i')
                        .setData("this is to be used in test"))
                .addSubField(new SubField()
                        .setCode(' ')
                        .setData("testing blank subfield code"));
        return new MarcRecord()
                .setLeader(new Leader().setData(DanMarc2LineFormatReader.DEFAULT_LEADER))
                .addAllFields(Arrays.asList(dataField245, dataField530));
    }

    static String asMarcXchange(MarcRecord record) {
        MarcXchangeV1Writer writer = new MarcXchangeV1Writer()
                .setProperty(MarcXchangeV1Writer.Property.ADD_XML_DECLARATION, Boolean.FALSE);
        return new String(writer.write(record, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
}
