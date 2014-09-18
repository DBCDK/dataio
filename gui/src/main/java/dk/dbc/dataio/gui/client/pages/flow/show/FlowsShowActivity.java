package dk.dbc.dataio.gui.client.pages.flow.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.pages.flow.modify.EditPlace;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;

import java.util.List;


/**
 * This class represents the show flows activity
 */
public class FlowsShowActivity extends AbstractActivity implements FlowsShowPresenter {
    private ClientFactory clientFactory;
    private FlowsShowView flowsShowView;
    private FlowStoreProxyAsync flowStoreProxy;
    private final PlaceController placeController;

    public FlowsShowActivity(ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
        placeController = clientFactory.getPlaceController();
    }

    private void bind() {
        flowsShowView = clientFactory.getFlowsShowView();
        flowsShowView.setPresenter(this);
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        bind();
        containerWidget.setWidget(flowsShowView.asWidget());
        fetchFlows();
    }

    @Override
    public void refreshFlowComponents(Flow flow) {
        flowStoreProxy.refreshFlowComponentsOld(flow.getId(), flow.getVersion(), new FilteredAsyncCallback<Flow>() {
            @Override
            public void onFilteredFailure(Throwable e) {
                flowsShowView.setErrorText(e.getClass().getName() + " - " + e.getMessage());
            }

            @Override
            public void onSuccess(Flow flow) {
                fetchFlows();
            }
        });
    }

    /**
     * Creates a new place
     * @param flow The flow to edit
     */
    @Override
    public void updateFlow(Flow flow) {
        placeController.goTo(new EditPlace(flow));
    }

    // Local methods
    private void fetchFlows() {
        flowStoreProxy.findAllFlows(new FilteredAsyncCallback<List<Flow>>() {
            @Override
            public void onFilteredFailure(Throwable e) {
                flowsShowView.setErrorText(e.getClass().getName() + " - " + e.getMessage());
            }
            @Override
            public void onSuccess(List<Flow> flows) {
                flowsShowView.setFlows(flows);
            }
        });
    }
}
