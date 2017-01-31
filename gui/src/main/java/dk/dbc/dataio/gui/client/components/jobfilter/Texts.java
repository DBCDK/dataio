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

package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.i18n.client.Constants;
import dk.dbc.dataio.gui.client.i18n.MainConstants;

public interface Texts extends Constants{

    // Header text
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String text_Filter();


    // Other texts
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String text_AddFilter();


    /*
     * Specific Job Filter texts
     */

    // Sink Filter
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String sinkFilter_name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String sinkFilter_PromptText();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String sinkFilter_ChooseASinkName();


    // Submitter Filter
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String submitterFilter_name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String submitterFilter_PromptText();


    // Suppress Submitter Filter
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String suppressSubmitterFilter_name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String suppressSubmitterFilter_ShowAllSubmitterJobs();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String suppressSubmitterFilter_SuppressSubmitterJobs();


    // Date Filter
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String jobDateFilter_name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String jobDateFilter_From();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String jobDateFilter_To();


    // Error Filter
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String errorFilter_name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String errorFilter_FailedIn();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String errorFilter_Processing();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String errorFilter_Delivering();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String errorFilter_JobCreation();


    // Active Jobs Filter
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String activeJobsFilter_name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String activeJobsFilter_Prompt();


    // Item Filter
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String itemFilter_name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String itemFilter_PromptText();
}
