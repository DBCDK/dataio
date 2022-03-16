package dk.dbc.dataio.gui.client.pages.iotraffic;

import dk.dbc.dataio.gui.client.i18n.MainConstants;

public interface Texts extends com.google.gwt.i18n.client.Constants {
    // Labels
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_PageTitle();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_New();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_Submitter();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_Packaging();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_Format();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_Destination();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_Action();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_AreYouSureAboutDeleting();


    // Captions
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String caption_DeleteGatekeeperDestination();


    // Buttons
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_Add();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_Delete();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_Cancel();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_Yes();


    // Error Messages
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_InputFieldValidationError();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_CannotCreateGatekeeperDestination();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_CannotFetchGatekeeperDestinations();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_CannotDeleteGatekeeperDestination();

}
