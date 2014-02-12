package dk.dbc.dataio.gui.client.pages.flowcomponentcreate;

import dk.dbc.dataio.gui.client.presenters.Presenter;

public interface FlowComponentCreatePresenter extends Presenter {
    void projectNameEntered(String projectName);
    void revisionSelected(String projectName, long selectedRevision);
    void scriptNameSelected(String projectName, long selectedRevision, String scriptName);
    void saveFlowComponent(String componentName, String svnProject, long svnRevision, String javaScriptName, String invocationMethod);
}
