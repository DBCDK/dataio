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

package dk.dbc.dataio.addi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.addi.bindings.EsReferenceData;
import dk.dbc.invariant.InvariantUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * This class is an abstraction for managing AddiRecord metadata to Java bindings.
 * This class is thread safe.
 */
public class AddiContext {
    private final ObjectMapper xmlMapper;

    public AddiContext() {
        xmlMapper = new XmlMapper();
    }

    /**
     * Extracts ES reference data from given Addi record
     * @param addiRecord addi record from which to extract ES reference data
     * @return ES reference data
     * @throws AddiException if given null-valued Addi record or on failure to extract ES reference data
     */
    public EsReferenceData getEsReferenceData(AddiRecord addiRecord) throws AddiException {
        try {
            InvariantUtil.checkNotNullOrThrow(addiRecord, "addiRecord");
            /* Apparently not all Stax implementations are able to create XMLStreamReader or XMLEventReader
               from a byte array, so for now we need to wrap as a string. This also means that we currently
               only support Addi XML metadata in UTF-8 encoding. */
            return xmlMapper.readValue(new String(addiRecord.getMetaData(), StandardCharsets.UTF_8), EsReferenceData.class);
        } catch (IOException | RuntimeException e) {
            throw new AddiException(e);
        }
    }
}
