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


    // Error Texts
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_CouldNotFetchItems();


    // Labels
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_Back();


    // Button texts
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_AllItems();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_FailedItems();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_IgnoredItems();


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
    String tab_JavascriptLog();

}
