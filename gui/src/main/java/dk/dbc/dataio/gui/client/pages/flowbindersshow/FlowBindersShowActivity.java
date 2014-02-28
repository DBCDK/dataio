package dk.dbc.dataio.gui.client.pages.flowbindersshow;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.types.FlowBinderContentViewData;
import dk.dbc.dataio.gui.util.ClientFactory;
import java.util.ArrayList;
import java.util.List;


/**
 * This class represents the show flowbinders activity
 */
public class FlowBindersShowActivity extends AbstractActivity implements FlowBindersShowPresenter {
    private final ClientFactory clientFactory;
    private FlowBindersShowView flowBindersShowView;
    private final FlowStoreProxyAsync flowStoreProxy;

    private static List<FlowBinder> flowBinders = null;
    private List<Flow> flows = null;
    private List<Sink> sinks = null;
    private List<Submitter> submitters = null;

    public FlowBindersShowActivity(/*FlowBindersShowPlace place,*/ ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
    }

    @Override
    public void bind() {
        flowBindersShowView = clientFactory.getFlowBindersShowView();
        flowBindersShowView.setPresenter(this);
    }

    @Override
    public void reload() {
		flowBindersShowView.refresh();
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



    // Local methods

    private void fetchFlowBinders() {
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
        flowStoreProxy.findAllFlows(new AsyncCallback<List<Flow>>() {
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
        flowStoreProxy.findAllSinks(new AsyncCallback<List<Sink>>() {
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
        flowStoreProxy.findAllSubmitters(new AsyncCallback<List<Submitter>>() {
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
        this.flowBinders = flowBinders;
        sendDataToView();
    }

    private void setFlows(List<Flow> flows) {
        this.flows = flows;
        sendDataToView();
    }

    private void setSinks(List<Sink> sinks) {
        this.sinks = sinks;
        sendDataToView();
    }

    private void setSubmitters(List<Submitter> submitters) {
        this.submitters = submitters;
        sendDataToView();
    }

    private void sendDataToView() {
        if (flowBinders != null && flows != null && sinks != null && submitters != null) {
            List<FlowBinderContentViewData> result = new ArrayList<FlowBinderContentViewData>();
            for (FlowBinder flowBinder: flowBinders) {
                FlowBinderContentViewData flowBinderContentViewData = new FlowBinderContentViewData(
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
                    getSubmitterNames(flowBinder.getContent().getSubmitterIds()),
                    flowBinder.getContent().getSinkId(),
                    getSinkName(flowBinder.getContent().getSinkId())
                );
                result.add(flowBinderContentViewData);
            }
            flowBindersShowView.setFlowBinders(result);
        }
    }

    private String getFlowName(Long flowId) {
        for (Flow flow: flows) {
            if (flow.getId() == flowId) {
                return flow.getContent().getName();
            }
        }
        // At this point, the flow name was not found in the flow store.
        // Instead note the flow id as a text string:
        return "Flow ID: " + Long.toString(flowId);
    }

    private List<String> getSubmitterNames(List<Long> submitterIds) {
        List<String> result = new ArrayList<String>();
        for (Long id: submitterIds) {
            String submitterName = "Submitter ID: " + Long.toString(id); // In case submitter name is not found in flowstore
            for (Submitter submitter: submitters) {
                if (submitter.getId() == id) {
                    submitterName = submitter.getContent().getName();
                }
            }
            result.add(submitterName);
        }
        return result;
    }

    private String getSinkName(Long sinkId) {
        for (Sink sink: sinks) {
            if (sink.getId() == sinkId) {
                return sink.getContent().getName();
            }
        }
        // At this point, the sink name was not found in the flow store.
        // Instead note the sink id as a text string:
        return "Sink ID: " + Long.toString(sinkId);
    }

    private void displayErrorMessage(String message, Throwable e) {
        flowBindersShowView.onFailure(message + ", " + e.getClass().getName() + " - " + e.getMessage());
    }

}
