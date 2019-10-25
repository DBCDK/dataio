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

import org.w3c.dom.Document;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.dom.DOMResult;
import java.nio.charset.StandardCharsets;

/**
 * Handles marshalling of dk.dbc.dataio.sink.openupdate.bindings.BibliographicRecordExtraData
 */
public class BibliographicRecordExtraDataMarshaller {
    private final Marshaller marshaller;

    public BibliographicRecordExtraDataMarshaller() {
        try {
            marshaller = JAXBContext.newInstance(BibliographicRecordExtraData.class).createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.toString());
            marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, BibliographicRecordExtraData.NAMESPACE);
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Converts a BibliographicRecordExtraData instance to W3C Document object
     * @param data BibliographicRecordExtraData to convert
     * @return Document representation
     * @throws IllegalArgumentException if given null-valued argument
     * @throws JAXBException If any unexpected problem occurs during the marshalling.
     */
    public Document toXmlDocument(BibliographicRecordExtraData data) throws IllegalArgumentException, JAXBException {
        final DOMResult domResult = new DOMResult();
        marshaller.marshal(data, domResult);
        return (Document) domResult.getNode();
    }
}
