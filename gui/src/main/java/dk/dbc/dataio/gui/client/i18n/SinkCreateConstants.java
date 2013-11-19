package dk.dbc.dataio.gui.client.i18n;

import com.google.gwt.i18n.client.Constants;


public interface SinkCreateConstants extends Constants {
    
    // Labels
    @DefaultStringValue("Sink navn")
    String label_SinkName();

    @DefaultStringValue("Resource navn")
    String label_ResourceName();

    
    // Buttons
    @DefaultStringValue("Gem")
    String button_Save();


    // Error messages
    @DefaultStringValue("Alle felter skal udfyldes.")
    String error_InputFieldValidationError();
    
    @DefaultStringValue("En sink med det pågældende navn er allerede oprettet i flow store.")
    String error_ProxyKeyViolationError();
    
    @DefaultStringValue("De udfyldte felter forårsagede en data valideringsfejl i flow store.")
    String error_ProxyDataValidationError();
    
    @DefaultStringValue("Det kunne ikke undersøges, om det pågældende resource navn er en gyldig sink resource")
    String error_PingCommunicationError();
    
    @DefaultStringValue("Det pågældende resource navn er ikke en gyldig sink resource")
    String error_ResourceNameNotValid();
    
    
    // Status messages
    @DefaultStringValue("Opsætningen blev gemt")
    String status_SinkSuccessfullySaved();
    
}
