package dk.dbc.dataio.gui.client.pages.javascriptlog;

import com.google.gwt.i18n.client.Constants;
import dk.dbc.dataio.gui.client.i18n.MainConstants;

public interface Texts extends com.google.gwt.i18n.client.Constants {

    // Menu text
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String menu_JavaScriptLogShow();

    // Error messages
    @Constants.DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_CannotFetchJavaScriptLog();
}
