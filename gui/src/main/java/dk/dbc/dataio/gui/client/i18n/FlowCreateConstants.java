package dk.dbc.dataio.gui.client.i18n;

import com.google.gwt.i18n.client.Constants;


public interface FlowCreateConstants extends Constants {
    
    // Labels
    @DefaultStringValue("Flownavn")
    String label_FlowName();

    @DefaultStringValue("Beskrivelse")
    String label_Description();

    @DefaultStringValue("Flow komponenter")
    String label_FlowComponents();

    
    // Buttons
    @DefaultStringValue("Gem")
    String button_Save();


    // Error messages
    @DefaultStringValue("Alle felter skal udfyldes.")
    String error_InputFieldValidationError();
    
    
    // Status messages
    @DefaultStringValue("Opsætningen blev gemt")
    String status_FlowSuccessfullySaved();
    
}
