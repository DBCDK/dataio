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

package dk.dbc.dataio.sink.ims;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.oss.ns.updatemarcxchange.MarcXchangeRecord;
import info.lc.xmlns.marcxchange_v1.CollectionType;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;

public class MarcXchangeRecordUnmarshaller {

    private final Unmarshaller unmarshaller;

    public MarcXchangeRecordUnmarshaller() throws IllegalStateException {
        try {
            final JAXBContext jaxbContext = JAXBContext.newInstance(CollectionType.class);
            unmarshaller = jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            throw new IllegalStateException("Exception caught while instantiating JaxbContext", e);
        }
    }

    /* Transforms given MARC exchange collection into its corresponding CollectionType
      representation and wraps it in a MarcXchangeRecord
    */
    public MarcXchangeRecord toMarcXchangeRecord(ChunkItem chunkItem) throws JAXBException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(chunkItem.getData());
        final JAXBElement<CollectionType> jaxbCollection = unmarshaller.unmarshal(new StreamSource(byteArrayInputStream), CollectionType.class);
        final MarcXchangeRecord marcXchangeRecord = new MarcXchangeRecord();
        marcXchangeRecord.setCollection(jaxbCollection.getValue());
        return marcXchangeRecord;
    }
}
