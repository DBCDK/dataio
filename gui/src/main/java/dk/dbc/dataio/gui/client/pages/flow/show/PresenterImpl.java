package dk.dbc.dataio.gui.client.pages.flow.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.pages.flow.modify.EditPlace;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;

import java.util.List;


/**
 * This class represents the show flows presenter implementation
 */
public class PresenterImpl extends AbstractActivity implements Presenter {
    private ClientFactory clientFactory;
    private View view;
    private FlowStoreProxyAsync flowStoreProxy;
    private final PlaceController placeController;


    /**
     * Default constructor
     *
     * @param clientFactory The client factory to be used
     */
    public PresenterImpl(ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
        placeController = clientFactory.getPlaceController();
    }


    /**
     * start method
     * Is called by PlaceManager, whenever the Place is being invoked
     * This method is the start signal for the presenter
     *
     * @param containerWidget the widget to use
     * @param eventBus        the eventBus to use
     */
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        view = clientFactory.getFlowsShowView();
        view.setPresenter(this);
        containerWidget.setWidget(view.asWidget());
        fetchFlows();
    }


    /**
     * This method opens a new view, for editing the flow in question
     * @param model The model for the flow to edit
     */
    @Override
    public void editFlow(FlowModel model) {
        placeController.goTo(new EditPlace(model));
    }


    /**
     * This method refreshes all flowcomponents in the flow, passed as a parameter in the call to the method
     * @param model The flow model, in which all flow components is refreshed
     */
    @Override
    public void refreshFlowComponents(FlowModel model) {
        flowStoreProxy.refreshFlowComponents(model.getId(), model.getVersion(), new RefreshFlowComponentsCallback());
    }

    /*
     * Local methods
     */

    /**
     * This method fetches all flows, and sends them to the view
     */
    private void fetchFlows() {
        flowStoreProxy.findAllFlows(new FetchFlowsCallback());
    }


    /*
     * Private classes
     */

    /**
     * This class is the callback class for the findAllFlows method in the Flow Store
     */
    protected class FetchFlowsCallback extends FilteredAsyncCallback<List<FlowModel>> {
        @Override
        public void onFilteredFailure(Throwable e) {
            view.setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, clientFactory.getProxyErrorTexts(), this.getClass().getCanonicalName()));
        }

        @Override
        public void onSuccess(List<FlowModel> models) {
            view.setFlows(models);
        }
    }

    protected class RefreshFlowComponentsCallback extends FilteredAsyncCallback<FlowModel> {
        @Override
        public void onFilteredFailure(Throwable e) {
            view.setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, clientFactory.getProxyErrorTexts(), null));
        }

        @Override
        public void onSuccess(FlowModel model) {
            fetchFlows();
        }
    }

}
