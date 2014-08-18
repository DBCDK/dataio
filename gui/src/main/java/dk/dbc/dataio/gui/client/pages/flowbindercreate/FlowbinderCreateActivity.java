package dk.dbc.dataio.gui.client.pages.flowbindercreate;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents the create flowbinder activity encompassing saving
 * of flowbinder data in the flow store via RPC proxy
 */
public class FlowbinderCreateActivity extends AbstractActivity implements FlowbinderCreatePresenter {
    private final FlowbinderCreateConstants constants = GWT.create(FlowbinderCreateConstants.class);
    private ClientFactory clientFactory;
    private FlowbinderCreateView flowbinderCreateView;
    private FlowStoreProxyAsync flowStoreProxy;
    private Map<String, Sink> availableSinks = new HashMap<String, Sink>();
    private Map<String, Submitter> availableSubmitters = new HashMap<String, Submitter>();
    private Map<String, Flow> availableFlows = new HashMap<String, Flow>();

    public FlowbinderCreateActivity(ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
    }

    @Override
    public void bind() {
        flowbinderCreateView = clientFactory.getFlowbinderCreateView();
        flowbinderCreateView.setPresenter(this);
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        bind();
        containerWidget.setWidget(flowbinderCreateView.asWidget());
        flowbinderCreateView.clearFields();
        fetchSinks();
        fetchAvailableSubmitters();
        fetchFlows();
    }

    private void fetchSinks() {
        flowStoreProxy.findAllSinks(new FilteredAsyncCallback<List<Sink>>() {
            @Override
            public void onFilteredFailure(Throwable e) {
                flowbinderCreateView.onFailure(e.getClass().getName() + " - " + e.getMessage());
            }
            @Override
            public void onSuccess(List<Sink> result) {
                Map<String, String> sinksToView = new HashMap<String, String>();
                for (Sink sink: result) {
                    String key = Long.toString(sink.getId());
                    try {
                        availableSinks.put(key, sink);
                        sinksToView.put(key, sink.getContent().getName());
                    } catch (Exception e) {
                        flowbinderCreateView.onFailure(e.getClass().getName() + " - " + e.getMessage());
                    }
                    flowbinderCreateView.setAvailableSinks(sinksToView);
                }
            }
        });
    }

    private void fetchAvailableSubmitters() {
        flowStoreProxy.findAllSubmitters(new FilteredAsyncCallback<List<Submitter>>() {
            @Override
            public void onFilteredFailure(Throwable e) {
                flowbinderCreateView.onFailure(e.getClass().getName() + " - " + e.getMessage());
            }
            @Override
            public void onSuccess(List<Submitter> result) {
                Map<String, String> submittersToView = new LinkedHashMap<String, String>();
                for (Submitter submitter: result) {
                    String key = Long.toString(submitter.getId());
                    try {
                        availableSubmitters.put(key, submitter);
                        submittersToView.put(key, formatSubmitterName(submitter.getContent()));
                    } catch (Exception e) {
                        flowbinderCreateView.onFailure(e.getClass().getName() + " - " + e.getMessage());
                    }
                }
                flowbinderCreateView.setAvailableSubmitters(submittersToView);
            }
        });
    }

    private void fetchFlows() {
        flowStoreProxy.findAllFlows(new FilteredAsyncCallback<List<Flow>>() {
            @Override
            public void onFilteredFailure(Throwable e) {
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
    public void saveFlowbinder(String name, String description, String packaging, String format, String charset, String destination, String recordSplitter, String flow, List<String> submitters, String sink) {
        long sinkId = 0;
        long flowId = 0;
        final List<Long> submitterIds = new ArrayList<Long>();
        FlowBinderContent flowbinderContent = null;
        try {
            sinkId = availableSinks.get(sink).getId();
            flowId = availableFlows.get(flow).getId();
            for (String submitterId: submitters) {
                submitterIds.add(availableSubmitters.get(submitterId).getId());
            }
            flowbinderContent = new FlowBinderContent(name, description, packaging, format, charset, destination, recordSplitter, flowId, submitterIds, sinkId);
        } catch (Exception e) {
            flowbinderCreateView.onFailure(e.getClass().getName() + " - " + e.getMessage());
        }
        flowStoreProxy.createFlowBinder(flowbinderContent, new FilteredAsyncCallback<Void>() {
            @Override
            public void onFilteredFailure(Throwable e) {
                flowbinderCreateView.onFailure(constants.error_FlowbinderAlreadyExistsError());
            }
            @Override
            public void onSuccess(Void result) {
                flowbinderCreateView.onSuccess(constants.status_SaveSuccess());
            }
        });
    }

    private String formatSubmitterName(SubmitterContent content) {
        return content.getNumber() + " (" + content.getName() + ")";
    }

}
