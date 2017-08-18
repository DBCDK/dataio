/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.oclc.wciru;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.sink.worldcat.ChunkItemWithWorldCatAttributes;
import dk.dbc.dataio.sink.worldcat.Holding;
import dk.dbc.dataio.sink.worldcat.WciruServiceBroker;
import dk.dbc.dataio.sink.worldcat.WorldCatAttributes;
import dk.dbc.ocnrepo.dto.WorldCatEntity;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Ignore
public class WciruServiceBrokerTest {
    // TODO: 09/03/2017 tests needs to be rewritten. Only in this state because OCLC stopped
    private final String record = "<data/>";
    private final String ocn = "ocn";
    private final List<Holding> holdings = new ArrayList<>();
    private final WciruServiceConnector wciruServiceConnector = mock(WciruServiceConnector.class);
//    private WorldCatAttributes worldCatAttributes;
//    private ChunkItemWithWorldCatAttributes chunkItemWithWorldCatAttributes;
    private WorldCatEntity worldCatEntity;

    @Before
    public void setUp() throws IOException, JSONBException {
        holdings.add(new Holding().withAction(Holding.Action.INSERT).withSymbol(WciruServiceBroker.PRIMARY_HOLDING_SYMBOL));
        worldCatEntity = new WorldCatEntity().withPid("1").withOcn("1").withAgencyId(123);
    }

    @Test
    public void push_deleteRecord_ok() throws WciruServiceConnectorException, IOException, JSONBException {

        WorldCatAttributes worldCatAttributes = new WorldCatAttributes()
                .withPid("testPid")
                .withOcn("testOcn")
                .withCompareRecord("testRecord")
                .withHoldings(Arrays.asList(
                        new Holding().withSymbol("ABCDE").withAction(Holding.Action.DELETE),
                        new Holding().withSymbol("FGHIJ").withAction(Holding.Action.DELETE)
                ));

        ChunkItem chunkItem = new ChunkItemBuilder().setData("2\n{}\n7\n" + record + "\n").build();
        ChunkItemWithWorldCatAttributes chunkItemWithWorldCatAttributes = ChunkItemWithWorldCatAttributes.of(chunkItem).get(0).withWorldCatAttributes(worldCatAttributes);

        final WciruServiceBroker wciruServiceBroker = new WciruServiceBroker(wciruServiceConnector);
        final DiagnosticsType diagnosticsType = new DiagnosticsType();
        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setMessage("SUCCESS");
        diagnosticsType.diagnostic = Collections.singletonList(diagnostic);
        UpdateResponseType updateResponseType = new UpdateResponseType();
        updateResponseType.setDiagnostics(diagnosticsType);
        updateResponseType.setRecordIdentifier(ocn);

        when(wciruServiceConnector.replaceRecord(any(Element.class), anyString(), anyString(), anyString())).thenReturn(updateResponseType);
        when(wciruServiceConnector.deleteRecord(any(Element.class), anyString())).thenReturn(updateResponseType);

        ChunkItem result = wciruServiceBroker.push(chunkItemWithWorldCatAttributes, worldCatEntity);
        assertThat(result.getData(), not(record));
    }

    @Test
    public void push_addOrUpdateRecord_ok() throws WciruServiceConnectorException, IOException, JSONBException {

        WorldCatAttributes worldCatAttributes = new WorldCatAttributes()
                .withPid("testPid")
                .withOcn("testOcn")
                .withCompareRecord("testRecord")
                .withHoldings(Arrays.asList(
                        new Holding().withSymbol("ABCDE").withAction(Holding.Action.INSERT),
                        new Holding().withSymbol("FGHIJ").withAction(Holding.Action.INSERT)
                ));

        ChunkItem chunkItem = new ChunkItemBuilder().setData("2\n{}\n7\n" + record + "\n").build();
        ChunkItemWithWorldCatAttributes chunkItemWithWorldCatAttributes = ChunkItemWithWorldCatAttributes.of(chunkItem).get(0).withWorldCatAttributes(worldCatAttributes);

        final WciruServiceBroker wciruServiceBroker = new WciruServiceBroker(wciruServiceConnector);
        final DiagnosticsType diagnosticsType = new DiagnosticsType();
        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setMessage("SUCCESS");
        diagnosticsType.diagnostic = Collections.singletonList(diagnostic);
        UpdateResponseType updateResponseType = new UpdateResponseType();
        updateResponseType.setDiagnostics(diagnosticsType);
        updateResponseType.setRecordIdentifier(ocn);

        when(wciruServiceConnector.replaceRecord(any(Element.class), anyString(), anyString(), anyString())).thenReturn(updateResponseType);
        when(wciruServiceConnector.addOrUpdateRecord(any(Element.class), anyString(), anyString())).thenReturn(updateResponseType);

        ChunkItem result = wciruServiceBroker.push(chunkItemWithWorldCatAttributes, worldCatEntity);
        assertThat(result.getData(), not(record));
    }
}