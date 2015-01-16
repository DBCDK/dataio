package dk.dbc.dataio.gui.client.pages.sink.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.pages.sink.modify.EditPlace;
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
        view = clientFactory.getSinksShowView();
        view.setPresenter(this);
        containerWidget.setWidget(view.asWidget());
        fetchSinks();
    }


    /**
     * This method opens a new view, for editing the sink in question
     * @param model The model for the sink to edit
     */
    @Override
    public void editSink(SinkModel model) {
        placeController.goTo(new EditPlace(model));
    }

    /*
     * Local methods
     */

    /**
     * This method fetches all sinks, and sends them to the view
     */
    private void fetchSinks() {
        flowStoreProxy.findAllSinks(new FetchSinksCallback());
    }


    /*
     * Private classes
     */

    /**
     * This class is the callback class for the findAllSinks method in the Flow Store
     */
    protected class FetchSinksCallback extends FilteredAsyncCallback<List<SinkModel>> {
        @Override
        public void onFilteredFailure(Throwable e) {
            view.setErrorText(e.getClass().getName() + " - " + e.getMessage());
        }

        @Override
        public void onSuccess(List<SinkModel> models) {
            view.setSinks(models);
        }
    }

}
