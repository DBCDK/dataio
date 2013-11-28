package dk.dbc.dataio.gui.client.i18n;

import com.google.gwt.i18n.client.Constants;


public interface FlowComponentCreateConstants extends Constants {
    
    // Labels
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_ComponentName();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_SvnProject();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_SvnRevision();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_ScriptName();

    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String label_InvocationMethod();


    // Button Texts
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String button_Save();


    // Error messages
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_InputFieldValidationError();
    
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_ScmProjectNotFoundError();
    
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_ScmIllegalProjectNameError();
    
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String error_JavaScriptReferenceError();
    
    
    // Status messages
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String status_FlowComponentSuccessfullySaved();
    
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String status_SavingFlowComponent();
    
    @DefaultStringValue(MainConstants.TRANSLATED_TEXT_IS_MISSING)
    String status_Busy();
    
}
