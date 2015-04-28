package dk.dbc.dataio.gui.client.exceptions.texts;

import com.google.gwt.i18n.client.Constants;
import dk.dbc.dataio.gui.client.i18n.MainConstants;

public interface ProxyErrorTexts extends Constants {

    // Error messages flow store
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String flowStoreProxy_keyViolationError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String flowStoreProxy_dataValidationError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String flowStoreProxy_preconditionFailedError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String flowStoreProxy_conflictError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String flowStoreProxy_notFoundError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String flowStoreProxy_generalServerError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String flowStoreProxy_modelMapperInvalidFieldValue();


    // Error messages log store
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String logStoreProxy_notFoundError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String logStoreProxy_generalServerError();
}

