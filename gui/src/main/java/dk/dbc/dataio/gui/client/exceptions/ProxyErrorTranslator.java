/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

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
                case SERVICE_NOT_FOUND: stringBuilder.append(text.flowStoreProxy_serviceError());
                    break;
                case BAD_REQUEST: stringBuilder.append(text.flowStoreProxy_dataValidationError());
                    break;
                case NOT_ACCEPTABLE: stringBuilder.append(text.flowStoreProxy_keyViolationError());
                    break;
                case ENTITY_NOT_FOUND: stringBuilder.append(text.flowStoreProxy_notFoundError());
                    break;
                case CONFLICT_ERROR: stringBuilder.append(text.flowStoreProxy_conflictError());
                    break;
                case INTERNAL_SERVER_ERROR: stringBuilder.append(text.flowStoreProxy_generalServerError());
                    break;
                case MODEL_MAPPER_INVALID_FIELD_VALUE: stringBuilder.append(text.flowStoreProxy_modelMapperInvalidFieldValue());
                    break;
                case PRECONDITION_FAILED: stringBuilder.append(text.flowStoreProxy_preconditionFailedError());
                    break;
                case SUBVERSION_LOOKUP_FAILED: stringBuilder.append(text.flowStoreProxy_subversionLookupFailedError());
                    break;
                case ERROR_UNKNOWN: stringBuilder.append(text.flowStoreProxy_errorUnknownError());
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


    public static String toClientErrorFromJobStoreProxy(Throwable e, ProxyErrorTexts text, String clientMessage) {
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
                case SERVICE_NOT_FOUND: stringBuilder.append(text.jobStoreProxy_serviceError());
                    break;
                case BAD_REQUEST: stringBuilder.append(text.jobStoreProxy_dataValidationError());
                    break;
                case NOT_ACCEPTABLE: stringBuilder.append(text.jobStoreProxy_keyViolationError());
                    break;
                case ENTITY_NOT_FOUND: stringBuilder.append(text.jobStoreProxy_notFoundError());
                    break;
                case CONFLICT_ERROR: stringBuilder.append(text.jobStoreProxy_conflictError());
                    break;
                case INTERNAL_SERVER_ERROR: stringBuilder.append(text.jobStoreProxy_generalServerError());
                    break;
                case MODEL_MAPPER_INVALID_FIELD_VALUE: stringBuilder.append(text.jobStoreProxy_modelMapperInvalidFieldValue());
                    break;
                case PRECONDITION_FAILED: stringBuilder.append(text.jobStoreProxy_preconditionFailedError());
                    break;
                case FORBIDDEN_SINK_TYPE_TICKLE: stringBuilder.append(text.jobStoreProxy_invalidSinkTypeError());
                    break;
                case ERROR_UNKNOWN: stringBuilder.append(text.jobStoreProxy_errorUnknownError());
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

    public static String toClientErrorFromTickleHarvesterProxy(Throwable e, ProxyErrorTexts text, String clientMessage) {
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
                case SERVICE_NOT_FOUND: stringBuilder.append(text.tickleHarvesterProxy_serviceError());
                    break;
                case BAD_REQUEST: stringBuilder.append(text.tickleHarvesterProxy_dataValidationError());
                    break;
                case ERROR_UNKNOWN: stringBuilder.append(text.tickleHarvesterProxy_errorUnknownError());
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

    public static String toClientErrorFromFtpProxy(Throwable e, ProxyErrorTexts text, String clientMessage) {
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
                case NAMING_ERROR: stringBuilder.append(text.ftpProxy_namingError());
                    break;
                case FTP_CONNECTION_ERROR: stringBuilder.append(text.ftpProxy_ftpConnectionError());
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
