package dk.dbc.dataio.gui.client.pages.javascriptlog;

import com.google.gwt.i18n.client.Constants;
import dk.dbc.dataio.gui.client.i18n.MainConstants;

public interface Texts extends Constants {
    // Error messages
    @Constants.DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_CannotFetchJavaScriptLog();
}
