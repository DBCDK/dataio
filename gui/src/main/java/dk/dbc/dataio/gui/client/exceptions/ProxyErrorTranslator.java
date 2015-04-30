package dk.dbc.dataio.gui.client.exceptions;

import dk.dbc.dataio.gui.client.exceptions.texts.ProxyErrorTexts;

public class ProxyErrorTranslator {

    public static String toClientErrorFromFlowStoreProxy(Throwable e, ProxyErrorTexts text, String clientMessage) {
        final String errorMessage;
        ProxyError errorCode = null;

        if (e instanceof ProxyException) {
            errorCode = ((ProxyException) e).getErrorCode();
        }
        if (errorCode == null) {
            errorMessage = e.getMessage();
        } else {
            final StringBuilder stringBuilder = new StringBuilder();
            switch (errorCode) {
                case ENTITY_NOT_FOUND: stringBuilder.append(text.flowStoreProxy_notFoundError());
                    break;
                case CONFLICT_ERROR: stringBuilder.append(text.flowStoreProxy_conflictError());
                    break;
                case NOT_ACCEPTABLE: stringBuilder.append(text.flowStoreProxy_keyViolationError());
                    break;
                case BAD_REQUEST: stringBuilder.append(text.flowStoreProxy_dataValidationError());
                    break;
                case PRECONDITION_FAILED: stringBuilder.append(text.flowStoreProxy_preconditionFailedError());
                    break;
                case SERVICE_NOT_FOUND: stringBuilder.append(text.flowStoreProxy_serviceError());
                    break;
                case MODEL_MAPPER_INVALID_FIELD_VALUE: stringBuilder.append(text.flowStoreProxy_modelMapperInvalidFieldValue());
                    break;
                case INTERNAL_SERVER_ERROR: stringBuilder.append(text.flowStoreProxy_generalServerError());
                    break;
                default:
                    stringBuilder.append(e.getMessage());
            }

            if(clientMessage != null) {
                stringBuilder.append(" {").append(clientMessage).append("}.");
            }
            errorMessage = stringBuilder.toString();
        }
        return errorMessage;
    }

}
