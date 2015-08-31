package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.i18n.client.Constants;
import dk.dbc.dataio.gui.client.i18n.MainConstants;

public interface Texts extends Constants{

    // Header text
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String text_Filter();


    // Other texts
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String text_AddFilter();


    // Specific Job Filter texts
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String sinkFilter_name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String sinkFilter_PromptText();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String sinkFilter_ChooseASinkName();


    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String submitterFilter_name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String submitterFilter_PromptText();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String submitterFilter_toolTip();


    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String suppressSubmitterFilter_name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String suppressSubmitterFilter_ShowAllSubmitterJobs();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String suppressSubmitterFilter_SuppressSubmitterJobs();
}
