package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.i18n.client.Constants;
import dk.dbc.dataio.gui.client.i18n.MainConstants;


public interface Texts extends Constants {

    // Menu Texts
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_Items();


    // ColumnLabels
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String column_Item();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String column_Status();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String column_Level();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String column_Message();


    // Error Texts
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_CouldNotFetchJob();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_CouldNotFetchItems();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_CannotFetchJavaScriptLog();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_CouldNotFetchItemData();


    // Labels
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_Back();


    // Texts
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String text_Item();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String text_JobId();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String text_Submitter();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String text_Sink();


    // Lifecycles
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String lifecycle_Partitioning();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String lifecycle_Processing();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String lifecycle_Delivering();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String lifecycle_Done();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String lifecycle_Unknown();


    // Tabs
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String tab_AllItems();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String tab_FailedItems();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String tab_IgnoredItems();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String tab_JobInfo();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String tab_JobDiagnostic();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String tab_JavascriptLog();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String tab_InputPost();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String tab_OutputPost();


    // JobInfo Prompt Labels
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_Packaging();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_Format();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_Charset();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_Destination();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_MailForNotificationAboutVerification();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_MailForNotificationAboutProcessing();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_ResultMailInitials();


}
