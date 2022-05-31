package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.DiagnosticBuilder;
import dk.dbc.dataio.jobstore.types.JobStoreException;
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

public class MarcXchangeToMarc21LineFormatConverterTest {
    private final List<Diagnostic> diagnostics = Collections.emptyList();

    private final String endTag = "\n";
    private final String expectedRecordAsLineFormat =
            "245 12 $aA *programmer is born$beveryday@dbc\n" +
                    "530    $ithis is to be used in test$ testing blank subfield code\n";

    private final ChunkItem chunkItemFailed = buildChunkItem(
            asMarcXchange(getMarcRecord()), ChunkItem.Status.FAILURE);

    private final String e0100 = "e01 00 ";
    private final String diagnosticMessage = "This a diagnostic FATAL message";

    private MarcXchangeToMarc21LineFormatConverter converter;

    @Before
    public void newInstance() {
        converter = new MarcXchangeToMarc21LineFormatConverter();
    }

    @Test
    public void convertInvalidMarc() {
        final ChunkItem chunkItem = buildChunkItem("invalid", ChunkItem.Status.FAILURE);
        try {
            converter.convert(chunkItem, StandardCharsets.UTF_8, diagnostics);
            fail("No JobStoreException thrown");
        } catch (JobStoreException e) {
            assertThat(e.getCause() instanceof MarcReaderException, is(true));
        }
    }

    @Test
    public void convertChunkItemWithMultipleDiagnostics() throws JobStoreException {
        final String firstDiagnosticMessage = "This is the first diagnostic FATAL message";
        final String secondDiagnosticMessage = "This is the second diagnostic WARNING message";
        final String thirdDiagnosticMessage = "This is the third diagnostic FATAL message";

        final List<Diagnostic> diagnostics = Arrays.asList(
                new DiagnosticBuilder().setMessage(firstDiagnosticMessage).build(),
                new DiagnosticBuilder().setMessage(secondDiagnosticMessage)
                        .setLevel(Diagnostic.Level.WARNING).build(),
                new DiagnosticBuilder().setMessage(thirdDiagnosticMessage).build());

        final byte[] danmarc2LineFormat = converter.convert(chunkItemFailed, StandardCharsets.UTF_8, diagnostics);

        final String e01Expected =
                e0100 + "$a" + firstDiagnosticMessage + "\n" +
                        e0100 + "$a" + secondDiagnosticMessage + "\n" +
                        e0100 + "$a" + thirdDiagnosticMessage + "\n";
        assertThat(StringUtil.asString(danmarc2LineFormat),
                is(expectedRecordAsLineFormat + e01Expected + endTag));
    }

    @Test
    public void convertChunkItemWithDiagnosticsWithTagField() throws JobStoreException {
        final List<Diagnostic> diagnostics = Collections.singletonList(
                new DiagnosticBuilder().setMessage(diagnosticMessage).setTag("field").build());

        final byte[] danmarc2LineFormat = converter.convert(chunkItemFailed, StandardCharsets.UTF_8, diagnostics);

        final String e01Expected = e0100 + "$bfield$a" + diagnosticMessage + "\n";
        assertThat(StringUtil.asString(danmarc2LineFormat),
                is(expectedRecordAsLineFormat + e01Expected + endTag));
    }

    @Test
    public void convertChunkItemWithDiagnosticsWithAttributeField() throws JobStoreException {
        final List<Diagnostic> diagnostics = Collections.singletonList(
                new DiagnosticBuilder().setMessage(diagnosticMessage).setAttribute("subfield").build());

        final byte[] danmarc2LineFormat = converter.convert(chunkItemFailed, StandardCharsets.UTF_8, diagnostics);

        final String e01Expected = e0100 + "$csubfield$a" + diagnosticMessage + "\n";
        assertThat(StringUtil.asString(danmarc2LineFormat),
                is(expectedRecordAsLineFormat + e01Expected + endTag));
    }

    @Test
    public void convertChunkItemWithDiagnosticsWithTagAndAttributeFields() throws JobStoreException {
        final List<Diagnostic> diagnostics = Collections.singletonList(
                new DiagnosticBuilder().setMessage(diagnosticMessage).setTag("field").setAttribute("subfield").build());

        final byte[] danmarc2LineFormat = converter.convert(chunkItemFailed, StandardCharsets.UTF_8, diagnostics);

        final String e01Expected = e0100 + "$bfield$csubfield$a" + diagnosticMessage + "\n";
        assertThat(StringUtil.asString(danmarc2LineFormat),
                is(expectedRecordAsLineFormat + e01Expected + endTag));
    }

    @Test
    public void convertChunkItemWithoutDiagnostics() throws JobStoreException {
        final ChunkItem chunkItem = buildChunkItem(asMarcXchange(getMarcRecord()), ChunkItem.Status.SUCCESS);

        final byte[] danmarc2LineFormat = converter.convert(chunkItem, StandardCharsets.UTF_8, diagnostics);
        assertThat(StringUtil.asString(danmarc2LineFormat),
                is(expectedRecordAsLineFormat + endTag));
    }

    static ChunkItem buildChunkItem(String data, ChunkItem.Status status) {
        return new ChunkItemBuilder().setData(data.getBytes()).setStatus(status).build();
    }

    static MarcRecord getMarcRecord() {
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
                        .setData("this is to be used in test"))
                .addSubfield(new SubField()
                        .setCode(' ')
                        .setData("testing blank subfield code"));
        return new MarcRecord()
                .setLeader(new Leader().setData(DanMarc2LineFormatReader.DEFAULT_LEADER))
                .addAllFields(Arrays.asList(dataField245, dataField530));
    }

    static String asMarcXchange(MarcRecord record) {
        final MarcXchangeV1Writer writer = new MarcXchangeV1Writer()
                .setProperty(MarcXchangeV1Writer.Property.ADD_XML_DECLARATION, Boolean.FALSE);
        return new String(writer.write(record, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
}
