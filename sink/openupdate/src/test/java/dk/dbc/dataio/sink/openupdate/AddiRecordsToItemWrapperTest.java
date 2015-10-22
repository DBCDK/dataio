package dk.dbc.dataio.sink.openupdate;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.sink.openupdate.connector.OpenUpdateServiceConnector;
import dk.dbc.oss.ns.catalogingupdate.BibliographicRecord;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import javax.xml.ws.WebServiceException;

import static dk.dbc.dataio.commons.types.ChunkItem.Status.SUCCESS;
import static dk.dbc.dataio.commons.utils.lang.StringUtil.asBytes;
import static dk.dbc.dataio.commons.utils.lang.StringUtil.asString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by ThomasBerg on 20/10/15.
 */
public class AddiRecordsToItemWrapperTest extends AbstractOpenUpdateSinkTestBase {

    private OpenUpdateServiceConnector mockedOpenUpdateServiceConnector = mock(OpenUpdateServiceConnector.class);
    private final ChunkItem NO_PROCESSED_ITEM = null;
    private final ChunkItem processedChunkItemValid = new ChunkItem(
            1l,
            asBytes(getAddiAsString(getMetaXml(AddiRecordPreprocessor.UPDATE_TEMPLATE_ATTRIBUTE), TestData.MARCX_VALID_FROM_PROCESSING)),
            SUCCESS);
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
        when(mockedOpenUpdateServiceConnector.updateRecord(anyString(), (BibliographicRecord)anyObject())).thenReturn(TestData.getWebserviceResultOK());

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
        when(mockedOpenUpdateServiceConnector.updateRecord(anyString(), (BibliographicRecord)anyObject())).thenReturn(TestData.webserviceResultWithValidationErrors());

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
    public void callOpenUpdateWebServiceForEachAddiRecord_stackTrace() throws JAXBException {

        // Expectations
        when(mockedOpenUpdateServiceConnector.updateRecord(anyString(), (BibliographicRecord)anyObject())).thenThrow(WebServiceException.class);

        // Subject Under Test
        AddiRecordsToItemWrapper itemWrapper = new AddiRecordsToItemWrapper(processedChunkItemValid, mockedOpenUpdateServiceConnector);
        final ChunkItem chunkItemForDelivery = itemWrapper.callOpenUpdateWebServiceForEachAddiRecord();
        assertNotNull(chunkItemForDelivery);
        assertEquals("Expected status FAILURE", chunkItemForDelivery.getStatus(), ChunkItem.Status.FAILURE);
        assertTrue(asString(chunkItemForDelivery.getData()).contains("FAILED_STACKTRACE"));
    }
}