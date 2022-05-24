package dk.dbc.dataio.sink.worldcat;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Pid;
import dk.dbc.dataio.commons.utils.lang.ResourceReader;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.oclc.wciru.Diagnostic;
import dk.dbc.oclc.wciru.WciruServiceConnector;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class FormattedOutputTest {
    private final WciruServiceConnector wciruServiceConnector = mock(WciruServiceConnector.class);
    private final Diagnostic warning = getWarning();
    private final Diagnostic error = getError();
    private final Exception exception = new IllegalStateException("test");

    private final Pid pid = Pid.of("123456-test:local");
    private final WciruServiceBroker.Result brokerResult = getBrokerResult();

    @Test
    public void fromWciruServiceBrokerResult() {
        final ChunkItem chunkItem = FormattedOutput.of(pid, brokerResult);
        assertThat("chunk item status", chunkItem.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("chunk item type", chunkItem.getType(), is(Collections.singletonList(ChunkItem.Type.STRING)));
        assertThat("chunk item encoding", chunkItem.getEncoding(), is(StandardCharsets.UTF_8));
        assertThat("chunk item data", new String(chunkItem.getData(), chunkItem.getEncoding()),
                is(ResourceReader.getResourceAsString(this.getClass(), "formatted_output.txt")));
    }

    @Test
    public void fromWciruServiceBrokerResultwithException() {
        final WciruServiceBroker.Result brokerResult = getBrokerResult().withException(exception);
        final ChunkItem chunkItem = FormattedOutput.of(pid, brokerResult);
        assertThat("chunk item status", chunkItem.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("chunk item type", chunkItem.getType(), is(Collections.singletonList(ChunkItem.Type.STRING)));
        assertThat("chunk item encoding", chunkItem.getEncoding(), is(StandardCharsets.UTF_8));
        assertThat("chunk item data", new String(chunkItem.getData(), chunkItem.getEncoding()),
                is(ResourceReader.getResourceAsString(this.getClass(), "formatted_output.txt") +
                        "\n\n" + StringUtil.getStackTraceString(exception, "")));
        final dk.dbc.dataio.commons.types.Diagnostic diagnostic = chunkItem.getDiagnostics().get(0);
        assertThat("diagnostic level", diagnostic.getLevel(),
                is(dk.dbc.dataio.commons.types.Diagnostic.Level.FATAL));
        assertThat("diagnostic message", diagnostic.getMessage(), is(exception.getMessage()));
    }

    @Test
    public void fromException() {
        final ChunkItem chunkItem = FormattedOutput.of(exception);
        assertThat("chunk item status", chunkItem.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("chunk item type", chunkItem.getType(), is(Collections.singletonList(ChunkItem.Type.STRING)));
        assertThat("chunk item encoding", chunkItem.getEncoding(), is(StandardCharsets.UTF_8));
        assertThat("chunk item data", new String(chunkItem.getData(), chunkItem.getEncoding()),
                is(exception.getMessage()));
        final dk.dbc.dataio.commons.types.Diagnostic diagnostic = chunkItem.getDiagnostics().get(0);
        assertThat("diagnostic level", diagnostic.getLevel(),
                is(dk.dbc.dataio.commons.types.Diagnostic.Level.FATAL));
        assertThat("diagnostic message", diagnostic.getMessage(), is(exception.getMessage()));
    }

    private WciruServiceBroker.Result getBrokerResult() {
        return new WciruServiceBroker(wciruServiceConnector).new Result()
                .withOcn("789")
                .withEvents(new WciruServiceBroker.Event()
                    .withAction(WciruServiceBroker.Event.Action.ADD_OR_UPDATE)
                    .withHolding(new Holding()
                        .withSymbol(WciruServiceBroker.PRIMARY_HOLDING_SYMBOL)
                        .withAction(Holding.Action.INSERT))
                    .withDiagnostics(warning, error))
                .withEvents(new WciruServiceBroker.Event()
                    .withAction(WciruServiceBroker.Event.Action.REPLACE)
                    .withHolding(new Holding()
                        .withSymbol("ABC")
                        .withAction(Holding.Action.DELETE)))
                .withEvents(new WciruServiceBroker.Event()
                    .withAction(WciruServiceBroker.Event.Action.DELETE)
                    .withDiagnostics(warning));
    }

    private Diagnostic getWarning() {
        final Diagnostic warning = new Diagnostic();
        warning.setMessage("a warning");
        warning.setDetails("details about the warning");
        warning.setUri("uri:warning");
        return warning;
    }

    private Diagnostic getError() {
        final Diagnostic error = new Diagnostic();
        error.setMessage("an error");
        error.setDetails("details about the error");
        error.setUri("uri:error");
        return error;
    }
}
