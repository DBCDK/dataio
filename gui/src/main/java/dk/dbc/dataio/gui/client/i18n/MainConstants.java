package dk.dbc.dataio.gui.client.i18n;

import com.google.gwt.i18n.client.Constants;

public interface MainConstants extends Constants {
    String TRANSLATED_TEXT_IS_MISSING = "<translated text is missing>";

    // Headers
    @DefaultStringValue(TRANSLATED_TEXT_IS_MISSING)
    String header_DataIO();


    // Main menu items
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String mainMenu_Submitters();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String mainMenu_Flows();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String mainMenu_Sinks();


    // Sub menu items
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String subMenu_FlowCreation();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String subMenu_FlowComponentCreation();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String subMenu_SubmitterCreation();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String subMenu_FlowbinderCreation();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String subMenu_SinkCreation();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String subMenu_FlowComponentsShow();

}
