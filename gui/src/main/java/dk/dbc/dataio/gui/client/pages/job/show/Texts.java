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

package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.i18n.client.Constants;
import dk.dbc.dataio.gui.client.i18n.MainConstants;

public interface Texts extends Constants {

    // Labels
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_Jobs();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_JobId();

    // Column Headers
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_JobId();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_SubmitterNumber();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_SubmitterName();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_FlowBinderName();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_SinkName();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_TotalChunkCount();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_JobCreationTime();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_FailureCounter();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_IgnoredCounter();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_ProgressBar();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_JobStatus();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_RerunJob();

    // Button texts
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_AllJobs();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_ProcessingFailedJobs();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_DeliveringFailedJobs();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_FatalJobs();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_Refresh();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_ShowJob();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_RerunJob();

    // Error texts
    @Constants.DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_InputFieldValidationError();

    @Constants.DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_NumericInputFieldValidationError();

    @Constants.DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_JobNotFound();
}
