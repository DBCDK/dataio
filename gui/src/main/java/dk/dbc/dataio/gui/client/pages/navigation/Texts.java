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

package dk.dbc.dataio.gui.client.pages.navigation;

import com.google.gwt.i18n.client.Constants;
import dk.dbc.dataio.gui.client.i18n.MainConstants;


public interface Texts extends Constants {

    // Menu texts
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_FlowCreation();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_FlowEdit();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_Flows();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_FlowBinderCreation();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_FlowBinderEdit();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_FlowBinders();

    @Constants.DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_FlowComponentCreation();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_FlowComponentEdit();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_FlowComponents();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_Jobs();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_TestJobs();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_AcctestJobs();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_SinkCreation();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_SinkEdit();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_Sinks();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_SinkStatus();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_SubmitterCreation();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_SubmitterEdit();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_Submitters();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_JobEdit();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_Items();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_HarvesterEdit();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_Harvesters();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_TickleHarvesters();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_RrHarvesters();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_UshHarvesters();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_CoRepoHarvesters();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_HoldingsItemHarvesters();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_HttpFtpFetchHarvesters();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_InfomediaHarvesters();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_Gatekeeper();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_IoTraffic();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_Ftp();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_FailedFtps();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_BaseMaintenance();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_HarvesterCreation();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_JobPurge();


    // Main Panel texts
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String text_DebugVersion();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String text_SvnNumber();


    // Error texts
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_SystemPropertyCouldNotBeRead();

}
