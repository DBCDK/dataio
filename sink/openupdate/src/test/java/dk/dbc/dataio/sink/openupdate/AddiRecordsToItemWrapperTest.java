package dk.dbc.dataio.sink.openupdate;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.sink.openupdate.connector.OpenUpdateServiceConnector;
import org.junit.Test;

import static dk.dbc.dataio.commons.types.ChunkItem.Status.SUCCESS;
import static dk.dbc.dataio.commons.utils.lang.StringUtil.asBytes;
import static org.mockito.Mockito.mock;

/**
 * Created by ThomasBerg on 20/10/15.
 */
public class AddiRecordsToItemWrapperTest extends AbstractOpenUpdateSinkTestBase {

    private OpenUpdateServiceConnector mockedOpenUpdateServiceConnector = mock(OpenUpdateServiceConnector.class);
    private final ChunkItem NO_PROCESSED_ITEM = null;
    private final ChunkItem chunkItemValid = new ChunkItem(1l, asBytes(getAddiAsString(getMetaXmlWithoutUpdateElement(), getContentXml())) , SUCCESS);
    private final OpenUpdateServiceConnector NO_OPENUPDATE_SERVICE_CONNECTOR = null;

    @Test(expected = NullPointerException.class)
    public void constructor_addiRecordsForItemArgIsNull_throws() {
        new AddiRecordsToItemWrapper(NO_PROCESSED_ITEM, mockedOpenUpdateServiceConnector);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_openUpdateServiceConnectorArgIsNull_throws() {
        new AddiRecordsToItemWrapper(chunkItemValid, NO_OPENUPDATE_SERVICE_CONNECTOR);
    }
//        final AddiRecord addiRecord = buildAddiRecord();
/*
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

*/
}
