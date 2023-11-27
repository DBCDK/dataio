package dk.dbc.dataio.sink.worldcat;

import dk.dbc.oclc.wciru.Diagnostic;
import dk.dbc.oclc.wciru.DiagnosticsType;
import dk.dbc.oclc.wciru.OperationStatusType;
import dk.dbc.oclc.wciru.UpdateResponseType;
import dk.dbc.oclc.wciru.WciruServiceConnector;
import dk.dbc.oclc.wciru.WciruServiceConnectorException;
import dk.dbc.ocnrepo.dto.WorldCatEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WciruServiceBrokerTest {
    private WciruServiceConnector wciruServiceConnector;
    private WciruServiceBroker wciruServiceBroker;

    @BeforeEach
    public void createWciruServiceBroker() {
        wciruServiceConnector = mock(WciruServiceConnector.class);
        wciruServiceBroker = new WciruServiceBroker(wciruServiceConnector);
    }

    @Test
    public void suppressedErrorsDoNotCauseBrokerFailure() throws WciruServiceConnectorException {
        // Setup broker arguments

        List<Holding> holdings = Arrays.asList(
                new Holding()
                        .withSymbol(WciruServiceBroker.PRIMARY_HOLDING_SYMBOL)
                        .withAction(Holding.Action.DELETE),
                new Holding()
                        .withSymbol("DKB")
                        .withAction(Holding.Action.DELETE));

        ChunkItemWithWorldCatAttributes chunkItem =
                (ChunkItemWithWorldCatAttributes) new ChunkItemWithWorldCatAttributes()
                        .withWorldCatAttributes(new WorldCatAttributes()
                                .withHoldings(holdings))
                        .withData("<record/>");

        WorldCatEntity worldCatEntity = new WorldCatEntity().withOcn("xyz");

        // Setup mocked service responses:
        //      1st: Successful response for replaceRecord call for the primary
        //           holding symbol.
        //      2nd: Failed response with a suppressed diagnostic for
        //           replaceRecord call for an unknown holding symbol.
        //      3rd: Successful response for deleteRecord call.

        UpdateResponseType successResponse = new UpdateResponseType();
        successResponse.setOperationStatus(OperationStatusType.SUCCESS);
        successResponse.setRecordIdentifier(worldCatEntity.getOcn());

        when(wciruServiceConnector.replaceRecord(
                any(Element.class),
                eq(worldCatEntity.getOcn()),
                eq(holdings.get(0).getSymbol()),
                eq(holdings.get(0).getAction().getWciruValue())))
                .thenReturn(successResponse);

        DiagnosticsType diagnostics = new DiagnosticsType();
        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setUri("uri: info:srw/diagnostic/12/13");
        diagnostic.setMessage("Invalid data structure: component rejected");
        diagnostic.setDetails("SRU_RemoveLSN_Failures_No_LSN_Found. The PPN [800010-katalog:99122974111405763] was not found in the database record.:Unspecified error(100)");
        diagnostics.getDiagnostic().add(diagnostic);

        UpdateResponseType failResponse = new UpdateResponseType();
        failResponse.setOperationStatus(OperationStatusType.FAIL);
        failResponse.setDiagnostics(diagnostics);

        when(wciruServiceConnector.replaceRecord(
                any(Element.class),
                eq(worldCatEntity.getOcn()),
                eq(holdings.get(1).getSymbol()),
                eq(holdings.get(1).getAction().getWciruValue())))
                .thenReturn(failResponse);

        when(wciruServiceConnector.deleteRecord(
                any(Element.class),
                eq(worldCatEntity.getOcn())))
                .thenReturn(successResponse);

        // Call broker

        WciruServiceBroker.Result brokerResult = wciruServiceBroker.push(chunkItem, worldCatEntity);
        assertThat(brokerResult.isFailed(), is(false));
    }
}
