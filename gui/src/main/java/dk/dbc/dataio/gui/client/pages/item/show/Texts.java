package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.i18n.client.Constants;
import dk.dbc.dataio.gui.client.i18n.MainConstants;


public interface Texts extends Constants {

    // ColumnLabels
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String column_Item();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String column_RecordId();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String column_Status();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String column_Trace();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String column_Fixed();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String column_Level();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String column_Message();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String column_Stacktrace();


    // Error Texts
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_CouldNotFetchJob();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_CouldNotFindJob();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_CouldNotFetchItems();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_CannotFetchJavaScriptLog();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_CouldNotFetchData();

    @Constants.DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_CouldNotFetchJobNotifications();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_JndiFtpFetchError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_JndiFileStoreFetchError();

    @Constants.DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_JndiElkUrlFetchError();

    // Labels
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_Back();


    // Headers
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String header_TransFile();


    // Texts
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String text_Item();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String text_JobId();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String text_Submitter();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String text_Sink();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String text_TrackingId();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String text_recordId();


    // Lifecycles
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String lifecycle_Partitioning();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String lifecycle_Processing();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String lifecycle_Delivering();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String lifecycle_PartitioningFailed();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String lifecycle_PartitioningIgnored();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String lifecycle_ProcessingFailed();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String lifecycle_ProcessingIgnored();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String lifecycle_DeliveringFailed();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String lifecycle_DeliveringIgnored();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String lifecycle_Done();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String lifecycle_Unknown();


    // Tabs
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String tab_AllItems();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String tab_FailedItems();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String tab_IgnoredItems();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String tab_JobInfo();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String tab_JobDiagnostic();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String tab_JobNotification();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String tab_JavascriptLog();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String tab_PartitioningPost();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String tab_ProcessingPost();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String tab_DeliveringPost();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String tab_NextOutputPost();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String tab_ItemDiagnostic();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String tab_Stacktrace();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String tab_Note();


    // JobInfo Prompt Labels
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobInfo_Packaging();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobInfo_Format();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobInfo_Charset();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobInfo_Destination();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobInfo_MailForNotificationAboutVerification();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobInfo_MailForNotificationAboutProcessing();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobInfo_ResultMailInitials();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobInfo_Type();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobInfo_JobCreationTime();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobInfo_JobCompletionTime();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobInfo_PreviousJobId();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobInfo_Filestore();


    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobInfo_ExportLinksHeader();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobInfo_ExportLinkItemsPartitioned();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobInfo_ExportLinkItemsProcessed();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobInfo_ExportLinkItemFailedInPartitioning();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobInfo_ExportLinkItemFailedInProcessing();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobInfo_ExportLinkItemFailedInDelivering();


    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobInfo_AncestryTransFile();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobInfo_AncestryDataFile();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobInfo_AncestryBatchId();


    // JobNotification Prompt Labels
    @Constants.DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobNotification_JobId();

    @Constants.DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobNotification_Destination();

    @Constants.DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobNotification_JobCreationTime();

    @Constants.DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobNotification_JobCompletionTime();

    @Constants.DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobNotification_Type();

    @Constants.DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobNotification_Status();

    @Constants.DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobNotification_StatusMessage();


    // Type texts
    @Constants.DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String typeJobCompleted();

    @Constants.DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String typeJobCreated();


    // Status texts
    @Constants.DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String statusCompleted();

    @Constants.DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String statusFailed();

    @Constants.DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String statusWaiting();

    // Buttons
    @Constants.DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_Save();

    @Constants.DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_Trace();

}
