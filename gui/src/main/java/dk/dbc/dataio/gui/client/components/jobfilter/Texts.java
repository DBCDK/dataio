package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.i18n.client.Constants;
import dk.dbc.dataio.gui.client.i18n.MainConstants;

public interface Texts extends Constants {

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


    // Job Status Filter
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String jobStatusFilter_name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String JobStatusFilter_Prompt();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String jobStatusFilter_Active();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String jobStatusFilter_Waiting();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String jobStatusFilter_Done();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String jobStatusFilter_Failed();


    // Item Filter
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String itemFilter_name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String itemFilter_PromptText();
}
