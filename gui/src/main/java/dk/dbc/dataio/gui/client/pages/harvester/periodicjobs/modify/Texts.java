package dk.dbc.dataio.gui.client.pages.harvester.periodicjobs.modify;

import dk.dbc.dataio.gui.client.i18n.MainConstants;

public interface Texts extends com.google.gwt.i18n.client.Constants {
    // Labels
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_AreYouSureAboutDeleting();

    // Captions
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String caption_DeleteHarvester();

    // Prompts
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_PickupType();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_Name();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_Schedule();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_Description();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_Resource();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_Query();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_QueryFileId();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String queryFileId_Button_Remove();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_Collection();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_Destination();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_Format();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_Submitter();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_Recipients();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_Subject();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_Body();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_Mimetype();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_RecordLimit();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_FtpAddress();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_FtpUser();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_FtpPasword();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_FtpSubdir();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_SFtpAddress();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_SFtpUser();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_SFtpPasword();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_SFtpSubdir();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_Contact();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_TimeOfLastHarvest();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_OverrideFilename();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_ContentHeader();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_ContentFooter();


    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_Enabled();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_HttpReceivingAgency();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_HarvesterType();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_HoldingsSolrUrl();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String prompt_HoldingsType();

    // pickup type selection
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_PickupTypeHTTP();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_PickupTypeMail();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_PickupTypeFtp();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_PickupTypeSFtp();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_PickupTypeAnySink();


    // harvester type selection
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_HarvesterType_STANDARD();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_HarvesterType_DAILY_PROOFING();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_HarvesterType_SUBJECT_PROOFING();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_HarvesterType_STANDARD_WITH_HOLDINGS();

    // Buttons
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_Save();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_Run();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_Refresh();


    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_Delete();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_Yes();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_Cancel();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_Upload();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_validateSolr();

    // Status messages
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String status_ConfigSuccessfullySaved();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String status_JobSuccessfullyStarted();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String status_ValidateFailure();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String status_ConfigSuccessfullyDeleted();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String status_Busy();

    // Help texts
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_Contact();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_Collection();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_HoldingsSolrUrl();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_HoldingsTypeSelection();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_Description();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_Query();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_QueryFileId();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_Resource();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_Schedule();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_HttpReceivingAgency();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_Recipients();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_RecordLimit();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_Subject();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_Body();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_FtpAddress();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_FtpUser();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_FtpPassword();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_FtpSubdir();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_SFtpAddress();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_SFtpUser();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_SFtpPassword();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String help_SFtpSubdir();

    // Holdings types
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_HoldingsType_INCLUSIVE();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String selection_HoldingsType_EXCLUSIVE();

    // Error messages
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_HarvesterNotFound();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_IllegalResourceValidationError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_InputFieldValidationError();

    @DefaultStringValue((MainConstants.TRANSLATED_TEXT_IS_MISSING))
    String error_MailRecordLimitInvalidValue();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_JndiFileStoreFetchError();

    String status_JobStartFailed();

    String status_WaitForHarvesterStatus();

}
