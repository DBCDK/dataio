package dk.dbc.dataio.gui.client.pages.sink.modify;

import dk.dbc.dataio.gui.client.i18n.MainConstants;

public interface Texts extends com.google.gwt.i18n.client.Constants {
    // Labels
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_SinkType();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_SinkName();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_QueueName();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_Description();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_Timeout();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_SinkUrl();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_UserId();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_Database();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_Password();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_QueueProviders();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_EnterQueueProviders();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_SequenceAnalysis();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_AreYouSureAboutDeleting();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_ImsEndpoint();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_ProjectId();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_WorldCatRetryDiagnostics();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_EnterRetryDiagnostics();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_WorldCatEndpoint();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_VipEndpoint();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_UpdateServiceUserId();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_UpdateServicePassword();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_UpdateServiceQueueProviders();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_UpdateServiceIgnoredValidationErrors();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_EnterIgnoredValidationErrors();

    // Captions
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String caption_DeleteSink();


    // Buttons
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_Save();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_Delete();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_OK();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_SequenceAnalysisOptionAll();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_SequenceAnalysisOptionIdOnly();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_Cancel();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_Yes();


    // Sink Type Selection
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_ESSink();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_UpdateSink();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_DpfSink();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_DummySink();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_ImsSink();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_WorldCatSink();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_HiveSink();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_TickleSink();

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

    // Error messages
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_InputFieldValidationError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_NameFormatValidationError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_QueueNameValidationError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_NumericEsUserValidationError();

    // Status messages
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String status_SinkSuccessfullySaved();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String status_SinkSuccessfullyDeleted();

}
