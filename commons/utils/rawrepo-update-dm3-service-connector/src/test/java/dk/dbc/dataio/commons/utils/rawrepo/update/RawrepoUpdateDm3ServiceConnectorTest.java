package dk.dbc.dataio.commons.utils.rawrepo.update;

import dk.dbc.commons.useragent.UserAgent;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpPost;
import dk.dbc.updateservice.dto.UpdateRequest;
import dk.dbc.updateservice.dto.UpdateResponse;
import dk.dbc.updateservice.dto.UpdateResponseStatus;
import dk.dbc.updateservice.dto.ValidationMessage;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RawrepoUpdateDm3ServiceConnectorTest {
    private static final String BASE_URL = "http://rawrepo-update/";
    private final FailSafeHttpClient failSafeHttpClient = mock(FailSafeHttpClient.class);
    private final RawrepoUpdateDm3ServiceConnector connector =
            new RawrepoUpdateDm3ServiceConnector(failSafeHttpClient, BASE_URL);
    {
        when(failSafeHttpClient.getUserAgent()).thenReturn(new UserAgent(getClass().getName()));
    }

    @Test
    void updateRecord_requestIsNull_throws() {
        assertThat(() -> connector.updateRecord(null), isThrowing(NullPointerException.class));
    }

    @Test
    void updateRecord_unexpectedStatusCode_throws() {
        when(failSafeHttpClient.execute(any(HttpPost.class)))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), null));

        assertThat(() -> connector.updateRecord(new UpdateRequest()),
                isThrowing(RawrepoUpdateServiceConnectorException.class));
    }

    @Test
    void updateRecord_success_returnsOkResponse() throws RawrepoUpdateServiceConnectorException {
        final UpdateResponse okResponse = UpdateResponse.ok();
        when(failSafeHttpClient.execute(any(HttpPost.class)))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), okResponse));

        final UpdateResponse result = connector.updateRecord(new UpdateRequest());
        assertThat(result.getStatus(), is(UpdateResponseStatus.OK));
        assertThat(result.getErrors(), is(Collections.emptyList()));
    }

    @Test
    void updateRecord_validationError_returnsErrorResponse() throws RawrepoUpdateServiceConnectorException {
        final UpdateResponse errorResponse = UpdateResponse.fail(new ValidationMessage("felt mangler"));
        when(failSafeHttpClient.execute(any(HttpPost.class)))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), errorResponse));

        final UpdateResponse result = connector.updateRecord(new UpdateRequest());
        assertThat(result.getStatus(), is(UpdateResponseStatus.ERROR));
        assertThat(result.getErrors().size(), is(1));
        assertThat(result.getErrors().getFirst().getMessage(), is("felt mangler"));
    }

}
