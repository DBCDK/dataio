package dk.dbc.dataio.gui.client.pages.flowcomponentcreateedit;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.types.RevisionInfo;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherError;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherException;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcherAsync;
import dk.dbc.dataio.gui.util.ClientFactory;

import java.util.Arrays;
import java.util.List;

/**
 * This class represents the create flow activity encompassing saving
 * of flow data in the flow store via RPC proxy
 */
public class FlowComponentCreateActivity extends AbstractActivity implements FlowComponentCreateEditPresenter {
    private final FlowComponentCreateEditConstants constants = GWT.create(FlowComponentCreateEditConstants.class);
    private ClientFactory clientFactory;
    private FlowComponentCreateEditView flowComponentCreateView;
    private JavaScriptProjectFetcherAsync javaScriptProjectFetcher;
    private FlowStoreProxyAsync flowStoreProxy;

    public FlowComponentCreateActivity(ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        javaScriptProjectFetcher = clientFactory.getJavaScriptProjectFetcherAsync();
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
    }

    @Override
    public void bind() {
        flowComponentCreateView = clientFactory.getFlowComponentCreateEditView();
        flowComponentCreateView.setPresenter(this);
    }

    @Override
    public void reload() {
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        bind();
        containerWidget.setWidget(flowComponentCreateView.asWidget());
        flowComponentCreateView.clearFields();
    }

    private void fetchRevisions(String projectUrl) throws JavaScriptProjectFetcherException {
        javaScriptProjectFetcher.fetchRevisions(projectUrl, new FilteredAsyncCallback<List<RevisionInfo>>() {
            @Override
            public void onFilteredFailure(Throwable e) {
                flowComponentCreateView.fetchRevisionFailed(getJavaScriptProjectFetcherError(e),
                        e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
            }
            @Override
            public void onSuccess(List<RevisionInfo> revisions) {
                flowComponentCreateView.setAvailableRevisions(revisions, 0);
            }
        });
    }

    private void fetchScriptNames(String projectUrl, long revision) throws JavaScriptProjectFetcherException {
        javaScriptProjectFetcher.fetchJavaScriptFileNames(projectUrl, revision, new FilteredAsyncCallback<List<String>>() {
            @Override
            public void onFilteredFailure(Throwable e) {
                flowComponentCreateView.fetchScriptNamesFailed(e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
            }
            @Override
            public void onSuccess(List<String> scriptNames) {
                flowComponentCreateView.setAvailableScriptNames(scriptNames, null);
            }
        });
    }

    private void fetchInvocationMethods(String projectUrl, long revision, String scriptName) throws JavaScriptProjectFetcherException {
        javaScriptProjectFetcher.fetchJavaScriptInvocationMethods(projectUrl, revision, scriptName, new FilteredAsyncCallback<List<String>>() {
            @Override
            public void onFilteredFailure(Throwable e) {
                flowComponentCreateView.fetchInvocationMethodsFailed(getJavaScriptProjectFetcherError(e),
                        e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
            }
            @Override
            public void onSuccess(List<String> invocationMethods) {
                flowComponentCreateView.setAvailableInvocationMethods(invocationMethods, null);
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
    public void saveFlowComponent(String componentName, String svnProjectForInvocationJavascript, long svnRevision, String invocationJavascriptName, String invocationMethod) {
        fetchJavaScriptsAndSave(componentName, svnProjectForInvocationJavascript, svnRevision, invocationJavascriptName, invocationMethod);  // Calls saveFlowComponentWithJavaScripts asynchronously after having fetched java scripts
    }

    private void fetchJavaScriptsAndSave(final String componentName, final String svnProjectForInvocationJavascript, final long svnRevision, final String invocationJavascriptName, final String invocationMethod) {
        javaScriptProjectFetcher.fetchRequiredJavaScript(svnProjectForInvocationJavascript, svnRevision, invocationJavascriptName, invocationMethod, new FilteredAsyncCallback<List<JavaScript>>() {
            @Override
            public void onFilteredFailure(Throwable e) {
                flowComponentCreateView.onFailure(e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
            }

            @Override
            public void onSuccess(List<JavaScript> javaScripts) {
                saveFlowComponentWithJavaScripts(componentName, svnProjectForInvocationJavascript, svnRevision, invocationJavascriptName, javaScripts, invocationMethod);
            }
        });
    }

    private void saveFlowComponentWithJavaScripts(String componentName, String svnProjectForInvocationJavascript, long svnRevision, String invocationJavascriptName, List<JavaScript> javaScripts, String invocationMethod) {
        final FlowComponentContent flowComponentContent = new FlowComponentContent(componentName, svnProjectForInvocationJavascript, svnRevision, invocationJavascriptName, javaScripts, invocationMethod);
        flowStoreProxy.createFlowComponent(flowComponentContent, new FilteredAsyncCallback<FlowComponent>() {
            @Override
            public void onFilteredFailure(Throwable e) {
                flowComponentCreateView.onFailure(e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
            }

            @Override
            public void onSuccess(FlowComponent flowComponent) {
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
