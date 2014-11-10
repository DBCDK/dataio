package dk.dbc.dataio.gui.client.pages.flowcomponent.modify;

import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

public interface Presenter extends GenericPresenter {
    void nameChanged(String name);
    void projectChanged(String name);
    void revisionChanged(String selectedRevision);
    void scriptNameChanged(String selectedScriptName);
    void invocationMethodChanged(String selectedInvocationMethod);
    void keyPressed();
    void saveButtonPressed();
}
