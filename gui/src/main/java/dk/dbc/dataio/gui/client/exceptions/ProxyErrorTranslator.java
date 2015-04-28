package dk.dbc.dataio.gui.client.exceptions;

import dk.dbc.dataio.gui.client.exceptions.texts.ProxyErrorTexts;

public abstract class ProxyErrorTranslator {

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
