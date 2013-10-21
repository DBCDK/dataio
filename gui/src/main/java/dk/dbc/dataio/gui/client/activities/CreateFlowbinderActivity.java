package dk.dbc.dataio.gui.client.activities;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.gui.client.places.FlowbinderCreatePlace;
import dk.dbc.dataio.gui.client.presenters.FlowbinderCreatePresenter;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.views.FlowbinderCreateView;
import dk.dbc.dataio.gui.client.views.FlowbinderCreateViewImpl;
import dk.dbc.dataio.gui.util.ClientFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents the create flowbinder activity encompassing saving
 * of flowbinder data in the flow store via RPC proxy
 */
public class CreateFlowbinderActivity extends AbstractActivity implements FlowbinderCreatePresenter {
    private ClientFactory clientFactory;
    private FlowbinderCreateView flowbinderCreateView;
    private FlowStoreProxyAsync flowStoreProxy;
    private Map<String, Submitter> availableSubmitters = new HashMap<String, Submitter>();
    private Map<String, Flow> availableFlows = new HashMap<String, Flow>();

    public CreateFlowbinderActivity(FlowbinderCreatePlace place, ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
    }

    @Override
    public void bind() {
        flowbinderCreateView = clientFactory.getFlowbinderCreateView();
        flowbinderCreateView.setPresenter(this);
    }

    @Override
    public void reload() {
		flowbinderCreateView.refresh();
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        bind();
        containerWidget.setWidget(flowbinderCreateView.asWidget());
        fetchAvailableSubmitters();
        fetchFlows();
    }

    private void fetchAvailableSubmitters() {
        flowStoreProxy.findAllSubmitters(new AsyncCallback<List<Submitter>>() {
            @Override
            public void onFailure(Throwable e) {
                flowbinderCreateView.onFailure(e.getClass().getName() + " - " + e.getMessage());
            }
            @Override
            public void onSuccess(List<Submitter> result) {
                Map<String, String> submittersToView = new HashMap<String, String>();
                for (Submitter submitter: result) {
                    String key = Long.toString(submitter.getId());
                    try {
                        availableSubmitters.put(key, submitter);
                        submittersToView.put(key, submitter.getContent().getName());
                    } catch (Exception e) {
                        flowbinderCreateView.onFailure(e.getClass().getName() + " - " + e.getMessage());
                    }
                    flowbinderCreateView.setAvailableSubmitters(submittersToView);
                }
            }
        });
    }

    private void fetchFlows() {
        flowStoreProxy.findAllFlows(new AsyncCallback<List<Flow>>() {
            @Override
            public void onFailure(Throwable e) {
                flowbinderCreateView.onFailure(e.getClass().getName() + " - " + e.getMessage());
            }
            @Override
            public void onSuccess(List<Flow> result) {
                Map<String, String> flowsToView = new HashMap<String, String>();
                for (Flow flow: result) {
                    String key = Long.toString(flow.getId());
                    try {
                        availableFlows.put(key, flow);
                        flowsToView.put(key, flow.getContent().getName());
                    } catch (Exception e) {
                        flowbinderCreateView.onFailure(e.getClass().getName() + " - " + e.getMessage());
                    }
                    flowbinderCreateView.setAvailableFlows(flowsToView);
                }
            }
        });
    }

    @Override
    public void saveFlowbinder(String name, String description, String packaging, String format, String charset, String destination, String recordSplitter, String flow, List<String> submitters) {
        long flowId = 0;
        final List<Long> submitterIds = new ArrayList<Long>();
        FlowBinderContent flowbinderContent = null;
        try {
            flowId = availableFlows.get(flow).getId();
            for (String submitterId: submitters) {
                submitterIds.add(availableSubmitters.get(submitterId).getId());
            }
            flowbinderContent = new FlowBinderContent(name, description, packaging, format, charset, destination, recordSplitter, flowId, submitterIds);
        } catch (Exception e) {
            flowbinderCreateView.onFailure(e.getClass().getName() + " - " + e.getMessage());
        }
        flowStoreProxy.createFlowBinder(flowbinderContent, new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable e) {
//                flowbinderCreateView.onFailure(e.getClass().getName() + " - " + e.getMessage());
                flowbinderCreateView.onFailure("Du forsøger at oprette en Flowbinder, der allerede eksisterer");
            }
            @Override
            public void onSuccess(Void result) {
                flowbinderCreateView.onSuccess(FlowbinderCreateViewImpl.FLOWBINDER_CREATION_SAVE_SUCCESS);
            }
        });
    }

}
