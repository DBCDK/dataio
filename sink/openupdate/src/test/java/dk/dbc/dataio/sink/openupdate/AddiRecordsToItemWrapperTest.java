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
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.sink.openupdate.connector.OpenUpdateServiceConnector;
import dk.dbc.oss.ns.catalogingupdate.BibliographicRecord;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import javax.xml.ws.WebServiceException;
import java.util.ArrayList;
import java.util.List;

import static dk.dbc.dataio.commons.types.ChunkItem.Status.SUCCESS;
import static dk.dbc.dataio.commons.utils.lang.StringUtil.asString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
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

public class
AddiRecordsToItemWrapperTest extends AbstractOpenUpdateSinkTestBase {
    private AddiRecordPreprocessor addiRecordPreprocessor = new AddiRecordPreprocessor();
    private OpenUpdateServiceConnector mockedOpenUpdateServiceConnector = mock(OpenUpdateServiceConnector.class);
    private final UpdateRecordResultMarshaller updateRecordResultMarshaller = new UpdateRecordResultMarshaller();
    private final ChunkItem chunkItemWithValidAddiRecords = buildChunkItemWithMultipleValidAddiRecords();
    private final String queueProvider = "queue";

    private final ChunkItem processedChunkItemValid = new ChunkItemBuilder()
            .setData(getAddi(getMetaXml(), getMarcExchangeValidatedOkByWebservice()))
            .setStatus(SUCCESS).build();


    @Test(expected = NullPointerException.class)
    public void constructor_addiRecordsForItemArgIsNull_throws() {
        new AddiRecordsToItemWrapper(null, addiRecordPreprocessor, mockedOpenUpdateServiceConnector, updateRecordResultMarshaller);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_addiRecordPreprocessorArgIsNull_throws() {
        new AddiRecordsToItemWrapper(processedChunkItemValid, null, mockedOpenUpdateServiceConnector, updateRecordResultMarshaller);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_openUpdateServiceConnectorArgIsNull_throws() {
        new AddiRecordsToItemWrapper(processedChunkItemValid, addiRecordPreprocessor, null, updateRecordResultMarshaller);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_updateRecordResultMarshallerArgIsNull_throws() {
        new AddiRecordsToItemWrapper(processedChunkItemValid, addiRecordPreprocessor, mockedOpenUpdateServiceConnector, null);
    }

    @Test
    public void callOpenUpdateWebServiceForEachAddiRecord_OK() throws JAXBException {
        // Expectations
        when(mockedOpenUpdateServiceConnector.updateRecord(anyString(), anyString(), any(BibliographicRecord.class), anyString()))
                .thenReturn(getWebserviceResultValidatedOk());

        // Subject Under Test
        final ChunkItem chunkItemForDelivery = newAddiRecordsToItemWrapper().callOpenUpdateWebServiceForEachAddiRecord(queueProvider);

        // Verification
        assertNotNull(chunkItemForDelivery);
        assertEquals("Expected status OK", chunkItemForDelivery.getStatus(), ChunkItem.Status.SUCCESS);
        assertFalse(asString(chunkItemForDelivery.getData()).contains("errorMessages"));
        assertTrue(asString(chunkItemForDelivery.getData()).contains("OK"));
        assertThat(chunkItemForDelivery.getDiagnostics(), is(nullValue()));
        assertThat(chunkItemForDelivery.getTrackingId(), is(DBC_TRACKING_ID_VALUE));
    }

    @Test
    public void callOpenUpdateWebServiceForEachAddiRecord_validationError() throws JAXBException {
        // Expectations
        when(mockedOpenUpdateServiceConnector.updateRecord(anyString(), anyString(), any(BibliographicRecord.class), anyString()))
                .thenReturn(getWebserviceResultWithValidationErrors());

        // Subject Under Test
        final ChunkItem chunkItemForDelivery = newAddiRecordsToItemWrapper().callOpenUpdateWebServiceForEachAddiRecord(queueProvider);

        // Verification
        String chunkItemDataAsString = asString(chunkItemForDelivery.getData());
        assertNotNull(chunkItemForDelivery);
        assertEquals("Expected status FAILURE", chunkItemForDelivery.getStatus(), ChunkItem.Status.FAILURE);
        assertTrue(chunkItemDataAsString.contains("message"));
        assertTrue(StringUtils.countMatches(chunkItemDataAsString, "<message>") == 3);
        assertThat(chunkItemForDelivery.getDiagnostics(), is(notNullValue()));
        assertThat(chunkItemForDelivery.getDiagnostics().size(), is(3));
        assertThat(chunkItemForDelivery.getTrackingId(), is(DBC_TRACKING_ID_VALUE));
    }

    @Test
    public void callOpenUpdateWebServiceForEachAddiRecord_stackTrace() throws JAXBException {
        // Expectations
        when(mockedOpenUpdateServiceConnector.updateRecord(anyString(), anyString(), any(BibliographicRecord.class), anyString()))
                .thenThrow(new WebServiceException());

        // Subject Under Test
        final ChunkItem chunkItemForDelivery = newAddiRecordsToItemWrapper().callOpenUpdateWebServiceForEachAddiRecord(queueProvider);

        // Verification
        assertNotNull(chunkItemForDelivery);
        assertEquals("Expected status FAILURE", chunkItemForDelivery.getStatus(), ChunkItem.Status.FAILURE);
        assertTrue(asString(chunkItemForDelivery.getData()).contains("FAILED_STACKTRACE"));
        assertThat(chunkItemForDelivery.getDiagnostics().size(), is(1));
        assertThat(chunkItemForDelivery.getTrackingId(), is(DBC_TRACKING_ID_VALUE));
    }

    @Test
    public void callOpenUpdateWebServiceForEachAddiRecordWithMultipleAddiRecords_OK() throws JAXBException {
        // Expectations
        when(mockedOpenUpdateServiceConnector.updateRecord(anyString(), anyString(), any(BibliographicRecord.class), anyString()))
                .thenReturn(getWebserviceResultValidatedOk());

        // Subject Under Test
        final ChunkItem chunkItemForDelivery = newAddiRecordsToItemWrapper().callOpenUpdateWebServiceForEachAddiRecord(queueProvider);

        // Verification
        assertNotNull(chunkItemForDelivery);
        assertEquals("Expected status OK", chunkItemForDelivery.getStatus(), ChunkItem.Status.SUCCESS);
        assertFalse(asString(chunkItemForDelivery.getData()).contains("errorMessages"));
        assertTrue(asString(chunkItemForDelivery.getData()).contains("OK"));
        assertThat(chunkItemForDelivery.getDiagnostics(), is(nullValue()));
        verify(mockedOpenUpdateServiceConnector, times(3)).updateRecord(anyString(), anyString(), any(BibliographicRecord.class), anyString());
        assertThat(chunkItemForDelivery.getTrackingId(), is(DBC_TRACKING_ID_VALUE));
    }

    /*
     * Private methods
     */

    private AddiRecordsToItemWrapper newAddiRecordsToItemWrapper() {
        return new AddiRecordsToItemWrapper(
                chunkItemWithValidAddiRecords,
                addiRecordPreprocessor,
                mockedOpenUpdateServiceConnector,
                updateRecordResultMarshaller);
    }

    private ChunkItem buildChunkItemWithMultipleValidAddiRecords() {
        return new ChunkItemBuilder().setData(getAddi(buildListOfAddRecords())).setTrackingId(DBC_TRACKING_ID_VALUE).setStatus(SUCCESS).build();
    }

    private List<AddiRecordWrapper> buildListOfAddRecords() {
        List<AddiRecordWrapper> addiRecordWrappers = new ArrayList<>();
        addiRecordWrappers.add(new AddiRecordWrapper(getMetaXml(), getMarcExchangeValidatedOkByWebservice()));
        addiRecordWrappers.add(new AddiRecordWrapper(getMetaXml(), getMarcExchangeValidatedOkByWebservice()));
        addiRecordWrappers.add(new AddiRecordWrapper(getMetaXml(), getMarcExchangeValidatedOkByWebservice()));
        return addiRecordWrappers;
    }
}