package dk.dbc.dataio.gui.client.pages.flowbinder.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.gui.client.pages.flowbinder.modify.EditPlace;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.types.FlowBinderContentViewData;
import dk.dbc.dataio.gui.util.ClientFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * This class represents the show flow binders activity
 */
public class FlowBindersShowActivity extends AbstractActivity implements FlowBindersShowPresenter {
    private final ClientFactory clientFactory;
    private FlowBindersShowView flowBindersShowView;
    private final FlowStoreProxyAsync flowStoreProxy;
    private final PlaceController placeController;

    private static List<FlowBinder> flowBinders = null;
    private final Map<Long, String> flows = new HashMap<Long, String>();
    private final Map<Long, String> sinks = new HashMap<Long, String>();
    private final Map<Long, SubmitterContent> submitters = new HashMap<Long, SubmitterContent>();
    private final SimpleCounter counter = new SimpleCounter();

    public FlowBindersShowActivity(ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
        placeController = clientFactory.getPlaceController();
    }

    private void bind() {
        flowBindersShowView = clientFactory.getFlowBindersShowView();
        flowBindersShowView.setPresenter(this);
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        bind();
        containerWidget.setWidget(flowBindersShowView.asWidget());
        fetchFlowBinders();
        fetchFlows();
        fetchSinks();
        fetchSubmitters();
    }

    /**
     * Creates a new place
     * @param flowBinderContentViewData containing information about the flow binder
     */
    @Override
    public void updateFlowBinder(FlowBinderContentViewData flowBinderContentViewData) {
        placeController.goTo(new EditPlace(flowBinderContentViewData));
    }


    // Local methods

    private void fetchFlowBinders() {
        counter.increment();
        flowStoreProxy.findAllFlowBinders(new AsyncCallback<List<FlowBinder>>() {
            @Override
            public void onFailure(Throwable e) {
                displayErrorMessage("Could not fetch Flowbinder", e);
            }

            @Override
            public void onSuccess(List<FlowBinder> flowBinders) {
                setFlowBinders(flowBinders);
            }
        });
    }

    private void fetchFlows() {
        counter.increment();
        flowStoreProxy.findAllFlowsOld(new AsyncCallback<List<Flow>>() {
            @Override
            public void onFailure(Throwable e) {
                displayErrorMessage("Could not fetch Flow", e);
            }

            @Override
            public void onSuccess(List<Flow> flows) {
                setFlows(flows);
            }
        });
        }

    private void fetchSinks() {
        counter.increment();
        flowStoreProxy.findAllSinksOld(new AsyncCallback<List<Sink>>() {
            @Override
            public void onFailure(Throwable e) {
                displayErrorMessage("Could not fetch Sink", e);
            }

            @Override
            public void onSuccess(List<Sink> sinks) {
                setSinks(sinks);
            }
        });
    }

    private void fetchSubmitters() {
        counter.increment();
        flowStoreProxy.findAllSubmittersOld(new AsyncCallback<List<Submitter>>() {
            @Override
            public void onFailure(Throwable e) {
                displayErrorMessage("Could not fetch Submitter", e);
            }

            @Override
            public void onSuccess(List<Submitter> submitters) {
                setSubmitters(submitters);
            }
        });
    }

    private void setFlowBinders(List<FlowBinder> flowBinders) {
        FlowBindersShowActivity.flowBinders = flowBinders;
        sendDataToView();
    }

    private void setFlows(List<Flow> flows) {
        for (Flow flow: flows) {
            this.flows.put(flow.getId(), flow.getContent().getName());
        }
        sendDataToView();
    }

    private void setSinks(List<Sink> sinks) {
        for (Sink sink: sinks) {
            this.sinks.put(sink.getId(), sink.getContent().getName());
        }
        sendDataToView();
    }

    private void setSubmitters(List<Submitter> submitters) {
        for (Submitter submitter: submitters) {
            this.submitters.put(submitter.getId(), submitter.getContent());
        }
        sendDataToView();
    }

    private void sendDataToView() {
        if (counter.decrementAndCheck()) {
            List<FlowBinderContentViewData> result = new ArrayList<FlowBinderContentViewData>();
            for (FlowBinder flowBinder: flowBinders) {
                FlowBinderContentViewData flowBinderContentViewData = new FlowBinderContentViewData(
                    flowBinder.getId(),
                    flowBinder.getVersion(),
                    flowBinder.getContent().getName(),
                    flowBinder.getContent().getDescription(),
                    flowBinder.getContent().getPackaging(),
                    flowBinder.getContent().getFormat(),
                    flowBinder.getContent().getCharset(),
                    flowBinder.getContent().getDestination(),
                    flowBinder.getContent().getRecordSplitter(),
                    flowBinder.getContent().getFlowId(),
                    getFlowName(flowBinder.getContent().getFlowId()),
                    flowBinder.getContent().getSubmitterIds(),
                    getSubmitterContent(flowBinder.getContent().getSubmitterIds()),
                    flowBinder.getContent().getSinkId(),
                    getSinkName(flowBinder.getContent().getSinkId())
                );
                result.add(flowBinderContentViewData);
            }
            flowBindersShowView.setFlowBinders(result);
        }
    }

    private String getFlowName(Long flowId) {
        if (flows.containsKey(flowId)) {
            return flows.get(flowId);
        } else {
            return "Flow ID: " + Long.toString(flowId);
        }
    }

    private List<SubmitterContent> getSubmitterContent(List<Long> submitterIds) {
        List<SubmitterContent> result = new ArrayList<SubmitterContent>();
        for (Long id: submitterIds) {
            if (submitters.containsKey(id)) {
            result.add(submitters.get(id));
            } else {
                result.add(new SubmitterContent(id, "Submitter ID: " + Long.toString(id), "-"));
            }
        }
        return result;
    }

    private String getSinkName(Long sinkId) {
        if (sinks.containsKey(sinkId)) {
            return sinks.get(sinkId);
        } else {
            return "Sink ID: " + Long.toString(sinkId);
        }
    }

    private void displayErrorMessage(String message, Throwable e) {
        flowBindersShowView.setErrorText(message + ", " + e.getClass().getName() + " - " + e.getMessage());
    }

    private static class SimpleCounter {
        private int count = 0;
        void increment() {
            count++;
        }
        boolean decrementAndCheck() {
            if (count > 0) {
                count--;
            }
            return count<=0;
        }
    }
}
