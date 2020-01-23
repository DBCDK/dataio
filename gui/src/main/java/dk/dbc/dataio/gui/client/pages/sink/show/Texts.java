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

package dk.dbc.dataio.gui.client.pages.sink.show;

import com.google.gwt.i18n.client.Constants;
import dk.dbc.dataio.gui.client.i18n.MainConstants;


public interface Texts extends Constants {
    // Buttons
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_Edit();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_Create();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_Refresh();


    // Labels
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_Sinks();


    // Column Headers
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_Type();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_Name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_ResourceName();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_Description();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_Action();


    // Sink Type Selection
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_DpfSink();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_ESSink();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_UpdateSink();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_DummySink();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_ImsSink();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_HiveSink();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_TickleSink();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_WorldCatSink();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_MarcConvSink();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_PeriodicJobsSink();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_VipSink();
}
