package dk.dbc.dataio.gui.client.i18n;

import com.google.gwt.i18n.client.Constants;


public interface SubmitterCreateConstants extends Constants {
    
    // Labels
    @DefaultStringValue("<translated text is missing>")
    String label_SubmitterNumber();

    @DefaultStringValue("<translated text is missing>")
    String label_SubmitterName();

    @DefaultStringValue("<translated text is missing>")
    String label_Description();


    // Buttons
    @DefaultStringValue("<translated text is missing>")
    String button_Save();


    // Error messages
    @DefaultStringValue("<translated text is missing>")
    String error_InputFieldValidationError();
    
    @DefaultStringValue("<translated text is missing>")
    String error_NumberInputFieldValidationError();
    
    @DefaultStringValue("<translated text is missing>")
    String error_ProxyKeyViolationError();
    
    @DefaultStringValue("<translated text is missing>")
    String error_ProxyDataValidationError();
    
    
    // Status messages
    @DefaultStringValue("<translated text is missing>")
    String status_SubmitterSuccessfullySaved();
    
}
