package dk.dbc.dataio.sink.rawrepo.update.v3;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;
import dk.dbc.dataio.sink.rawrepo.update.v3.connector.Authentication;
import dk.dbc.dataio.sink.rawrepo.update.v3.connector.UpdateRequest;
import dk.dbc.dataio.sink.rawrepo.update.v3.connector.UpdateResponse;
import dk.dbc.dataio.sink.rawrepo.update.v3.connector.UpdateResponseStatus;
import dk.dbc.dataio.sink.rawrepo.update.v3.connector.UpdateServiceConnector;
import dk.dbc.dataio.sink.rawrepo.update.v3.connector.UpdateServiceConnectorException;
import dk.dbc.marc.binding.MarcBinding;
import dk.dbc.marc.reader.JsonReader;
import org.eclipse.microprofile.metrics.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChunkItemProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkItemProcessor.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final UpdateServiceConnector connector;
    private final OpenUpdateSinkConfig config;
    private final ValidationMessageInterpreter validationMessageInterpreter;

    public ChunkItemProcessor(UpdateServiceConnector connector, OpenUpdateSinkConfig config) {
        this.connector = connector;
        this.config = config;
        this.validationMessageInterpreter = new ValidationMessageInterpreter(config.getIgnoredValidationErrors());
    }

    public ChunkItem process(ChunkItem chunkItem) {
        List<UpdateRequest> records;
        try {
            records = OBJECT_MAPPER.readValue(chunkItem.getData(),
                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, UpdateRequest.class));
        } catch (Exception e) {
            String message = "Failed to parse update record list: " + e.getMessage();
            return ChunkItem.failedChunkItem()
                    .withId(chunkItem.getId())
                    .withType(ChunkItem.Type.STRING)
                    .withTrackingId(chunkItem.getTrackingId())
                    .withData(message)
                    .withDiagnostics(new Diagnostic(Diagnostic.Level.FATAL, message, e));
        }

        StringBuilder output = new StringBuilder();
        List<Diagnostic> diagnostics = new ArrayList<>();

        for (int i = 0; i < records.size(); i++) {
            UpdateRequest record = records.get(i);
            record.setTrackingId(chunkItem.getTrackingId());
            setAuthentication(record);
            callUpdateService(record, output, diagnostics, i, records.size());
        }

        ChunkItem result = ChunkItem.successfulChunkItem()
                .withId(chunkItem.getId())
                .withType(ChunkItem.Type.STRING)
                .withTrackingId(chunkItem.getTrackingId())
                .withData(output.toString());
        if (!diagnostics.isEmpty()) {
            result.appendDiagnostics(diagnostics);
        }
        return result;
    }

    private void setAuthentication(UpdateRequest record) {
        Authentication auth = new Authentication();
        auth.setUserId(config.getUserId());
        auth.setGroupId(config.getGroupId());
        auth.setPassword(config.getPassword());
        record.setAuthentication(auth);
    }

    private void callUpdateService(UpdateRequest record, StringBuilder output,
                                   List<Diagnostic> diagnostics, int index, int total) {
        try {
            long startTime = System.currentTimeMillis();
            UpdateResponse response = config.isValidateOnly()
                    ? connector.validate(record)
                    : connector.update(record);
            Metric.update_service_requests.timer(
                    new Tag("submitter", Objects.requireNonNullElse(record.getSubmitter(), "unknown")),
                    new Tag("template", record.getTemplateName()))
                    .update(Duration.ofMillis(System.currentTimeMillis() - startTime));

            MarcBinding marcBinding = response.getStatus() == UpdateResponseStatus.ERROR
                    ? parseMarcBinding(record.getContent())
                    : null;
            List<Diagnostic> itemDiagnostics = validationMessageInterpreter.getDiagnostics(response, marcBinding);

            if (itemDiagnostics.isEmpty()) {
                output.append("Record ").append(index + 1).append(" of ").append(total)
                        .append(" [trackingId=").append(record.getTrackingId()).append("]")
                        .append(" -> OK").append(System.lineSeparator());
            } else {
                diagnostics.addAll(itemDiagnostics);
                output.append("Record ").append(index + 1).append(" of ").append(total)
                        .append(" [trackingId=").append(record.getTrackingId()).append("]")
                        .append(" -> ERROR").append(System.lineSeparator());
                output.append(toE01Lines(itemDiagnostics));
            }
        } catch (UpdateServiceConnectorException e) {
            Diagnostic d = new Diagnostic(Diagnostic.Level.FATAL, e.getMessage(), e);
            diagnostics.add(d);
            output.append(e.getMessage()).append(System.lineSeparator());
        }
    }

    private MarcBinding parseMarcBinding(JsonNode content) {
        if (content == null) {
            return null;
        }
        try {
            byte[] bytes = OBJECT_MAPPER.writeValueAsBytes(content);
            try (JsonReader reader = new JsonReader(new ByteArrayInputStream(bytes))) {
                return reader.readBinding();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to parse MARC content for error field resolution", e);
            return null;
        }
    }

    private String toE01Lines(List<Diagnostic> diagnostics) {
        StringBuilder sb = new StringBuilder();
        for (Diagnostic d : diagnostics) {
            if (d.getStacktrace() != null) {
                sb.append(d.getMessage());
            } else {
                sb.append("e01 00 *a").append(d.getMessage());
                if (d.getTag() != null) {
                    sb.append("*b").append(d.getTag());
                    if (d.getAttribute() != null) {
                        sb.append("*c").append(d.getAttribute());
                    }
                }
            }
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }
}
