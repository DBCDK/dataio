package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.marc.binding.ControlField;
import dk.dbc.marc.binding.MarcRecord;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MarcXchangeToLineFormatConverterTest {
    @Test
    public void convertMarc21() throws JobStoreException {
        ControlField f001 = new ControlField();
        f001.setTag("001");
        f001.setData("123456");
        ControlField f003 = new ControlField();
        f003.setTag("003");
        f003.setData("TEST");

        MarcRecord marcRecord = MarcXchangeToMarc21LineFormatConverterTest.getMarcRecord()
                .addField(f001)
                .addField(f003);

        ChunkItem chunkItem =
                MarcXchangeToMarc21LineFormatConverterTest.buildChunkItem(
                        MarcXchangeToMarc21LineFormatConverterTest.asMarcXchange(marcRecord),
                        ChunkItem.Status.SUCCESS);

        MarcXchangeToLineFormatConverter converter = new MarcXchangeToLineFormatConverter();
        byte[] danmarc2LineFormat = converter.convert(chunkItem, StandardCharsets.UTF_8, null);
        assertThat(StringUtil.asString(danmarc2LineFormat),
                is("245 12 $aA *programmer is born$beveryday@dbc\n" +
                        "530    $ithis is to be used in test$ testing blank subfield code\n" +
                        "001 123456\n" +
                        "003 TEST\n\n"));
    }

    @Test
    public void convertDanMarc2() throws JobStoreException {
        ChunkItem chunkItem =
                MarcXchangeToDanMarc2LineFormatConverterTest.buildChunkItem(
                        MarcXchangeToDanMarc2LineFormatConverterTest.asMarcXchange(
                                MarcXchangeToDanMarc2LineFormatConverterTest.getMarcRecord()),
                        ChunkItem.Status.SUCCESS);

        MarcXchangeToLineFormatConverter converter = new MarcXchangeToLineFormatConverter();
        byte[] danmarc2LineFormat = converter.convert(chunkItem, StandardCharsets.UTF_8, null);
        assertThat(StringUtil.asString(danmarc2LineFormat),
                is("245 12 *aA @*programmer is born*beveryday@@dbc\n" +
                        "530 00 *ithis is to be used in test* testing blank subfield code\n$\n"));
    }

    @Test
    public void convertDanMarc2WithErroneousControlField() throws JobStoreException {
        ControlField f700 = new ControlField();
        f700.setTag("700");
        f700.setData("Illegal control field");

        MarcRecord marcRecord = MarcXchangeToDanMarc2LineFormatConverterTest.getMarcRecord()
                .addField(f700);

        ChunkItem chunkItem =
                MarcXchangeToDanMarc2LineFormatConverterTest.buildChunkItem(
                        MarcXchangeToDanMarc2LineFormatConverterTest.asMarcXchange(marcRecord),
                        ChunkItem.Status.SUCCESS);

        MarcXchangeToLineFormatConverter converter = new MarcXchangeToLineFormatConverter();
        byte[] danmarc2LineFormat = converter.convert(chunkItem, StandardCharsets.UTF_8, null);
        assertThat(StringUtil.asString(danmarc2LineFormat),
                is("245 12 *aA @*programmer is born*beveryday@@dbc\n" +
                        "530 00 *ithis is to be used in test* testing blank subfield code\n" +
                        "e01 00 *bfelt '700'*afelt '700' mangler delfelter\n$\n"));
    }
}
