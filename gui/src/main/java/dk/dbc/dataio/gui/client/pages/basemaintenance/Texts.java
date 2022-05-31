package dk.dbc.dataio.gui.client.pages.basemaintenance;

import com.google.gwt.i18n.client.Constants;
import dk.dbc.dataio.gui.client.i18n.MainConstants;


public interface Texts extends Constants {

    // Prompt texts
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_TrackingIdSearch();

    // Button texts
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_TrackingIdSearch();

    // Error messages
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_JndiElkUrlFetchError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_EmptyTrackingIdError();

}
