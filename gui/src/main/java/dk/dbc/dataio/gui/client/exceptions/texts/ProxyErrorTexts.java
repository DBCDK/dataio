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

package dk.dbc.dataio.gui.client.exceptions.texts;

import com.google.gwt.i18n.client.Constants;
import dk.dbc.dataio.gui.client.i18n.MainConstants;

public interface ProxyErrorTexts extends Constants {

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

