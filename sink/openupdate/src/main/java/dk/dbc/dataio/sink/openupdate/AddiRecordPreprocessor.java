
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

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.sink.util.DocumentTransformer;
import dk.dbc.oss.ns.catalogingupdate.BibliographicRecord;
import dk.dbc.oss.ns.catalogingupdate.CatalogingUpdateServices;
import dk.dbc.oss.ns.catalogingupdate.ExtraRecordData;
import dk.dbc.oss.ns.catalogingupdate.RecordData;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordRequest;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;

public class AddiRecordPreprocessor extends DocumentTransformer{
    static final String NAMESPACE_URI             = "dk.dbc.dataio.processing";
    static final String ELEMENT                   = "sink-update-template";
    static final String UPDATE_TEMPLATE_ATTRIBUTE = "updateTemplate";
    static final String RECORD_SCHEMA             = "info:lc/xmlns/marcxchange-v1";
    static final String RECORD_PACKAGING          = "xml";

    /**
     * This method pre-processes an addi record according to the following rules:
     *
     * If the processing tag is not found within the meta data,
     * null is returned.
     *
     * If processing tag is found with attribute updateTemplate, updateRecord is called, with the attribute value and
     * content data of the addi record.
     *
     * @param addiRecord Addi record to pre-process
     * @return updateRecordResult if the template tag is located, null if the processing tag is not located
     * @throws IllegalArgumentException on invalid metadata or content
     */
    public UpdateRecordResult execute(AddiRecord addiRecord) throws IllegalArgumentException {
        UpdateRecordResult updateRecordResult = null;
        try {
            final Document metaDataDocument = byteArrayToDocument(addiRecord.getMetaData());
            final NodeList nodeList = metaDataDocument.getElementsByTagNameNS(NAMESPACE_URI, ELEMENT);

            if (nodeList.getLength() > 0) { // The processing tag has been located
                final Node processingNode = nodeList.item(0);

                // Create UpdateRecordRequest
                Document contentDataDocument = byteArrayToDocument(addiRecord.getContentData());
                UpdateRecordRequest updateRecordRequest = buildUpdateRecordRequest(processingNode, contentDataDocument);

                // Call UpdateRecord
                CatalogingUpdateServices catalogingUpdateServices = new CatalogingUpdateServices();
                updateRecordResult = catalogingUpdateServices.getCatalogingUpdatePort().updateRecord(updateRecordRequest);
            }
            return updateRecordResult;
        } catch (IOException | SAXException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Builds an UpdateRecordRequest
     * @param node containing the schema name
     * @param document containing addi content data as MarcXChange
     *
     * @return a new updateRecordRequest
     */
    private UpdateRecordRequest buildUpdateRecordRequest(Node node, Document document) {
        UpdateRecordRequest updateRecordRequest = new UpdateRecordRequest();
        updateRecordRequest.setSchemaName(((Element) node).getAttribute(UPDATE_TEMPLATE_ATTRIBUTE));
        updateRecordRequest.setBibliographicRecord(getMarcXChangeRecord(document));
        return updateRecordRequest;
    }

    /**
     * Creates a bibliographical record
     * @param document containing addi content data as MarcXChange
     * @return bibliographical record
     */
    private BibliographicRecord getMarcXChangeRecord(Document document){

        BibliographicRecord bibliographicRecord = new BibliographicRecord();
        bibliographicRecord.setRecordSchema(RECORD_SCHEMA);
        bibliographicRecord.setRecordPacking(RECORD_PACKAGING);

        RecordData recordData = new RecordData();
        recordData.getContent().add(document.getDocumentElement());
        bibliographicRecord.setRecordData(recordData);

        ExtraRecordData extraRecordData = new ExtraRecordData();
        bibliographicRecord.setExtraRecordData(extraRecordData);

        return bibliographicRecord;
    }
}
