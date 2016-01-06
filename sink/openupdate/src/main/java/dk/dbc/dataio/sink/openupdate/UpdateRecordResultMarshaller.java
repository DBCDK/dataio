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


import dk.dbc.oss.ns.catalogingupdate.ObjectFactory;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/**
 * Handles marshalling of dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult
 */
public class UpdateRecordResultMarshaller {
    private final Marshaller marshaller;
    private final ObjectFactory objectFactory;

    public UpdateRecordResultMarshaller() {
        try {
            marshaller = JAXBContext.newInstance(UpdateRecordResult.class).createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.toString());
            objectFactory = new ObjectFactory();
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Converts a updateRecordResult instance to String
     * @param updateRecordResult to convert
     * @return string representation of the updateRecordResult provided as input
     * @throws JAXBException If any unexpected problem occurs during the marshalling.
     */
    public String asXml(UpdateRecordResult updateRecordResult) throws JAXBException {
        JAXBElement<UpdateRecordResult> wrappedUpdateRecordResult = objectFactory.createUpdateRecordResult(updateRecordResult);
        StringWriter stringWriter = new StringWriter();
        marshaller.marshal(wrappedUpdateRecordResult, stringWriter);
        return stringWriter.toString();
    }
}
