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

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.sink.openupdate.connector.OpenUpdateServiceConnector;
import dk.dbc.oss.ns.catalogingupdate.BibliographicRecord;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import javax.xml.ws.WebServiceException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static dk.dbc.dataio.commons.types.ChunkItem.Status.SUCCESS;
import static dk.dbc.dataio.commons.utils.lang.StringUtil.asString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AddiRecordsToItemWrapperTest extends AbstractOpenUpdateSinkTestBase {

    private OpenUpdateServiceConnector mockedOpenUpdateServiceConnector = mock(OpenUpdateServiceConnector.class);
    private final ChunkItem NO_PROCESSED_ITEM = null;

    private final ChunkItem processedChunkItemValid = new ChunkItem(
            1l,
            getAddi(getMetaXml(AddiRecordPreprocessor.UPDATE_TEMPLATE_ATTRIBUTE), OpenUpdateSinkTestData.MARCX_VALID_FROM_PROCESSING),
            SUCCESS);

    private final ChunkItem processedChunkItemValidWithMultipleAddiRercords = new ChunkItem(
            1l,
            getAddi(buildListOfAddRecords()),
            SUCCESS);

    private List<AddiRecordWrapper> buildListOfAddRecords() {
        List<AddiRecordWrapper> addiRecordWrappers = new ArrayList<>();
        addiRecordWrappers.add(new AddiRecordWrapper(getMetaXml(AddiRecordPreprocessor.UPDATE_TEMPLATE_ATTRIBUTE), OpenUpdateSinkTestData.MARCX_VALID_FROM_PROCESSING));
        addiRecordWrappers.add(new AddiRecordWrapper(getMetaXml(AddiRecordPreprocessor.UPDATE_TEMPLATE_ATTRIBUTE), OpenUpdateSinkTestData.MARCX_VALID_FROM_PROCESSING));
        addiRecordWrappers.add(new AddiRecordWrapper(getMetaXml(AddiRecordPreprocessor.UPDATE_TEMPLATE_ATTRIBUTE), OpenUpdateSinkTestData.MARCX_VALID_FROM_PROCESSING));
        return addiRecordWrappers;
    }

    private final OpenUpdateServiceConnector NO_OPENUPDATE_SERVICE_CONNECTOR = null;

    @Test(expected = NullPointerException.class)
    public void constructor_addiRecordsForItemArgIsNull_throws() {
        new AddiRecordsToItemWrapper(NO_PROCESSED_ITEM, mockedOpenUpdateServiceConnector);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_openUpdateServiceConnectorArgIsNull_throws() {
        new AddiRecordsToItemWrapper(processedChunkItemValid, NO_OPENUPDATE_SERVICE_CONNECTOR);
    }

    @Test
    public void callOpenUpdateWebServiceForEachAddiRecord_OK() throws JAXBException {

        // Expectations
        when(mockedOpenUpdateServiceConnector.updateRecord(anyString(), any(BibliographicRecord.class), any(UUID.class))).thenReturn(OpenUpdateSinkTestData.getWebserviceResultOK());

        // Subject Under Test
        AddiRecordsToItemWrapper itemWrapper = new AddiRecordsToItemWrapper(processedChunkItemValid, mockedOpenUpdateServiceConnector);
        final ChunkItem chunkItemForDelivery = itemWrapper.callOpenUpdateWebServiceForEachAddiRecord();
        assertNotNull(chunkItemForDelivery);
        assertEquals("Expected status OK", chunkItemForDelivery.getStatus(), ChunkItem.Status.SUCCESS);
        assertFalse(asString(chunkItemForDelivery.getData()).contains("errorMessages"));
        assertTrue(asString(chunkItemForDelivery.getData()).contains("OK"));
    }

    @Test
    public void callOpenUpdateWebServiceForEachAddiRecord_validationError() throws JAXBException {

        // Expectations
        when(mockedOpenUpdateServiceConnector.updateRecord(anyString(), any(BibliographicRecord.class), any(UUID.class))).thenReturn(OpenUpdateSinkTestData.webserviceResultWithValidationErrors());

        // Subject Under Test
        AddiRecordsToItemWrapper itemWrapper = new AddiRecordsToItemWrapper(processedChunkItemValid, mockedOpenUpdateServiceConnector);
        final ChunkItem chunkItemForDelivery = itemWrapper.callOpenUpdateWebServiceForEachAddiRecord();
        String chunkItemDataAsString = asString(chunkItemForDelivery.getData());
        assertNotNull(chunkItemForDelivery);
        assertEquals("Expected status FAILURE", chunkItemForDelivery.getStatus(), ChunkItem.Status.FAILURE);
        assertTrue(chunkItemDataAsString.contains("errorMessages"));
        assertTrue(StringUtils.countMatches(chunkItemDataAsString, "<errorMessages>") == 3);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void callOpenUpdateWebServiceForEachAddiRecord_stackTrace() throws JAXBException {

        // Expectations
        when(mockedOpenUpdateServiceConnector.updateRecord(anyString(), any(BibliographicRecord.class), any(UUID.class))).thenThrow(WebServiceException.class);

        // Subject Under Test
        AddiRecordsToItemWrapper itemWrapper = new AddiRecordsToItemWrapper(processedChunkItemValid, mockedOpenUpdateServiceConnector);
        final ChunkItem chunkItemForDelivery = itemWrapper.callOpenUpdateWebServiceForEachAddiRecord();
        assertNotNull(chunkItemForDelivery);
        assertEquals("Expected status FAILURE", chunkItemForDelivery.getStatus(), ChunkItem.Status.FAILURE);
        assertTrue(asString(chunkItemForDelivery.getData()).contains("FAILED_STACKTRACE"));
    }

    @Test
    public void callOpenUpdateWebServiceForEachAddiRecordWithMultipleAddiRecords_OK() throws JAXBException {

        // Expectations
        when(mockedOpenUpdateServiceConnector.updateRecord(anyString(), any(BibliographicRecord.class), any(UUID.class))).thenReturn(OpenUpdateSinkTestData.getWebserviceResultOK());

        // Subject Under Test
        AddiRecordsToItemWrapper itemWrapper = new AddiRecordsToItemWrapper(processedChunkItemValidWithMultipleAddiRercords, mockedOpenUpdateServiceConnector);
        final ChunkItem chunkItemForDelivery = itemWrapper.callOpenUpdateWebServiceForEachAddiRecord();
        assertNotNull(chunkItemForDelivery);
        assertEquals("Expected status OK", chunkItemForDelivery.getStatus(), ChunkItem.Status.SUCCESS);
        assertFalse(asString(chunkItemForDelivery.getData()).contains("errorMessages"));
        assertTrue(asString(chunkItemForDelivery.getData()).contains("OK"));
        verify(mockedOpenUpdateServiceConnector, times(3)).updateRecord(anyString(), any(BibliographicRecord.class), any(UUID.class));
    }
}