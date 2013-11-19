package dk.dbc.dataio.gui.client.activities;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.RevisionInfo;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherError;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherException;
import dk.dbc.dataio.gui.client.i18n.FlowComponentCreateConstants;
import dk.dbc.dataio.gui.client.places.FlowComponentCreatePlace;
import dk.dbc.dataio.gui.client.presenters.FlowComponentCreatePresenter;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcherAsync;
import dk.dbc.dataio.gui.client.views.FlowComponentCreateView;
import dk.dbc.dataio.gui.util.ClientFactory;

import java.util.Arrays;
import java.util.List;

/**
 * This class represents the create flow activity encompassing saving
 * of flow data in the flow store via RPC proxy
 */
public class CreateFlowComponentActivity extends AbstractActivity implements FlowComponentCreatePresenter {
    private final FlowComponentCreateConstants constants = GWT.create(FlowComponentCreateConstants.class);
    private ClientFactory clientFactory;
    private FlowComponentCreateView flowComponentCreateView;
    private JavaScriptProjectFetcherAsync javaScriptProjectFetcher;
    private FlowStoreProxyAsync flowStoreProxy;
    
    public CreateFlowComponentActivity(FlowComponentCreatePlace place, ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        javaScriptProjectFetcher = clientFactory.getJavaScriptProjectFetcherAsync();
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
    }

    @Override
    public void bind() {
        flowComponentCreateView = clientFactory.getFlowComponentCreateView();
        flowComponentCreateView.setPresenter(this);
    }

    @Override
    public void reload() {
		flowComponentCreateView.refresh();
    }
    
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        bind();
        containerWidget.setWidget(flowComponentCreateView.asWidget());
    }

    private void fetchRevisions(String projectUrl) throws JavaScriptProjectFetcherException {
        javaScriptProjectFetcher.fetchRevisions(projectUrl, new AsyncCallback<List<RevisionInfo>>() {
            @Override
            public void onFailure(Throwable e) {
                flowComponentCreateView.fetchRevisionFailed(getJavaScriptProjectFetcherError(e),
                        e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
            }
            @Override
            public void onSuccess(List<RevisionInfo> revisions) {
                flowComponentCreateView.setAvailableRevisions(revisions);
            }
        });
    }

    private void fetchScriptNames(String projectUrl, long revision) throws JavaScriptProjectFetcherException {
        javaScriptProjectFetcher.fetchJavaScriptFileNames(projectUrl, revision, new AsyncCallback<List<String>>() {
            @Override
            public void onFailure(Throwable e) {
                flowComponentCreateView.fetchScriptNamesFailed(e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
            }
            @Override
            public void onSuccess(List<String> scriptNames) {
                flowComponentCreateView.setAvailableScriptNames(scriptNames);
            }
        });
    }

    private void fetchInvocationMethods(String projectUrl, long revision, String scriptName) throws JavaScriptProjectFetcherException {
        javaScriptProjectFetcher.fetchJavaScriptInvocationMethods(projectUrl, revision, scriptName, new AsyncCallback<List<String>>() {
            @Override
            public void onFailure(Throwable e) {
                flowComponentCreateView.fetchInvocationMethodsFailed(getJavaScriptProjectFetcherError(e),
                        e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
            }
            @Override
            public void onSuccess(List<String> invocationMethods) {
                flowComponentCreateView.setAvailableInvocationMethods(invocationMethods);
            }
        });
    }

    @Override
    public void projectNameEntered(String projectName) {
        try {
            fetchRevisions(projectName);
        } catch (JavaScriptProjectFetcherException e) {
            flowComponentCreateView.onFailure(e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
        }
    }

    @Override
    public void revisionSelected(String projectName, long selectedRevision) {
        try {
            fetchScriptNames(projectName, selectedRevision);
        } catch (JavaScriptProjectFetcherException e) {
            flowComponentCreateView.onFailure(e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
        }
    }

    @Override
    public void scriptNameSelected(String projectName, long selectedRevision, String scriptName) {
        try {
            fetchInvocationMethods(projectName, selectedRevision, scriptName);
        } catch (JavaScriptProjectFetcherException e) {
            flowComponentCreateView.onFailure(e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
        }
    }

    @Override
    public void saveFlowComponent(String componentName, String svnProject, long svnRevision, String javaScriptName, String invocationMethod) {
        fetchJavaScriptsAndSave(componentName, svnProject, svnRevision, javaScriptName, invocationMethod);  // Calls saveFlowComponentWithJavaScripts asynchronously after having fetched java scripts
    }

    private void fetchJavaScriptsAndSave(final String componentName, String svnProject, long svnRevision, String javaScriptName, final String invocationMethod) {
        javaScriptProjectFetcher.fetchRequiredJavaScript(svnProject, svnRevision, javaScriptName, invocationMethod, new AsyncCallback<List<JavaScript>>() {
            @Override
            public void onFailure(Throwable e) {
                flowComponentCreateView.onFailure(e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
            }

            @Override
            public void onSuccess(List<JavaScript> javaScripts) {
                saveFlowComponentWithJavaScripts(componentName, javaScripts, invocationMethod);
            }
        });
    }

    private void saveFlowComponentWithJavaScripts(String componentName, List<JavaScript> javaScripts, String invocationMethod) {
        final FlowComponentContent flowComponentContent = new FlowComponentContent(componentName, javaScripts, invocationMethod);
        flowStoreProxy.createFlowComponent(flowComponentContent, new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable e) {
                flowComponentCreateView.onFailure(e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
            }

            @Override
            public void onSuccess(Void aVoid) {
                flowComponentCreateView.onSuccess(constants.status_FlowComponentSuccessfullySaved());
            }
        });
    }

    private JavaScriptProjectFetcherError getJavaScriptProjectFetcherError(Throwable e) {
        JavaScriptProjectFetcherError errorCode = null;
        if (e instanceof JavaScriptProjectFetcherException) {
            errorCode = ((JavaScriptProjectFetcherException) e).getErrorCode();
        }
        return errorCode;
    }
}
