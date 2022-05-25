package dk.dbc.dataio.gui.client.components.submitterfilter;

import com.google.gwt.i18n.client.Constants;
import dk.dbc.dataio.gui.client.i18n.MainConstants;

public interface Texts extends Constants {
    // Header text
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String text_Filter();

    // Other texts
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String text_AddFilter();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String enabledFilter_name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String enabledFilter_Prompt();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String enabledFilter_Enabled();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String enabledFilter_Disabled();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String nameFilter_name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String nameFilter_PromptText();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String numberFilter_name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String numberFilter_PromptText();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String priorityFilter_name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String priorityFilter_PromptText();
}
