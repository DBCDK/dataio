package dk.dbc.dataio.gui.client.pages.faileditems;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.view.client.ListDataProvider;
import dk.dbc.dataio.gui.client.pages.javascriptlog.JavaScriptLogPlace;
import dk.dbc.dataio.gui.util.ClientFactory;

import java.util.ArrayList;
import java.util.List;

public class PresenterImpl extends AbstractActivity implements Presenter {
    protected ClientFactory clientFactory;
    protected View view;
    protected PlaceController placeController;

    protected ListDataProvider<FailedItemModel> failedItemsDataProvider = new ListDataProvider<FailedItemModel>();

    public PresenterImpl(ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        placeController = clientFactory.getPlaceController();
    }

    /**
     * start method
     * Is called by PlaceManager, whenever the Place is being invoked
     * This method is the start signal for the presenter
     * @param containerWidget the widget to use
     * @param eventBus the eventBus to use
     */
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        view = clientFactory.getFaileditemsView();
        view.setPresenter(this);
        containerWidget.setWidget(view.asWidget());
        getAllFailedItems();
    }

    private void getAllFailedItems() {
        // The following implementation is replaced by the call to the Job Store Proxy, whenever it is ready
        List<FailedItemModel> failedItems = new ArrayList<FailedItemModel>();
        failedItems.add(new FailedItemModel("11", "Første fejlede post"));
        failedItems.add(new FailedItemModel("22", "Anden fejlede post"));
        failedItems.add(new FailedItemModel("33", "Tredje fejlede post"));
        failedItemsDataProvider.setList(failedItems);
        view.setFailedItemsDataProvider(failedItemsDataProvider);
    }

    /**
     * A signal from the view, saying that an item has been selected in the failed items list
     */
    @Override
    public void failedItemSelected(String failedItemId) {
        placeController.goTo(new JavaScriptLogPlace(failedItemId));
    }

}
