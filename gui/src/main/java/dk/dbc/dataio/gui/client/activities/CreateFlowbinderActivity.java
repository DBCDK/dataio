package dk.dbc.dataio.gui.client.activities;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.engine.Flow;
import dk.dbc.dataio.engine.FlowBinderContent;
import dk.dbc.dataio.engine.Submitter;
import dk.dbc.dataio.gui.client.exceptions.FlowStoreProxyError;
import dk.dbc.dataio.gui.client.exceptions.FlowStoreProxyException;
import dk.dbc.dataio.gui.client.places.FlowbinderCreatePlace;
import dk.dbc.dataio.gui.client.presenters.FlowbinderCreatePresenter;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.views.FlowbinderCreateView;
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
        flowbinderCreateView.clearAvailableSubmitters();
        fetchAvailableSubmitters();
        fetchFlows();
    }

    private void fetchAvailableSubmitters() {
        flowStoreProxy.findAllSubmitters(new AsyncCallback<List<Submitter>>() {
            @Override
            public void onFailure(Throwable e) {
                flowbinderCreateView.displayError(e.getClass().getName() + " - " + e.getMessage());
            }
            @Override
            public void onSuccess(List<Submitter> result) {
                for (Submitter submitter: result) {
                    String key = Long.toString(submitter.getId()) + "-" + Long.toString(submitter.getVersion());
                    try {
                        availableSubmitters.put(key, submitter);
                        flowbinderCreateView.setAvailableSubmitter(key, submitter.getContent().getName());
                    } catch (Exception e) {
                        flowbinderCreateView.displayError(e.getClass().getName() + " - " + e.getMessage());
                    }
                }
            }
        });
    }

    private void fetchFlows() {
        flowStoreProxy.findAllFlows(new AsyncCallback<List<Flow>>() {
            @Override
            public void onFailure(Throwable e) {
                flowbinderCreateView.displayError(e.getClass().getName() + " - " + e.getMessage());
            }
            @Override
            public void onSuccess(List<Flow> result) {
                for (Flow flow: result) {
                    String key = Long.toString(flow.getId()) + "-" + Long.toString(flow.getVersion());
                    try {
                        availableFlows.put(key, flow);
                        flowbinderCreateView.setAvailableFlow(key, flow.getContent().getName());
                    } catch (Exception e) {
                    }
                }
            }
        });
    }

    @Override
    public void saveFlowbinder(String name, String frameFormat, String contentFormat, String characterSet, String sink, String recordSplitter) {
        List<Submitter> submitters = new ArrayList<Submitter>();
        for (String submitterName: flowbinderCreateView.getSelectedSubmitters()) {
            submitters.add(availableSubmitters.get(submitterName));
        }
//        FlowBinderContent flowbinderContent = FlowBinderContent(name, description, packaging, format, charset, destination, recordSplitter, Long flowId, List<Long> submitterIds);
//        flowStoreProxy.createFlowBinder(null, null);
        
//        final SubmitterContent submitterContent = new SubmitterContent(Long.valueOf(number), name, description);

//        flowStoreProxy.createSubmitter(submitterContent, new AsyncCallback<Void>() {
//            @Override
//            public void onFailure(Throwable e) {
//                submitterCreateView.onFlowStoreProxyFailure(getErrorCode(e), e.getMessage());
//            }
//
//            @Override
//            public void onSuccess(Void aVoid) {
//                submitterCreateView.onSaveSubmitterSuccess();
//            }
//        });
    }

}
