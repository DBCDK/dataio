package dk.dbc.dataio.gui.client.activities;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.RevisionInfo;
import dk.dbc.dataio.engine.FlowContent;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherException;
import dk.dbc.dataio.gui.client.places.FlowComponentCreatePlace;
import dk.dbc.dataio.gui.client.presenters.FlowComponentCreatePresenter;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcherAsync;
import dk.dbc.dataio.gui.client.views.FlowComponentCreateView;
import dk.dbc.dataio.gui.client.views.FlowCreateViewImpl;
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

    
    public CreateFlowComponentActivity(FlowComponentCreatePlace place, ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        javaScriptProjectFetcher = clientFactory.getJavaScriptProjectFetcherAsync();
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

    @Override
    public void fetchRevisions(String projectUrl) throws JavaScriptProjectFetcherException {
        javaScriptProjectFetcher.fetchRevisions(projectUrl, new AsyncCallback<List<RevisionInfo>>() {
            @Override
            public void onFailure(Throwable e) {
                final String errorClassName = e.getClass().getName();
                flowComponentCreateView.displayError(errorClassName + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
            }
            @Override
            public void onSuccess(List<RevisionInfo> revisions) {
                flowComponentCreateView.setAvailableRevisions(revisions);
            }
        });
    }

    @Override
    public void fetchScriptNames(String projectUrl, long revision) throws JavaScriptProjectFetcherException {
        javaScriptProjectFetcher.fetchJavaScriptFileNames(projectUrl, revision, new AsyncCallback<List<String>>() {
            @Override
            public void onFailure(Throwable e) {
                final String errorClassName = e.getClass().getName();
                flowComponentCreateView.displayError(errorClassName + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
            }
            @Override
            public void onSuccess(List<String> scriptNames) {
                flowComponentCreateView.setAvailableScriptNames(scriptNames);
            }
        });
    }

    @Override
    public void fetchInvocationMethods(String projectUrl, long revision, String scriptName) throws JavaScriptProjectFetcherException {
        javaScriptProjectFetcher.fetchJavaScriptInvocationMethods(projectUrl, revision, scriptName, new AsyncCallback<List<String>>() {
            @Override
            public void onFailure(Throwable e) {
                final String errorClassName = e.getClass().getName();
                flowComponentCreateView.displayError(errorClassName + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
            }
            @Override
            public void onSuccess(List<String> invocationMethods) {
                flowComponentCreateView.setAvailabelInvocationMethods(invocationMethods);
            }
        });
    }
}
