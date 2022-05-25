package dk.dbc.dataio.gui.client.components.flowbinderfilter;

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
    String charsetFilter_name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String charsetFilter_PromptText();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String dataPartitionerFilter_name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String dataPartitionerFilter_PromptText();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String destinationFilter_name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String destinationFilter_PromptText();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String flowFilter_name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String flowFilter_PromptText();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String flowFilter_Choose();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String formatFilter_name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String formatFilter_PromptText();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String nameFilter_name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String nameFilter_PromptText();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String packagingFilter_name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String packagingFilter_PromptText();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String queueProviderFilter_name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String queueProviderFilter_PromptText();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String sinkFilter_name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String sinkFilter_PromptText();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String sinkFilter_Choose();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String submitterFilter_name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String submitterFilter_PromptText();
}
