package dk.dbc.dataio.gui.client.pages.flowcomponent.modify;

import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

public interface FlowComponentCreateEditPresenter extends GenericPresenter {
    void projectNameEntered(String projectName);
    void revisionSelected(String projectName, long selectedRevision);
    void scriptNameSelected(String projectName, long selectedRevision, String scriptName);
    void saveFlowComponent(String componentName, String svnProject, long svnRevision, String invocationJavascriptName, String invocationMethod);
}
