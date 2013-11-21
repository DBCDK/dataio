package dk.dbc.dataio.gui.client.i18n;

import com.google.gwt.i18n.client.Constants;


public interface SinkCreateConstants extends Constants {
    
    // Labels
    @DefaultStringValue("<translated text is missing>")
    String label_SinkName();

    @DefaultStringValue("<translated text is missing>")
    String label_ResourceName();

    
    // Buttons
    @DefaultStringValue("<translated text is missing>")
    String button_Save();


    // Error messages
    @DefaultStringValue("<translated text is missing>")
    String error_InputFieldValidationError();
    
    @DefaultStringValue("<translated text is missing>")
    String error_ProxyKeyViolationError();
    
    @DefaultStringValue("<translated text is missing>")
    String error_ProxyDataValidationError();
    
    @DefaultStringValue("<translated text is missing>")
    String error_PingCommunicationError();
    
    @DefaultStringValue("<translated text is missing>")
    String error_ResourceNameNotValid();
    
    
    // Status messages
    @DefaultStringValue("<translated text is missing>")
    String status_SinkSuccessfullySaved();
    
}
