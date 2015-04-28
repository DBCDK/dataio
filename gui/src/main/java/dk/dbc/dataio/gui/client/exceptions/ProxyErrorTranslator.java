package dk.dbc.dataio.gui.client.exceptions;

import dk.dbc.dataio.gui.client.exceptions.texts.ProxyErrorTexts;

import javax.ws.rs.core.Response;

public abstract class ProxyErrorTranslator {

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

    public static String toClientErrorFromFlowStoreProxy(Throwable e, ProxyErrorTexts text) {
        final String errorMessage;
        ProxyError errorCode = null;

        if (e instanceof ProxyException) {
            errorCode = ((ProxyException) e).getErrorCode();
        }
        if (errorCode == null) {
            errorMessage = e.getMessage();
        } else {
            switch (errorCode) {
                case ENTITY_NOT_FOUND: errorMessage = text.flowStoreProxy_notFoundError();
                    break;
                case CONFLICT_ERROR: errorMessage = text.flowStoreProxy_conflictError();
                    break;
                case NOT_ACCEPTABLE: errorMessage = text.flowStoreProxy_keyViolationError();
                    break;
                case BAD_REQUEST: errorMessage = text.flowStoreProxy_dataValidationError();
                    break;
                case PRECONDITION_FAILED: errorMessage = text.flowStoreProxy_preconditionFailedError();
                    break;
                case MODEL_MAPPER_INVALID_FIELD_VALUE: errorMessage = text.flowStoreProxy_modelMapperInvalidFieldValue();
                    break;
                case INTERNAL_SERVER_ERROR: errorMessage = text.flowStoreProxy_generalServerError();
                    break;
                default:
                    errorMessage = e.getMessage();
            }
        }
        return errorMessage;
    }

    public static String toClientErrorFromLogStoreProxy(Throwable e, ProxyErrorTexts texts) {
        final String errorMessage;
        ProxyError errorCode = null;

        if (e instanceof ProxyException) {
            errorCode = ((ProxyException) e).getErrorCode();
        }
        if (errorCode == null) {
            errorMessage = e.getMessage();
        } else {
            switch (errorCode) {
                case ENTITY_NOT_FOUND:
                    errorMessage = texts.logStoreProxy_notFoundError();
                    break;
                case INTERNAL_SERVER_ERROR:
                    errorMessage = texts.logStoreProxy_generalServerError();
                    break;
                default:
                    errorMessage = e.getMessage();
            }
        }
        return errorMessage;
    }


}
