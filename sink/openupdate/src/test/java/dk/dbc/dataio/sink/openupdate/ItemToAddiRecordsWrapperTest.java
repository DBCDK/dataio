package dk.dbc.dataio.sink.openupdate;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.sink.openupdate.connector.OpenUpdateServiceConnector;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;

/**
 * Created by ThomasBerg on 20/10/15.
 */
public class ItemToAddiRecordsWrapperTest extends AbstractOpenUpdateSinkTestBase {

    private OpenUpdateServiceConnector mockedOpenUpdateServiceConnector = mock(OpenUpdateServiceConnector.class);
    private final List<AddiRecord> NO_ADDI_RECORDS_FOR_ITEM = null;
    private final OpenUpdateServiceConnector NO_OPENUPDATE_SERVICE_CONNECTOR = null;

    @Test(expected = NullPointerException.class)
    public void constructor_addiRecordsForItemArgIsNull_throws() {
        new ItemToAddiRecordsWrapper(NO_ADDI_RECORDS_FOR_ITEM, mockedOpenUpdateServiceConnector);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_openUpdateServiceConnectorArgIsNull_throws() {
        final AddiRecord addiRecord = buildAddiRecord();
        new ItemToAddiRecordsWrapper(buildListOfAddiRecords(), NO_OPENUPDATE_SERVICE_CONNECTOR);
    }

    private List<AddiRecord> buildListOfAddiRecords() {
        List<AddiRecord> listOfAddiRecords = new ArrayList<>();
        listOfAddiRecords.add(buildAddiRecord());
        listOfAddiRecords.add(buildAddiRecord());
        listOfAddiRecords.add(buildAddiRecord());
        listOfAddiRecords.add(buildAddiRecord());
        listOfAddiRecords.add(buildAddiRecord());
        return listOfAddiRecords;
    }

    private AddiRecord buildAddiRecord() {
        return toAddiRecord(getAddi(getMetaXmlWithoutUpdateElement(), getContentXml()));
    }
}
