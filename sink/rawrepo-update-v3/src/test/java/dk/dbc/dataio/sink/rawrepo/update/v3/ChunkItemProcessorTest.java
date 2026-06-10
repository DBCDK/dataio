package dk.dbc.dataio.sink.rawrepo.update.v3;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;
import dk.dbc.dataio.sink.rawrepo.update.v3.connector.UpdateRequest;
import dk.dbc.dataio.sink.rawrepo.update.v3.connector.UpdateResponse;
import dk.dbc.dataio.sink.rawrepo.update.v3.connector.UpdateResponseStatus;
import dk.dbc.dataio.sink.rawrepo.update.v3.connector.UpdateServiceConnector;
import dk.dbc.dataio.sink.rawrepo.update.v3.connector.UpdateServiceConnectorException;
import dk.dbc.dataio.sink.rawrepo.update.v3.connector.ValidationMessage;
import dk.dbc.dataio.sink.rawrepo.update.v3.connector.ValidationStatus;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChunkItemProcessorTest {
    private final UpdateServiceConnector connector = mock(UpdateServiceConnector.class);
    private final OpenUpdateSinkConfig config = new OpenUpdateSinkConfig()
            .withEndpoint("http://update-service")
            .withUserId("user")
            .withPassword("secret");

    // Builds ChunkItem data as raw JSON strings (matching real job-processor output).
    // submitter is WRITE_ONLY on UpdateRequest so it must be in the JSON source, not
    // constructed via setSubmitter() before serialisation — WRITE_ONLY excludes it on write.
    private ChunkItem chunkItemWithRequests(String... jsonElements) {
        String json = "[" + String.join(",", jsonElements) + "]";
        return ChunkItem.successfulChunkItem()
                .withId(0)
                .withData(json.getBytes(StandardCharsets.UTF_8))
                .withType(ChunkItem.Type.JSON)
                .withEncoding(StandardCharsets.UTF_8);
    }

    private String req(String type) {
        String typeField = type != null ? "\"type\":\"" + type + "\"," : "";
        return "{" + typeField + "\"submitter\":\"870970\",\"templateName\":\"bog\",\"content\":{}}";
    }

    private String req() {
        return req("dbc");
    }

    private UpdateResponse okResponse() {
        UpdateResponse r = new UpdateResponse();
        r.setStatus(UpdateResponseStatus.OK);
        return r;
    }

    private UpdateResponse errorResponse(String message) {
        ValidationMessage vm = new ValidationMessage();
        vm.setType(ValidationStatus.ERROR);
        vm.setMessage(message);
        UpdateResponse r = new UpdateResponse();
        r.setStatus(UpdateResponseStatus.ERROR);
        r.setErrors(List.of(vm));
        return r;
    }

    @Test
    void process_singleRequestWithoutType_usesDefaultType() throws Exception {
        when(connector.update(any())).thenReturn(okResponse());

        // no "type" key → UpdateRequest.type stays at its default "dbc"
        ChunkItem result = new ChunkItemProcessor(connector, config, false)
                .process(chunkItemWithRequests("{\"submitter\":\"870970\",\"templateName\":\"bog\",\"content\":{}}"));

        verify(connector).update(any(UpdateRequest.class));
        assertThat(result.getStatus(), is(ChunkItem.Status.SUCCESS));
    }

    @Test
    void process_singleRequestWithType_callsConnectorWithThatType() throws Exception {
        when(connector.update(any())).thenReturn(okResponse());

        new ChunkItemProcessor(connector, config, false)
                .process(chunkItemWithRequests(req("dbc")));

        verify(connector).update(any(UpdateRequest.class));
    }

    @Test
    void process_authenticationComesFromConfigAndSubmitter() throws Exception {
        UpdateRequest[] captured = new UpdateRequest[1];
        when(connector.update(any())).thenAnswer(inv -> {
            captured[0] = inv.getArgument(0);
            return okResponse();
        });

        new ChunkItemProcessor(connector, config, false)
                .process(chunkItemWithRequests(req()));

        assertThat(captured[0].getAuthentication(), notNullValue());
        assertThat(captured[0].getAuthentication().getUserId(), is("user"));
        assertThat(captured[0].getAuthentication().getGroupId(), is("870970"));
        assertThat(captured[0].getAuthentication().getPassword(), is("secret"));
    }

    @Test
    void process_validateOnly_callsValidate() throws Exception {
        when(connector.validate(any())).thenReturn(okResponse());

        new ChunkItemProcessor(connector, config, true)
                .process(chunkItemWithRequests(req()));

        verify(connector).validate(any(UpdateRequest.class));
    }

    @Test
    void process_okResponse_returnsSuccessfulChunkItem() throws Exception {
        when(connector.update(any())).thenReturn(okResponse());

        ChunkItem result = new ChunkItemProcessor(connector, config, false)
                .process(chunkItemWithRequests(req()));

        assertThat(result.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat(result.getDiagnostics() == null || result.getDiagnostics().isEmpty(), is(true));
    }

    @Test
    void process_errorResponse_returnsFailedChunkItemWithDiagnostics() throws Exception {
        when(connector.update(any())).thenReturn(errorResponse("Felt 245 delfelt a mangler"));

        ChunkItem result = new ChunkItemProcessor(connector, config, false)
                .process(chunkItemWithRequests(req()));

        assertThat(result.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(result.getDiagnostics().size(), is(1));
        assertThat(result.getDiagnostics().get(0).getMessage(), is("Felt 245 delfelt a mangler"));
        assertThat(new String(result.getData(), StandardCharsets.UTF_8).contains("e01 00"), is(true));
    }

    @Test
    void process_multipleRequests_allAttempted() throws Exception {
        when(connector.update(any())).thenReturn(okResponse());

        ChunkItem result = new ChunkItemProcessor(connector, config, false)
                .process(chunkItemWithRequests(req(), req("dbc")));

        verify(connector, times(2)).update(any());
        assertThat(result.getStatus(), is(ChunkItem.Status.SUCCESS));
    }

    @Test
    void process_multipleRequests_firstOkSecondError_diagnosticsForErrorOnly() throws Exception {
        when(connector.update(any()))
                .thenReturn(okResponse())
                .thenReturn(errorResponse("error in second"));

        ChunkItem result = new ChunkItemProcessor(connector, config, false)
                .process(chunkItemWithRequests(req(), req()));

        assertThat(result.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(result.getDiagnostics().size(), is(1));
        String data = new String(result.getData(), StandardCharsets.UTF_8);
        assertThat(data.contains("-> OK"), is(true));
        assertThat(data.contains("e01 00"), is(true));
    }

    @Test
    void process_nonFatalDeleteResponse_returnsSuccessfulChunkItem() throws Exception {
        UpdateResponse response = new UpdateResponse();
        response.setStatus(UpdateResponseStatus.ERROR);
        ValidationMessage vm = new ValidationMessage();
        vm.setType(ValidationStatus.ERROR);
        vm.setMessage(ValidationMessageInterpreter.DELETE_NONEXISTENT_RECORD_MESSAGE);
        response.setErrors(List.of(vm));
        when(connector.update(any())).thenReturn(response);

        ChunkItem result = new ChunkItemProcessor(connector, config, false)
                .process(chunkItemWithRequests(req()));

        assertThat(result.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat(result.getDiagnostics() == null || result.getDiagnostics().isEmpty(), is(true));
    }

    @Test
    void process_connectorException_accumulatedAsFatalDiagnostic() throws Exception {
        when(connector.update(any())).thenThrow(new UpdateServiceConnectorException("auth failed"));

        ChunkItem result = new ChunkItemProcessor(connector, config, false)
                .process(chunkItemWithRequests(req()));

        assertThat(result.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(result.getDiagnostics().size(), is(1));
        assertThat(result.getDiagnostics().get(0).getLevel(), is(Diagnostic.Level.FATAL));
    }

    @Test
    void process_malformedJson_returnsFailedChunkItem() {
        ChunkItem item = ChunkItem.successfulChunkItem()
                .withId(0)
                .withData("not json at all".getBytes(StandardCharsets.UTF_8))
                .withType(ChunkItem.Type.STRING)
                .withEncoding(StandardCharsets.UTF_8);

        ChunkItem result = new ChunkItemProcessor(connector, config, false).process(item);

        assertThat(result.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(result.getDiagnostics().get(0).getLevel(), is(Diagnostic.Level.FATAL));
    }
}
