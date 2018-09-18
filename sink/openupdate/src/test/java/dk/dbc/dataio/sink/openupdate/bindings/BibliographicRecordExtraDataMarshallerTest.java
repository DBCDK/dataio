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

package dk.dbc.dataio.sink.openupdate.bindings;

import org.junit.Test;
import org.w3c.dom.Document;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class BibliographicRecordExtraDataMarshallerTest {
    private final BibliographicRecordExtraDataMarshaller marshaller = new BibliographicRecordExtraDataMarshaller();

    @Test(expected = IllegalArgumentException.class)
    public void toXmlDocument_dataArgIsNull_throws() throws JAXBException {
        marshaller.toXmlDocument(null);
    }

    @Test
    public void toXmlDocument() throws JAXBException {
        final BibliographicRecordExtraData bibliographicRecordExtraData = new BibliographicRecordExtraData();
        bibliographicRecordExtraData.setProviderName("myProvider");
        bibliographicRecordExtraData.setPriority(1000);

        final BibliographicRecordExtraData unmarshalled = unmarshall(marshaller.toXmlDocument(bibliographicRecordExtraData));
        assertThat("providerName", unmarshalled.getProviderName(),
                is(bibliographicRecordExtraData.getProviderName()));
        assertThat("priority", unmarshalled.getPriority(),
                is(1000));
    }

    public static BibliographicRecordExtraData unmarshall(Document document) throws JAXBException {
        final Unmarshaller unmarshaller = JAXBContext.newInstance(BibliographicRecordExtraData.class).createUnmarshaller();
        return unmarshaller.unmarshal(document.getDocumentElement(), BibliographicRecordExtraData.class).getValue();
    }
}