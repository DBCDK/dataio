package dk.dbc.dataio.gui.client.pages.job.modify;

import com.google.gwt.i18n.client.Constants;
import dk.dbc.dataio.gui.client.i18n.MainConstants;


public interface Texts extends Constants {

    // Headers
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String header_JobRerunFromRR();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String header_JobRerunFromTickle();

    // Labels
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobInfo_JobId();

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
    String promptJobInfo_JobCompletionTime();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobInfo_JobCreationTime();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobInfo_DataFile();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String promptJobInfo_PartNumber();

    // Buttons
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_Rerun();


    // Error messages
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_InputFieldValidationError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_NumberInputFieldValidationError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_NameFormatValidationError();


    // Status messages
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String status_JobSuccesfullyRerun();

    // Other texts
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String text_Job();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String text_Created();
}
