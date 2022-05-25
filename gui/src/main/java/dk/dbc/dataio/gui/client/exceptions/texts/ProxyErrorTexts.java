package dk.dbc.dataio.gui.client.exceptions.texts;

import com.google.gwt.i18n.client.Constants;
import dk.dbc.dataio.gui.client.i18n.MainConstants;

public interface ProxyErrorTexts extends Constants {

    // Error messages file store
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String fileStoreProxy_serviceError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String fileStoreProxy_notFoundError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String fileStoreProxy_badRequest();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String fileStoreProxy_generalServerError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String fileStoreProxy_errorUnknownError();

    // Error messages flow store
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String flowStoreProxy_serviceError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String flowStoreProxy_dataValidationError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String flowStoreProxy_keyViolationError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String flowStoreProxy_notFoundError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String flowStoreProxy_conflictError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String flowStoreProxy_generalServerError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String flowStoreProxy_modelMapperInvalidFieldValue();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String flowStoreProxy_preconditionFailedError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String flowStoreProxy_subversionLookupFailedError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String flowStoreProxy_errorUnknownError();


    // Error messages tickle repo
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String tickleHarvesterProxy_serviceError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String tickleHarvesterProxy_dataValidationError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String tickleHarvesterProxy_errorUnknownError();


    // Error messages job store
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String jobStoreProxy_serviceError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String jobStoreProxy_dataValidationError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String jobStoreProxy_keyViolationError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String jobStoreProxy_notFoundError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String jobStoreProxy_conflictError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String jobStoreProxy_generalServerError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String jobStoreProxy_modelMapperInvalidFieldValue();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String jobStoreProxy_preconditionFailedError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String jobStoreProxy_subversionLookupFailedError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String jobStoreProxy_errorUnknownError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String jobStoreProxy_invalidSinkTypeError();


    // Error messages ftp proxy
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String ftpProxy_namingError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String ftpProxy_ftpConnectionError();
}

