package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.Leader;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.binding.SubField;
import dk.dbc.marc.reader.DanMarc2LineFormatReader;
import dk.dbc.marc.writer.DanMarc2LineFormatWriter;
import dk.dbc.marc.writer.LineFormatWriter;
import dk.dbc.marc.writer.MarcWriterException;
import dk.dbc.marc.writer.MarcXchangeV1Writer;
import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class ChunkItemExporterTest {
    private final Charset encoding = StandardCharsets.UTF_8;
    private final MarcRecord marcRecord = getMarcRecord();
    private final List<Diagnostic> diagnostics = Collections.emptyList();
    private final byte[] marcXchange = getMarcRecordAsMarcXchange(marcRecord);
    private final byte[] danMarc2LineFormat = getMarcRecordAsDanMarc2LineFormat(marcRecord);
    private final byte[] marc21LineFormat = getMarcRecordAsMarc21LineFormat(marcRecord);
    private final ChunkItemExporter chunkItemExporter = new ChunkItemExporter();
    private final ChunkItem chunkItem = new ChunkItemBuilder()
            .setType(ChunkItem.Type.MARCXCHANGE)
            .setData(marcXchange)
            .build();

    @Test
    public void export_illegalConversion_throws() {
        try {
            chunkItemExporter.export(chunkItem, ChunkItem.Type.ADDI, encoding, diagnostics);
            fail("No JobStoreException thrown");
        } catch (JobStoreException e) {
        }
    }

    @Test
    public void export_chunkItemWithMarcXchange_canBeExportedAsDanMarc2LineFormat() throws JobStoreException {
        final byte[] bytes = chunkItemExporter.export(chunkItem, ChunkItem.Type.DANMARC2_LINEFORMAT, encoding, diagnostics);
        assertThat(StringUtil.asString(bytes), is(StringUtil.asString(danMarc2LineFormat)));
    }

    @Test
    public void export_chunkItemWithMarcXchange_canBeExportedAsMarc21LineFormat() throws JobStoreException {
        final byte[] bytes = chunkItemExporter.export(chunkItem, ChunkItem.Type.MARC21_LINEFORMAT, encoding, diagnostics);
        assertThat(StringUtil.asString(bytes), is(StringUtil.asString(marc21LineFormat)));
    }

    @Test
    public void export_chunkItemWithMarcXchange_canChooseTypeOfLineFormat() throws JobStoreException {
        final byte[] bytes = chunkItemExporter.export(chunkItem, ChunkItem.Type.LINEFORMAT, encoding, diagnostics);
        assertThat(StringUtil.asString(bytes), is(StringUtil.asString(danMarc2LineFormat)));
    }

    @Test
    public void export_chunkItemWithUnknownType_canBeExportedAsDanMarc2LineFormat() throws JobStoreException {
        final ChunkItem chunkItem = new ChunkItemBuilder()
                .setType(ChunkItem.Type.UNKNOWN)
                .setData(AddiUnwrapperTest.getValidAddi(StringUtil.asString(marcXchange)))
                .build();
        final byte[] bytes = chunkItemExporter.export(chunkItem, ChunkItem.Type.DANMARC2_LINEFORMAT, encoding, diagnostics);
        assertThat(StringUtil.asString(bytes), is(StringUtil.asString(danMarc2LineFormat)));
    }

    @Test
    public void export_chunkItemWithUnknownType_canBeExportedAsMarc21LineFormat() throws JobStoreException {
        final ChunkItem chunkItem = new ChunkItemBuilder()
                .setType(ChunkItem.Type.UNKNOWN)
                .setData(AddiUnwrapperTest.getValidAddi(StringUtil.asString(marcXchange)))
                .build();
        final byte[] bytes = chunkItemExporter.export(chunkItem, ChunkItem.Type.MARC21_LINEFORMAT, encoding, diagnostics);
        assertThat(StringUtil.asString(bytes), is(StringUtil.asString(marc21LineFormat)));
    }

    @Test
    public void export_chunkItemWithUnknownType_canChooseTypeOfLineFormat() throws JobStoreException {
        final ChunkItem chunkItem = new ChunkItemBuilder()
                .setType(ChunkItem.Type.UNKNOWN)
                .setData(AddiUnwrapperTest.getValidAddi(StringUtil.asString(marcXchange)))
                .build();
        final byte[] bytes = chunkItemExporter.export(chunkItem, ChunkItem.Type.LINEFORMAT, encoding, diagnostics);
        assertThat(StringUtil.asString(bytes), is(StringUtil.asString(danMarc2LineFormat)));
    }

    @Test
    public void export_chunkItemWithMarcXchangeWrappedInAddi_canBeExportedAsDanMarc2LineFormat() throws JobStoreException {
        final ChunkItem chunkItem = new ChunkItemBuilder()
                .setType(Arrays.asList(ChunkItem.Type.ADDI, ChunkItem.Type.MARCXCHANGE))
                .setData(AddiUnwrapperTest.getValidAddi(StringUtil.asString(marcXchange), StringUtil.asString(marcXchange)))
                .build();
        final byte[] bytes = chunkItemExporter.export(chunkItem, ChunkItem.Type.DANMARC2_LINEFORMAT, encoding, diagnostics);
        assertThat(StringUtil.asString(bytes),
                is(StringUtil.asString(danMarc2LineFormat) + StringUtil.asString(danMarc2LineFormat)));
    }

    @Test
    public void export_chunkItemWithMarcXchangeWrappedInAddi_canBeExportedAsMarc21LineFormat() throws JobStoreException {
        final ChunkItem chunkItem = new ChunkItemBuilder()
                .setType(Arrays.asList(ChunkItem.Type.ADDI, ChunkItem.Type.MARCXCHANGE))
                .setData(AddiUnwrapperTest.getValidAddi(StringUtil.asString(marcXchange), StringUtil.asString(marcXchange)))
                .build();
        final byte[] bytes = chunkItemExporter.export(chunkItem, ChunkItem.Type.MARC21_LINEFORMAT, encoding, diagnostics);
        assertThat(StringUtil.asString(bytes),
                is(StringUtil.asString(marc21LineFormat) + StringUtil.asString(marc21LineFormat)));
    }

    @Test
    public void export_chunkItemWithMarcXchangeWrappedInAddi_canChooseTypeOfLineFormat() throws JobStoreException {
        final ChunkItem chunkItem = new ChunkItemBuilder()
                .setType(Arrays.asList(ChunkItem.Type.ADDI, ChunkItem.Type.MARCXCHANGE))
                .setData(AddiUnwrapperTest.getValidAddi(StringUtil.asString(marcXchange), StringUtil.asString(marcXchange)))
                .build();
        final byte[] bytes = chunkItemExporter.export(chunkItem, ChunkItem.Type.LINEFORMAT, encoding, diagnostics);
        assertThat(StringUtil.asString(bytes),
                is(StringUtil.asString(danMarc2LineFormat) + StringUtil.asString(danMarc2LineFormat)));
    }

    @Test
    public void export_chunkItem_canBeExportedAsBytes() throws JobStoreException {
        final byte[] bytes = chunkItemExporter.export(chunkItem, ChunkItem.Type.BYTES, encoding, diagnostics);
        assertThat(StringUtil.asString(bytes), is(StringUtil.asString(chunkItem.getData())));
    }

    private MarcRecord getMarcRecord() {
        final DataField dataField245 = new DataField()
                .setTag("245")
                .setInd1('0')
                .setInd2('0')
                .addSubfield(new SubField()
                    .setCode('a')
                    .setData("A *programmer is born"))
                .addSubfield(new SubField()
                    .setCode('b')
                    .setData("everyday@dbc"));
        return new MarcRecord()
                .setLeader(new Leader().setData(DanMarc2LineFormatReader.DEFAULT_LEADER))
                .addAllFields(Collections.singletonList(dataField245));
    }

    private byte[] getMarcRecordAsMarcXchange(MarcRecord record) {
        final MarcXchangeV1Writer writer = new MarcXchangeV1Writer();
        return writer.write(record, encoding);
    }

    private byte[] getMarcRecordAsDanMarc2LineFormat(MarcRecord record) {
        final DanMarc2LineFormatWriter writer = new DanMarc2LineFormatWriter();
        try {
            return writer.write(record, encoding);
        } catch (MarcWriterException e) {
            throw new IllegalStateException(e);
        }
    }

    private byte[] getMarcRecordAsMarc21LineFormat(MarcRecord record) {
        final LineFormatWriter writer = new LineFormatWriter();
        try {
            return writer.write(record, encoding);
        } catch (MarcWriterException e) {
            throw new IllegalStateException(e);
        }
    }
}
