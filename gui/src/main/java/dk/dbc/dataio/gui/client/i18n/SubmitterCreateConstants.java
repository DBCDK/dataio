package dk.dbc.dataio.gui.client.i18n;

import com.google.gwt.i18n.client.Constants;


public interface SubmitterCreateConstants extends Constants {
    
    // Labels
    @DefaultStringValue("Submitternummer")
    String label_SubmitterNumber();

    @DefaultStringValue("Submitternavn")
    String label_SubmitterName();

    @DefaultStringValue("Beskrivelse")
    String label_Description();


    // Buttons
    @DefaultStringValue("Gem")
    String button_Save();


    // Error messages
    @DefaultStringValue("Alle felter skal udfyldes.")
    String error_InputFieldValidationError();
    
    @DefaultStringValue("Nummer felt skal indeholde en numerisk talværdi.")
    String error_NumberInputFieldValidationError();
    
    @DefaultStringValue("Et eller flere af de unikke felter { navn, nummer } er allerede oprettet i flow store.")
    String error_ProxyKeyViolationError();
    
    @DefaultStringValue("De udfyldte felter forårsagede en data valideringsfejl i flow store.")
    String error_ProxyDataValidationError();
    
    
    // Status messages
    @DefaultStringValue("Opsætningen blev gemt")
    String status_SubmitterSuccessfullySaved();
    
}
