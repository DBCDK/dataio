package dk.dbc.dataio.gui.client.pages.flowcomponent.flowcomponentcreateedit;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
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

public class FlowComponentEditActivity extends AbstractActivity implements FlowComponentCreateEditPresenter {
    private final FlowComponentCreateEditConstants constants = GWT.create(FlowComponentCreateEditConstants.class);

    private ClientFactory clientFactory;
    private FlowComponentCreateEditView flowComponentEditView;
    private JavaScriptProjectFetcherAsync javaScriptProjectFetcher;
    private FlowStoreProxyAsync flowStoreProxy;

    private Long flowComponentId;
    private FlowComponent flowComponent;


    public FlowComponentEditActivity(Place place, ClientFactory clientFactory) {
        FlowComponentEditPlace flowComponentEditPlace = (FlowComponentEditPlace) place;

        flowComponentId = flowComponentEditPlace.getFlowComponentId();
        this.clientFactory = clientFactory;
        javaScriptProjectFetcher = clientFactory.getJavaScriptProjectFetcherAsync();
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
    }

    private void bind() {
        flowComponentEditView = clientFactory.getFlowComponentCreateEditView();
        flowComponentEditView.setPresenter(this);
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        bind();
        containerWidget.setWidget(flowComponentEditView.asWidget());
        getFlowComponent(flowComponentId);
        flowComponentEditView.setStatusText("");  // Clear status message
    }

    @Override
    public void projectNameEntered(String projectName) {
        try {
            fetchRevisions(projectName);
        } catch (JavaScriptProjectFetcherException e) {
            flowComponentEditView.setErrorText(e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
        }
    }

    @Override
    public void revisionSelected(String projectName, long selectedRevision) {
        try {
            fetchScriptNames(projectName, selectedRevision);
        } catch (JavaScriptProjectFetcherException e) {
            flowComponentEditView.setErrorText(e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
        }
    }

    @Override
    public void scriptNameSelected(String projectName, long selectedRevision, String scriptName) {
        try {
            fetchInvocationMethods(projectName, selectedRevision, scriptName);
        } catch (JavaScriptProjectFetcherException e) {
            flowComponentEditView.setErrorText(e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
        }
    }

    @Override
    public void saveFlowComponent(String componentName, String svnProjectForInvocationJavascript, long svnRevision, String invocationJavascriptName, String invocationMethod) {
        fetchJavaScriptsAndUpdate(componentName, svnProjectForInvocationJavascript, svnRevision, invocationJavascriptName, invocationMethod);  // Calls updateFlowComponentWithJavaScripts asynchronously after having fetched java scripts
    }

    private void fetchJavaScriptsAndUpdate(final String componentName, final String svnProjectForInvocationJavascript, final long svnRevision, final String invocationJavascriptName, final String invocationMethod) {
        javaScriptProjectFetcher.fetchRequiredJavaScript(svnProjectForInvocationJavascript, svnRevision, invocationJavascriptName, invocationMethod, new FilteredAsyncCallback<List<JavaScript>>() {
            @Override
            public void onFilteredFailure(Throwable e) {
                flowComponentEditView.setErrorText(e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
            }

            @Override
            public void onSuccess(List<JavaScript> javaScripts) {
                updateFlowComponentWithJavaScripts(componentName, svnProjectForInvocationJavascript, svnRevision, invocationJavascriptName, javaScripts, invocationMethod);
            }
        });
    }

    private void updateFlowComponentWithJavaScripts(String componentName, String svnProjectForInvocationJavascript, long svnRevision, String invocationJavascriptName, List<JavaScript> javaScripts, String invocationMethod) {
        final FlowComponentContent flowComponentContent = new FlowComponentContent(componentName, svnProjectForInvocationJavascript, svnRevision, invocationJavascriptName, javaScripts, invocationMethod);
        flowStoreProxy.updateFlowComponent(flowComponentContent, flowComponent.getId(), flowComponent.getVersion(), new FilteredAsyncCallback<FlowComponent>() {
            @Override
            public void onFilteredFailure(Throwable e) {
                flowComponentEditView.setErrorText(e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
            }

            @Override
            public void onSuccess(FlowComponent flowComponent) {
                flowComponentEditView.onSaveFlowComponentSuccess();
                setFlowComponent(flowComponent);
            }
        });
    }

    public void getFlowComponent(final Long flowComponentId) {
        flowStoreProxy.getFlowComponent(flowComponentId, new FilteredAsyncCallback<FlowComponent>() {
            @Override
            public void onFilteredFailure(Throwable caught) {
                flowComponentEditView.setErrorText(constants.error_CannotFetchFlowComponent());
            }

            @Override
            public void onSuccess(FlowComponent flowComponent) {
                setFlowComponent(flowComponent);
                flowComponentEditView.initializeFields(constants.menu_FlowComponentEdit(), flowComponent);
            }
        });
    }

    private void setFlowComponent(FlowComponent flowComponent) {
        this.flowComponent = flowComponent;
    }

    private void fetchRevisions(String projectUrl) throws JavaScriptProjectFetcherException {
        javaScriptProjectFetcher.fetchRevisions(projectUrl, new FilteredAsyncCallback<List<RevisionInfo>>() {
            @Override
            public void onFilteredFailure(Throwable e) {
                flowComponentEditView.fetchRevisionFailed(getJavaScriptProjectFetcherError(e),
                        e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
            }

            @Override
            public void onSuccess(List<RevisionInfo> revisions) {
                flowComponentEditView.setAvailableRevisions(revisions, (int) flowComponent.getContent().getSvnRevision());
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


    private void fetchScriptNames(String projectUrl, long revision) throws JavaScriptProjectFetcherException {
        javaScriptProjectFetcher.fetchJavaScriptFileNames(projectUrl, revision, new FilteredAsyncCallback<List<String>>() {
            @Override
            public void onFilteredFailure(Throwable e) {
                flowComponentEditView.fetchScriptNamesFailed(e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
            }

            @Override
            public void onSuccess(List<String> scriptNames) {
                flowComponentEditView.setAvailableScriptNames(scriptNames, flowComponent.getContent().getInvocationJavascriptName());
            }
        });
    }

    private void fetchInvocationMethods(String projectUrl, long revision, String scriptName) throws JavaScriptProjectFetcherException {
        javaScriptProjectFetcher.fetchJavaScriptInvocationMethods(projectUrl, revision, scriptName, new FilteredAsyncCallback<List<String>>() {
            @Override
            public void onFilteredFailure(Throwable e) {
                flowComponentEditView.fetchInvocationMethodsFailed(getJavaScriptProjectFetcherError(e),
                        e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
            }
            @Override
            public void onSuccess(List<String> invocationMethods) {
                flowComponentEditView.setAvailableInvocationMethods(invocationMethods, flowComponent.getContent().getInvocationMethod());
            }
        });
    }

}