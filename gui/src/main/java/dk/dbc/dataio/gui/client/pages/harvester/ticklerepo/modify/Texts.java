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

package dk.dbc.dataio.gui.client.pages.harvester.ticklerepo.modify;

import dk.dbc.dataio.gui.client.i18n.MainConstants;


public interface Texts extends com.google.gwt.i18n.client.Constants {
    // Labels
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_AreYouSureAboutDeleting();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_Record();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_Records();


    // Captions
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String caption_DeleteHarvester();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String caption_RecordHarvestTitle();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String caption_DeleteOutdatedRecords();

    // Prompts
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_Id();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_Name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_Description();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_Destination();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_Format();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_Type();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_Enabled();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_NotificationsEnabled();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_DeleteOutdatedRecordsFromDate();


    // Buttons
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_TaskRecordHarvest();
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_Save();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_Delete();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_Yes();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_Cancel();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_DeleteOutdatedRecords();


    // Status messages
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String status_ConfigSuccessfullySaved();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String status_TickleRepoHarvesterSuccessfullyDeleted();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String status_HarvestTaskCreated();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String status_Busy();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String status_DeleteOutdatedRecords();


    // Error messages
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_InputFieldValidationError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_HarvesterNotFound();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_DeleteOutdatedRecordsFromDateValidationError();


    // DialogBox
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String dialog_numberOfRecords();
}
