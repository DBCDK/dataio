package dk.dbc.dataio.gui.client.exceptions;

import javax.ws.rs.core.Response;

public abstract class StatusCodeTranslator {

    public static ProxyError toProxyError(int statusCode) throws ProxyException {
        final Response.Status status = Response.Status.fromStatusCode(statusCode);

        final ProxyError errorCode;
        switch (status){
            case NOT_FOUND: errorCode = ProxyError.ENTITY_NOT_FOUND;
                break;
            case CONFLICT: errorCode = ProxyError.CONFLICT_ERROR;
                break;
            case NOT_ACCEPTABLE: errorCode = ProxyError.NOT_ACCEPTABLE;
                break;
            case PRECONDITION_FAILED: errorCode = ProxyError.PRECONDITION_FAILED;
                break;
            case BAD_REQUEST: errorCode = ProxyError.BAD_REQUEST;
                break;
            default:
                errorCode = ProxyError.INTERNAL_SERVER_ERROR;
        }
        return errorCode;
    }
}
