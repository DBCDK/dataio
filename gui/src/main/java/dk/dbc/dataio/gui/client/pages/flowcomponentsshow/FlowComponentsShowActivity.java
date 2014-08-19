package dk.dbc.dataio.gui.client.pages.flowcomponentsshow;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.pages.flowcomponentcreateedit.FlowComponentEditPlace;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;

import java.util.List;


/**
 * This class represents the show flows activity
 */
public class FlowComponentsShowActivity extends AbstractActivity implements FlowComponentsShowPresenter {
    private ClientFactory clientFactory;
    private FlowComponentsShowView flowComponentsShowView;
    private FlowStoreProxyAsync flowStoreProxy;
    private final PlaceController placeController;

    public FlowComponentsShowActivity(ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
        placeController = clientFactory.getPlaceController();
    }

    private void bind() {
        flowComponentsShowView = clientFactory.getFlowComponentsShowView();
        flowComponentsShowView.setPresenter(this);
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        bind();
        containerWidget.setWidget(flowComponentsShowView.asWidget());
        fetchFlowComponents();
    }

    /**
     * Creates a new place
     * @param flowComponent The flowComponent to edit
     */
    @Override
    public void editFlowComponent(FlowComponent flowComponent) {
        placeController.goTo(new FlowComponentEditPlace(flowComponent));
    }

    // Local methods
    private void fetchFlowComponents() {
        flowStoreProxy.findAllFlowComponents(new FilteredAsyncCallback<List<FlowComponent>>() {
            @Override
            public void onFilteredFailure(Throwable e) {
                flowComponentsShowView.onFailure(e.getClass().getName() + " - " + e.getMessage());
            }

            @Override
            public void onSuccess(List<FlowComponent> flowComponents) {
                flowComponentsShowView.setFlowComponents(flowComponents);
            }
        });
    }

}
