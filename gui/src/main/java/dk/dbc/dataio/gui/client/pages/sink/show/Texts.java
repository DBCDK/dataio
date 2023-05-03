package dk.dbc.dataio.gui.client.pages.sink.show;

import com.google.gwt.i18n.client.Constants;
import dk.dbc.dataio.gui.client.i18n.MainConstants;


public interface Texts extends Constants {
    // Buttons
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_Edit();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_Create();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_Refresh();


    // Labels
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_Sinks();


    // Column Headers
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_Type();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_Name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_QueueName();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_ResourceName();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_Description();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String columnHeader_Action();


    // Sink Type Selection
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_DpfSink();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_ESSink();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_UpdateSink();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_DummySink();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_ImsSink();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_HiveSink();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_TickleSink();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_WorldCatSink();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_MarcConvSink();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_PeriodicJobsSink();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_VipSink();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_HoldingsItemsSink();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_DMatSink();
}
