package dk.dbc.dataio.gui.client.pages.flowcomponent.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.CreatePlace;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.EditPlace;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;

import java.util.List;


/**
 * This class represents the show submitters presenter implementation
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
        view = clientFactory.getFlowComponentsShowView();
        view.setPresenter(this);
        containerWidget.setWidget(view.asWidget());
        fetchFlowComponents();
    }


    /**
     * This method opens a new view, for editing the flow component in question
     * @param model The model for the flow component to edit
     */
    @Override
    public void editFlowComponent(FlowComponentModel model) {
        placeController.goTo(new EditPlace(model));
    }

    /**
     * This method opens a new view, for creating a new flow component
     */
    @Override
    public void createFlowComponent() {
        placeController.goTo(new CreatePlace());
    }

    /*
     * Local methods
     */

    /**
     * This method fetches all flow components, and sends them to the view
     */
    private void fetchFlowComponents() {
        flowStoreProxy.findAllFlowComponents(new FetchFlowComponentsCallback());
    }


    /*
     * Private classes
     */

    /**
     * This class is the callback class for the findAllFlowComponents method in the Flow Store
     */
    protected class FetchFlowComponentsCallback extends FilteredAsyncCallback<List<FlowComponentModel>> {
        @Override
        public void onFilteredFailure(Throwable e) {
            view.setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, clientFactory.getProxyErrorTexts(), this.getClass().getCanonicalName()));
        }

        @Override
        public void onSuccess(List<FlowComponentModel> models) {
            view.setFlowComponents(models);
        }
    }

}
