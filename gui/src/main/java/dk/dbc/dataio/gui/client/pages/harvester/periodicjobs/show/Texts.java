package dk.dbc.dataio.gui.client.pages.harvester.periodicjobs.show;

import com.google.gwt.i18n.client.Constants;
import dk.dbc.dataio.gui.client.i18n.MainConstants;


public interface Texts extends Constants {
    // Column Headers
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_Collection();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_HoldingsSolrUrl();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_Description();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_Destination();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_Format();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_Name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_Query();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_Resource();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_Schedule();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_Status();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_SubmitterNumber();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_HarvesterType();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_TimeOfLastHarvest();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_Action();

    // Column values
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnValue_HarvesterType_STANDARD();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnValue_HarvesterType_DAILY_PROOFING();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnValue_HarvesterType_SUBJECT_PROOFING();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnValue_HarvesterType_STANDARD_WITH_HOLDINGS();

    // Button texts
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_NewPeriodicJobsHarvesterButton();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_EditPeriodicJobsHarvesterButton();

    // Value texts
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String value_Enabled();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String value_Disabled();

    // Help texts
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_Collection();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_Resource();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_Schedule();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_HarvesterType();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_HoldingsSolrUrl();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_HoldingsTypeSelection();
}
