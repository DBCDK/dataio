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

package dk.dbc.dataio.sink.es;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.addi.AddiContext;
import dk.dbc.dataio.addi.AddiException;
import dk.dbc.dataio.addi.bindings.EsReferenceData;
import dk.dbc.dataio.commons.utils.lang.JaxpUtil;
import dk.dbc.marc.DanMarc2Charset;
import dk.dbc.marc.Iso2709Packer;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AddiRecordPreprocessor {
    private final AddiContext addiContext = new AddiContext();

    /**
     * This method pre-processes an addi record according to the following rules:
     *
     * If metadata contains sink-processing element with attribute encodeAs2709 set to true, the content data is converted to iso2709.
     * DBCTrackingId is added as attribute to metadata info element if a non-null tracking ID value is given.
     * All dataIO specific elements are stripped from the metadata of the returned addi record.
     *
     * @param addiRecord Addi record to pre-process
     * @param trackingId tracking ID
     * @return pre-processed Addi record
     * @throws IllegalArgumentException on invalid metadata or content
     */
    public AddiRecord execute(AddiRecord addiRecord, String trackingId) throws IllegalArgumentException {
        try {
            byte[] content = addiRecord.getContentData();
            final EsReferenceData esReferenceData = addiContext.getEsReferenceData(addiRecord);
            if (esReferenceData.sinkDirectives != null && esReferenceData.sinkDirectives.encodeAs2709) {
                content = Iso2709Packer.create2709FromMarcXChangeRecord(
                        JaxpUtil.toDocument(addiRecord.getContentData()), new DanMarc2Charset());
            }
            esReferenceData.esDirectives.withTrackingId(trackingId);
            return new AddiRecord(esReferenceData.toXmlString().getBytes(StandardCharsets.UTF_8), content);
        } catch (AddiException | IOException | SAXException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
