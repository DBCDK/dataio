/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

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
    String numberFilter_name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String numberFilter_PromptText();
}
