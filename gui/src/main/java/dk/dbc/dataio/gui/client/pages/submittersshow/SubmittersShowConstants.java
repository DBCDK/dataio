package dk.dbc.dataio.gui.client.pages.submittersshow;

import com.google.gwt.i18n.client.Constants;
import dk.dbc.dataio.gui.client.i18n.MainConstants;


public interface SubmittersShowConstants extends Constants {

    // Labels
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_Submitters();


    // Column Headers
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_Number();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_Name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_Description();

}
