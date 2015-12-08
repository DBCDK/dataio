package dk.dbc.dataio.marc.writer;

import dk.dbc.dataio.marc.binding.ControlField;
import dk.dbc.dataio.marc.binding.DataField;
import dk.dbc.dataio.marc.binding.Leader;
import dk.dbc.dataio.marc.binding.MarcRecord;
import dk.dbc.dataio.marc.binding.SubField;
import dk.dbc.dataio.marc.reader.DanMarc2LineFormatReader;
import dk.dbc.marc.DanMarc2Charset;
import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class DanMarc2LineFormatWriterTest {
    private final DanMarc2LineFormatWriter writer = new DanMarc2LineFormatWriter();
    private final MarcRecord record = getMarcRecord();
    private final String recordAsLineFormat =
            "245 12 *a A @*programmer is born *b everyday@@dbc\n" +
            "530 00 *i this is a very long text exceeding the maximum number of seventy nine\n" +
            "     characters per line which results in a line continuation\n" +
            "$\n";

    @Test
    public void write_danMarc2Charset_throws() throws MarcWriterException {
        final DanMarc2Charset charset = new DanMarc2Charset(DanMarc2Charset.Variant.LINE_FORMAT);
        try {
            writer.write(record, charset);
            fail("No UnsupportedCharsetException thrown");
        } catch (UnsupportedCharsetException e) {
        }
    }

    @Test
    public void write_marcRecordContainsControlField_throws() throws MarcWriterException {
        final MarcRecord marcRecord = getMarcRecord()
                .addField(new ControlField()
                    .setTag("001")
                    .setData("data"));
        try {
            writer.write(marcRecord, StandardCharsets.UTF_8);
            fail("No MarcWriterException thrown");
        } catch (MarcWriterException e) {
            assertThat(e.getCause() instanceof IllegalArgumentException, is(true));
        }
    }

    @Test
    public void write_equality() throws MarcWriterException {
        final byte[] bytes = writer.write(record, StandardCharsets.UTF_8);
        assertThat(asString(bytes), is(recordAsLineFormat));
    }

    private String asString(byte[] bytes, Charset encoding) {
        return new String(bytes, encoding);
    }

    private String asString(byte[] bytes) {
        return asString(bytes, StandardCharsets.UTF_8);
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
                    .setData("this is a very long text exceeding the maximum number of seventy nine characters per line which results in a line continuation"));
        return new MarcRecord()
                .setLeader(new Leader().setData(DanMarc2LineFormatReader.DEFAULT_LEADER))
                .addAllFields(Arrays.asList(dataField245, dataField530));
    }
}