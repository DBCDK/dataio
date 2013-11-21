package dk.dbc.dataio.gui.client.i18n;

import com.google.gwt.i18n.client.Constants;


public interface FlowCreateConstants extends Constants {
    
    // Labels
    @DefaultStringValue("<translated text is missing>")
    String label_FlowName();

    @DefaultStringValue("<translated text is missing>")
    String label_Description();

    @DefaultStringValue("<translated text is missing>")
    String label_FlowComponents();

    
    // Buttons
    @DefaultStringValue("<translated text is missing>")
    String button_Save();


    // Error messages
    @DefaultStringValue("<translated text is missing>")
    String error_InputFieldValidationError();
    
    
    // Status messages
    @DefaultStringValue("<translated text is missing>")
    String status_FlowSuccessfullySaved();
    
}
