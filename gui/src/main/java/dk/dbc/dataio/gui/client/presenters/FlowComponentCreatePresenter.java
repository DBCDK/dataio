package dk.dbc.dataio.gui.client.presenters;

import dk.dbc.dataio.commons.types.RevisionInfo;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherException;
import java.util.List;

public interface FlowComponentCreatePresenter extends Presenter {
    public void projectNameEntered(String projectName);
    public void revisionSelected(long selectedRevision);
    public void scriptNameSelected(String scriptName);
    public void saveFlowComponent(String componentName, String svnProject, long svnRevision, String javaScriptName, String invocationMethod);
}
