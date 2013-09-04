package dk.dbc.dataio.gui.client.activities;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.RevisionInfo;
import dk.dbc.dataio.engine.FlowComponentContent;
import dk.dbc.dataio.engine.JavaScript;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherException;
import dk.dbc.dataio.gui.client.places.FlowComponentCreatePlace;
import dk.dbc.dataio.gui.client.presenters.FlowComponentCreatePresenter;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcherAsync;
import dk.dbc.dataio.gui.client.views.FlowComponentCreateView;
import dk.dbc.dataio.gui.client.views.FlowComponentCreateViewImpl;
import dk.dbc.dataio.gui.util.ClientFactory;
import java.util.Arrays;
import java.util.List;

/**
 * This class represents the create flow activity encompassing saving
 * of flow data in the flow store via RPC proxy
 */
public class CreateFlowComponentActivity extends AbstractActivity implements FlowComponentCreatePresenter {
    private ClientFactory clientFactory;
    private FlowComponentCreateView flowComponentCreateView;
    private JavaScriptProjectFetcherAsync javaScriptProjectFetcher;
    private FlowStoreProxyAsync flowStoreProxy;
    private String projectName = null;
    private long revision = 0;
    private String scriptName = null;
    private String invocationMethod = null;
    private String flowComponentName;
    
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
                flowComponentCreateView.disableRevisionEntry();
                flowComponentCreateView.disableScriptNameEntry();
                flowComponentCreateView.disableInvocationMethodEntry();
                flowComponentCreateView.displayError(e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
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
                flowComponentCreateView.disableScriptNameEntry();
                flowComponentCreateView.disableInvocationMethodEntry();
                flowComponentCreateView.displayError(e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
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
                flowComponentCreateView.disableInvocationMethodEntry();
                flowComponentCreateView.displayError(e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
            }
            @Override
            public void onSuccess(List<String> invocationMethods) {
                flowComponentCreateView.setAvailableInvocationMethods(invocationMethods);
            }
        });
    }

    @Override
    public void projectNameEntered(String projectName) {
        this.projectName = projectName;
        try {
            fetchRevisions(projectName);
        } catch (JavaScriptProjectFetcherException e) {
            flowComponentCreateView.displayError(e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
        }
    }

    @Override
    public void revisionSelected(long selectedRevision) {
        this.revision = selectedRevision;
        try {
            fetchScriptNames(this.projectName, this.revision);
        } catch (JavaScriptProjectFetcherException e) {
            flowComponentCreateView.displayError(e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
        }
    }

    @Override
    public void scriptNameSelected(String scriptName) {
        this.scriptName = scriptName;
        try {
            fetchInvocationMethods(this.projectName, this.revision, this.scriptName);
        } catch (JavaScriptProjectFetcherException e) {
            flowComponentCreateView.displayError(e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
        }
    }

    @Override
    public void saveFlowComponent(String componentName, String svnProject, long svnRevision, String javaScriptName, String invocationMethod) {
        this.flowComponentName = componentName;
        this.invocationMethod = invocationMethod;
        fetchJavaScriptsAndSave(svnProject, svnRevision, javaScriptName, invocationMethod);  // Calls saveFlowComponentWithJavaScripts asynchronously after having fetched java scripts
    }

    private void fetchJavaScriptsAndSave(String svnProject, long svnRevision, String javaScriptName, String invocationMethod) {
        javaScriptProjectFetcher.fetchRequiredJavaScript(svnProject, revision, javaScriptName, invocationMethod, new AsyncCallback<List<JavaScript>>() {
            @Override
            public void onFailure(Throwable e) {
                flowComponentCreateView.displaySuccess("");
                flowComponentCreateView.displayError(e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
            }

            @Override
            public void onSuccess(List<JavaScript> javaScripts) {
                saveFlowComponentWithJavaScripts(javaScripts);
            }
        });
    }

    private void saveFlowComponentWithJavaScripts(List<JavaScript> javaScripts) {
        final FlowComponentContent flowComponentContent = new FlowComponentContent(flowComponentName, javaScripts, invocationMethod);
        flowStoreProxy.createFlowComponent(flowComponentContent, new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable e) {
                flowComponentCreateView.displaySuccess("");
                flowComponentCreateView.displayError(e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
            }

            @Override
            public void onSuccess(Void aVoid) {
                flowComponentCreateView.displaySuccess(FlowComponentCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE);
            }
        });
    }
}
