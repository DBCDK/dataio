package dk.dbc.dataio.sink.worldcat;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Pid;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.oclc.wciru.Diagnostic;
import dk.dbc.oclc.wciru.WciruServiceConnectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class FormattedOutput {
    public static ChunkItem of(Pid pid, WciruServiceBroker.Result brokerResult) {
        return new WciruServiceBrokerResultFormatter(pid, brokerResult).toChunkItem();
    }

    public static ChunkItem of(Exception exception) {
        return new ExceptionFormatter(exception).toChunkItem();
    }

    private static class WciruServiceBrokerResultFormatter {
        private static final Logger LOGGER = LoggerFactory.getLogger(WciruServiceBrokerResultFormatter.class);

        private final Pid pid;
        private final WciruServiceBroker.Result brokerResult;
        private final StringBuilder buffer = new StringBuilder();

        WciruServiceBrokerResultFormatter(Pid pid, WciruServiceBroker.Result brokerResult) {
            this.pid = pid;
            this.brokerResult = brokerResult;
        }

        ChunkItem toChunkItem() {
            formatBrokerResult();

            final ChunkItem chunkItem;
            if (brokerResult.isFailed()) {
                chunkItem = ChunkItem.failedChunkItem()
                        .withDiagnostics(new dk.dbc.dataio.commons.types.Diagnostic(
                                dk.dbc.dataio.commons.types.Diagnostic.Level.FATAL,
                                brokerResult.getException().getMessage(),
                                brokerResult.getException()));
            } else {
                chunkItem = ChunkItem.successfulChunkItem();
            }
            return chunkItem
                    .withData(buffer.toString())
                    .withType(ChunkItem.Type.STRING)
                    .withEncoding(StandardCharsets.UTF_8);
        }

        private void formatBrokerResult() {
            buffer.append("PID: ").append(pid.toString())
                    .append(" OCN: ").append(brokerResult.getOcn());
            for (WciruServiceBroker.Event brokerEvent : brokerResult.getEvents()) {
                final String formattedBrokerEvent = formatBrokerEvent(brokerEvent);
                buffer.append("\n\n>> ").append(formattedBrokerEvent);
                LOGGER.info("{} {}", pid, formattedBrokerEvent);
            }
            if (brokerResult.isFailed()) {
                buffer.append("\n\n").append(StringUtil.getStackTraceString(brokerResult.getException(), ""));
            }
        }

        private String formatBrokerEvent(WciruServiceBroker.Event brokerEvent) {
            final StringBuilder buffer = new StringBuilder()
                    .append(brokerEvent.getAction());
            if (brokerEvent.getHolding() != null) {
                buffer.append(" ").append(brokerEvent.getHolding().toString());
            }
            for (Diagnostic diagnostic : brokerEvent.getDiagnostics()) {
                buffer.append("\n\n").append(WciruServiceConnectorException.toString(diagnostic));
            }
            return buffer.toString();
        }
    }

    private static class ExceptionFormatter {
        private final Exception exception;

        ExceptionFormatter(Exception exception) {
            this.exception = exception;
        }

        ChunkItem toChunkItem() {
            return ChunkItem.failedChunkItem()
                    .withDiagnostics(new dk.dbc.dataio.commons.types.Diagnostic(
                            dk.dbc.dataio.commons.types.Diagnostic.Level.FATAL,
                            exception.getMessage(),
                            exception))
                    .withData(exception.getMessage())
                    .withType(ChunkItem.Type.STRING)
                    .withEncoding(StandardCharsets.UTF_8);
        }
    }
}
