package dk.dbc.dataio.sink.ims;

import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.oss.ns.updatemarcxchange.MarcXchangeRecord;
import jakarta.xml.bind.JAXBException;
import org.junit.Assert;
import org.junit.Test;

public class MarcXchangeRecordUnmarshallerTest {

    private final MarcXchangeRecordUnmarshaller marcXchangeRecordUnmarshaller = new MarcXchangeRecordUnmarshaller();

    private final String collection =
            "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
                    "<marcx:record format=\"danMARC2\"><marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">" +
                    "<marcx:subfield code=\"a\">title1</marcx:subfield></marcx:datafield>" +
                    "</marcx:record>" +
                    "</marcx:collection>";

    @Test
    public void toMarcXchangeRecord_validCollection_returns() throws JAXBException {
        MarcXchangeRecord marcXchangeRecord = marcXchangeRecordUnmarshaller.toMarcXchangeRecord(
                new ChunkItemBuilder().setData(collection).setId(3).build());

        Assert.assertEquals("3", marcXchangeRecord.getMarcXchangeRecordId());
    }

    @Test(expected = JAXBException.class)
    public void toMarcXchangeRecord_invalidCollection_returns() throws JAXBException {
        marcXchangeRecordUnmarshaller.toMarcXchangeRecord(new ChunkItemBuilder().setData("invalid").build());
    }
}
