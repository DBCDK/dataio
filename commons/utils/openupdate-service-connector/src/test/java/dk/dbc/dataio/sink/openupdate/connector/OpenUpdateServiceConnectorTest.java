package dk.dbc.dataio.sink.openupdate.connector;

import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.binding.SubField;
import dk.dbc.oss.ns.catalogingupdate.MessageEntry;
import dk.dbc.oss.ns.catalogingupdate.Messages;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class OpenUpdateServiceConnectorTest {
    private final OpenUpdateServiceConnector connector = new OpenUpdateServiceConnector("endpoint");

    @Test
    public void toErrorFields_whenResultContainsNoErrors() {
        final UpdateRecordResult result = new UpdateRecordResult();
        assertThat(connector.toErrorFields("e01", result, null),
                is(Collections.emptyList()));
    }

    @Test
    public void toErrorFields_noMarcRecord() {
        final MessageEntry messageEntry1 = new MessageEntry();
        messageEntry1.setMessage("err1");
        final MessageEntry messageEntry2 = new MessageEntry();
        messageEntry2.setMessage("err2");
        final Messages messages = new Messages();
        messages.getMessageEntry().add(messageEntry1);
        messages.getMessageEntry().add(messageEntry2);
        final UpdateRecordResult result = new UpdateRecordResult();
        result.setMessages(messages);

        assertThat(connector.toErrorFields("e01", result, null),
                is(Arrays.asList(
                        new DataField()
                                .setTag("e01")
                                .setInd1('0')
                                .setInd2('0')
                                .addSubField(
                                        new SubField()
                                                .setCode('a')
                                                .setData("err1")),
                        new DataField()
                                .setTag("e01")
                                .setInd1('0')
                                .setInd2('0')
                                .addSubField(
                                        new SubField()
                                                .setCode('a')
                                                .setData("err2")))));
    }

    @Test
    public void toErrorFields_datafieldOrdinalOnly() {
        final MessageEntry messageEntry = new MessageEntry();
        messageEntry.setMessage("err");
        messageEntry.setOrdinalPositionOfField(1);
        final Messages messages = new Messages();
        messages.getMessageEntry().add(messageEntry);
        final UpdateRecordResult result = new UpdateRecordResult();
        result.setMessages(messages);

        final MarcRecord marcRecord = new MarcRecord()
                .addField(new DataField().setTag("001"))
                .addField(new DataField().setTag("002"))
                .addField(new DataField().setTag("003"));

        assertThat(connector.toErrorFields("e01", result, marcRecord),
                is(Collections.singletonList(
                        new DataField()
                                .setTag("e01")
                                .setInd1('0')
                                .setInd2('0')
                                .addSubField(
                                        new SubField()
                                                .setCode('a')
                                                .setData("err"))
                                .addSubField(
                                        new SubField()
                                                .setCode('b')
                                                .setData("felt 002")))));
    }

    @Test
    public void toErrorFields_datafieldOrdinalOnlyAndSubfieldOrdinal() {
        final MessageEntry messageEntry = new MessageEntry();
        messageEntry.setMessage("err");
        messageEntry.setOrdinalPositionOfField(0);
        messageEntry.setOrdinalPositionOfSubfield(1);
        final Messages messages = new Messages();
        messages.getMessageEntry().add(messageEntry);
        final UpdateRecordResult result = new UpdateRecordResult();
        result.setMessages(messages);

        final MarcRecord marcRecord = new MarcRecord()
                .addField(new DataField().setTag("001")
                        .addSubField(new SubField().setCode('a'))
                        .addSubField(new SubField().setCode('b'))
                        .addSubField(new SubField().setCode('b')))
                .addField(new DataField().setTag("002"));

        assertThat(connector.toErrorFields("e01", result, marcRecord),
                is(Collections.singletonList(
                        new DataField()
                                .setTag("e01")
                                .setInd1('0')
                                .setInd2('0')
                                .addSubField(
                                        new SubField()
                                                .setCode('a')
                                                .setData("err"))
                                .addSubField(
                                        new SubField()
                                                .setCode('b')
                                                .setData("delfelt b felt 001")))));
    }
}
