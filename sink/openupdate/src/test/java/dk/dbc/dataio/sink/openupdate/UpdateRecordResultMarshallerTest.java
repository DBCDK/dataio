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

package dk.dbc.dataio.sink.openupdate;

import dk.dbc.oss.ns.catalogingupdate.MessageEntry;
import dk.dbc.oss.ns.catalogingupdate.Messages;
import dk.dbc.oss.ns.catalogingupdate.Type;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;
import dk.dbc.oss.ns.catalogingupdate.UpdateStatusEnum;
import org.junit.Test;

import javax.xml.bind.JAXBException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class UpdateRecordResultMarshallerTest {
    @Test
    public void sXml_updateRecordResultIsNotNull_ok() throws JAXBException {
        final MessageEntry messageEntry = new MessageEntry();
        messageEntry.setType(Type.ERROR);
        messageEntry.setOrdinalPositionOfField(1);
        messageEntry.setOrdinalPositionOfSubfield(1);
        messageEntry.setMessage("UpdateRecordResultMarshallerTest validation error");

        final UpdateRecordResult updateRecordResult = new UpdateRecordResult();
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
