package dk.dbc.dataio.gui.client.panels.statuspopup;

import com.google.gwt.i18n.client.Constants;
import dk.dbc.dataio.gui.client.i18n.MainConstants;

public interface Texts extends Constants {

    // Headers
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String header_Success();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String header_Failed();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String header_Ignored();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String header_Chunkifying();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String header_Processing();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String header_Delivering();

    // Labels
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_TotalCount();

    // Links
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String link_MoreInfo();

}
