package dk.dbc.dataio.sink.ims;

import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.oss.ns.updatemarcxchange.MarcXchangeRecord;
import org.junit.Test;

import javax.xml.bind.JAXBException;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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

        assertThat(marcXchangeRecord.getMarcXchangeRecordId(), is("3"));
    }

    @Test
    public void toMarcXchangeRecord_invalidCollection_returns() {
        assertThat(() -> marcXchangeRecordUnmarshaller.toMarcXchangeRecord(
                new ChunkItemBuilder().setData("invalid").build()), isThrowing(JAXBException.class));
    }
}
