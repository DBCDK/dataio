package dk.dbc.dataio.gui.client.presenters;

import dk.dbc.dataio.commons.types.RevisionInfo;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherException;
import java.util.List;

public interface FlowComponentCreatePresenter extends Presenter {

    public void fetchRevisions(String projectUrl) throws JavaScriptProjectFetcherException;
    public void fetchScriptNames(String project, long revision) throws JavaScriptProjectFetcherException;
    public void fetchInvocationMethods(String project, long revision, String scriptName) throws JavaScriptProjectFetcherException;
    
}
