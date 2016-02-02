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

package dk.dbc.dataio.gui.client.pages.harvester.show;

import com.google.gwt.i18n.client.Constants;
import dk.dbc.dataio.gui.client.i18n.MainConstants;


public interface Texts extends Constants {

    // Column Headers
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_Name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_Resource();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_Target();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_Id();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_Size();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_FormatOverrides();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_Relations();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_Destination();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_Format();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_Type();


    // Help texts
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_Name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_Resource();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_Target();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_Id();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_Size();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_FormatOverrides();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_Relations();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_Destination();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_Format();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_Type();


    // Include Relation values
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String includeRelationsTrue();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String includeRelationsFalse();
}
