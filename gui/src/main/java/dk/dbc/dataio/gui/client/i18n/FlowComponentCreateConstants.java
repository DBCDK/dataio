package dk.dbc.dataio.gui.client.i18n;

import com.google.gwt.i18n.client.Constants;


public interface FlowComponentCreateConstants extends Constants {
    
    // Labels
    @DefaultStringValue("<translated text is missing>")
    String label_ComponentName();

    @DefaultStringValue("<translated text is missing>")
    String label_SvnProject();

    @DefaultStringValue("<translated text is missing>")
    String label_SvnRevision();

    @DefaultStringValue("<translated text is missing>")
    String label_ScriptName();

    @DefaultStringValue("<translated text is missing>")
    String label_InvocationMethod();


    // Button Texts
    @DefaultStringValue("<translated text is missing>")
    String button_Save();


    // Error messages
    @DefaultStringValue("<translated text is missing>")
    String error_InputFieldValidationError();
    
    @DefaultStringValue("<translated text is missing>")
    String error_ScmProjectNotFoundError();
    
    @DefaultStringValue("<translated text is missing>")
    String error_ScmIllegalProjectNameError();
    
    @DefaultStringValue("<translated text is missing>")
    String error_JavaScriptReferenceError();
    
    
    // Status messages
    @DefaultStringValue("<translated text is missing>")
    String status_FlowComponentSuccessfullySaved();
    
    @DefaultStringValue("<translated text is missing>")
    String status_SavingFlowComponent();
    
    @DefaultStringValue("<translated text is missing>")
    String status_Busy();
    
}
