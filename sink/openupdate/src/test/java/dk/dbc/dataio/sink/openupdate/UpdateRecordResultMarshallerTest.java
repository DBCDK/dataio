package dk.dbc.dataio.sink.openupdate;

import dk.dbc.oss.ns.catalogingupdate.MessageEntry;
import dk.dbc.oss.ns.catalogingupdate.Messages;
import dk.dbc.oss.ns.catalogingupdate.Type;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;
import dk.dbc.oss.ns.catalogingupdate.UpdateStatusEnum;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class UpdateRecordResultMarshallerTest {
    @Test
    public void sXml_updateRecordResultIsNotNull_ok() throws JAXBException {
        MessageEntry messageEntry = new MessageEntry();
        messageEntry.setType(Type.ERROR);
        messageEntry.setOrdinalPositionOfField(1);
        messageEntry.setOrdinalPositionOfSubfield(1);
        messageEntry.setMessage("UpdateRecordResultMarshallerTest validation error");

        UpdateRecordResult updateRecordResult = new UpdateRecordResult();
        updateRecordResult.setUpdateStatus(UpdateStatusEnum.FAILED);
        updateRecordResult.setMessages(new Messages());
        updateRecordResult.getMessages().getMessageEntry().add(messageEntry);

        assertThat(new UpdateRecordResultMarshaller().asXml(updateRecordResult), is(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                        "<updateRecordResult xmlns=\"http://oss.dbc.dk/ns/catalogingUpdate\">" +
                        "<updateStatus>failed</updateStatus>" +
                        "<messages>" +
                        "<messageEntry>" +
                        "<type>error</type>" +
                        "<ordinalPositionOfField>1</ordinalPositionOfField>" +
                        "<ordinalPositionOfSubfield>1</ordinalPositionOfSubfield>" +
                        "<message>UpdateRecordResultMarshallerTest validation error</message>" +
                        "</messageEntry>" +
                        "</messages>" +
                        "</updateRecordResult>"));
    }
}
