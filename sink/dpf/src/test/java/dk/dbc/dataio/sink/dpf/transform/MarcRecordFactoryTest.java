package dk.dbc.dataio.sink.dpf.transform;

import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.Leader;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.binding.SubField;
import dk.dbc.marc.reader.DanMarc2LineFormatReader;
import dk.dbc.marc.reader.MarcReaderException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MarcRecordFactoryTest {
    private final MarcRecord simpleMarcRecord = new MarcRecord()
            .setLeader(new Leader().setData(DanMarc2LineFormatReader.DEFAULT_LEADER))
            .addField(
                    new DataField()
                            .setTag("001")
                            .setInd1('0')
                            .setInd2('0')
                            .addSubField(new SubField()
                                    .setCode('a')
                                    .setData("123456"))
                            .addSubField(new SubField()
                                    .setCode('b')
                                    .setData("870970")));

    private final String simpleMarcXchange =
            "<?xml version='1.0' encoding='UTF-8'?>\n" +
                    "<record xmlns='info:lc/xmlns/marcxchange-v1' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:schemaLocation='info:lc/xmlns/marcxchange-v1 http://www.loc.gov/standards/iso25577/marcxchange-1-1.xsd'>" +
                    "<leader>00000n    22000000  4500</leader>" +
                    "<datafield ind1='0' ind2='0' tag='001'>" +
                    "<subfield code='a'>123456</subfield>" +
                    "<subfield code='b'>870970</subfield>" +
                    "</datafield>" +
                    "</record>";

    @Test
    public void fromMarcXchange_invalidXml() {
        assertThrows(MarcReaderException.class, () -> MarcRecordFactory.fromMarcXchange("invalid XML".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void fromMarcXchange() throws MarcReaderException {
        MarcRecord marcRecord = MarcRecordFactory.fromMarcXchange(simpleMarcXchange.getBytes(StandardCharsets.UTF_8));
        assertThat(marcRecord, is(simpleMarcRecord));
    }

    @Test
    public void toMarcXchange() {
        MarcRecord marcRecord = new MarcRecord()
                .setLeader(new Leader().setData(DanMarc2LineFormatReader.DEFAULT_LEADER))
                .addField(
                        new DataField()
                                .setTag("001")
                                .setInd1('0')
                                .setInd2('0')
                                .addSubField(new SubField()
                                        .setCode('a')
                                        .setData("123456"))
                                .addSubField(new SubField()
                                        .setCode('b')
                                        .setData("870970")));
        byte[] bytes = MarcRecordFactory.toMarcXchange(marcRecord);
        assertThat(new String(bytes, StandardCharsets.UTF_8), is(simpleMarcXchange));
    }
}
