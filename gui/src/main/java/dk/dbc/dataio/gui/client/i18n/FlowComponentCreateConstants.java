package dk.dbc.dataio.gui.client.i18n;

import com.google.gwt.i18n.client.Constants;


public interface FlowComponentCreateConstants extends Constants {
    
    // Labels
    @DefaultStringValue("Komponentnavn")
    String label_ComponentName();

    @DefaultStringValue("SVN Projekt")
    String label_SvnProject();

    @DefaultStringValue("SVN Revision")
    String label_SvnRevision();

    @DefaultStringValue("Script navn")
    String label_ScriptName();

    @DefaultStringValue("Invocation Method")
    String label_InvocationMethod();


    // Button Texts
    @DefaultStringValue("Gem")
    String button_Save();


    // Error messages
    @DefaultStringValue("Alle felter skal udfyldes.")
    String error_InputFieldValidationError();
    
    @DefaultStringValue("Det angivne projekt findes ikke i SVN.")
    String error_ScmProjectNotFoundError();
    
    @DefaultStringValue("Det angivne projekt må ikke indeholde sti elementer.")
    String error_ScmIllegalProjectNameError();
    
    @DefaultStringValue("Der skete en fejl i forbindelse med kald til SVN. Prøv at vælge en anden revision eller et andet javascript.")
    String error_JavaScriptReferenceError();
    
    
    // Status messages
    @DefaultStringValue("Opsætningen blev gemt")
    String status_FlowComponentSuccessfullySaved();
    
    @DefaultStringValue("Opsætningen gemmes...")
    String status_SavingFlowComponent();
    
    @DefaultStringValue("Busy...")
    String status_Busy();
    
}
