package dk.dbc.dataio.gui.client.exceptions;

import dk.dbc.dataio.jobstore.types.JobError;

import javax.ws.rs.core.Response;

public class StatusCodeTranslator {

    public static ProxyError toProxyError(int statusCode) throws ProxyException {
        final Response.Status status = Response.Status.fromStatusCode(statusCode);

        final ProxyError errorCode;
        switch (status) {
            case NOT_FOUND:
                errorCode = ProxyError.ENTITY_NOT_FOUND;
                break;
            case CONFLICT:
                errorCode = ProxyError.CONFLICT_ERROR;
                break;
            case NOT_ACCEPTABLE:
                errorCode = ProxyError.NOT_ACCEPTABLE;
                break;
            case PRECONDITION_FAILED:
                errorCode = ProxyError.PRECONDITION_FAILED;
                break;
            case BAD_REQUEST:
                errorCode = ProxyError.BAD_REQUEST;
                break;
            default:
                errorCode = ProxyError.INTERNAL_SERVER_ERROR;
        }
        return errorCode;
    }

    public static ProxyError toProxyError(JobError.Code statusCode) throws ProxyException {
        // TODO: 31/05/2017 all JobError code's should be defined here once they are in use by the frontend
        final ProxyError errorCode;
        switch (statusCode) {
            case FORBIDDEN_SINK_TYPE_TICKLE:
                errorCode = ProxyError.FORBIDDEN_SINK_TYPE_TICKLE;
                break;
            default:
                errorCode = ProxyError.ERROR_UNKNOWN;
        }
        return errorCode;
    }
}
