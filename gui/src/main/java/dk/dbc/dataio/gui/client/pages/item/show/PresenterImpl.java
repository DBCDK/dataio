package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.model.ItemListCriteriaModel;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.LogStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;

import java.util.List;

public class PresenterImpl extends AbstractActivity implements Presenter {
    protected ClientFactory clientFactory;
    protected Texts texts;
    protected View view;
    protected PlaceController placeController;
    protected JobStoreProxyAsync jobStoreProxy;
    protected LogStoreProxyAsync logStoreProxy;
    protected String jobId;
    protected String submitterNumber;
    protected String sinkName;

    public PresenterImpl(com.google.gwt.place.shared.Place place, ClientFactory clientFactory, Texts texts) {
        this.clientFactory = clientFactory;
        this.texts = texts;
        placeController = clientFactory.getPlaceController();
        jobStoreProxy = clientFactory.getJobStoreProxyAsync();
        logStoreProxy = clientFactory.getLogStoreProxyAsync();
        Place showPlace = (Place) place;
        this.jobId = showPlace.getJobId();
        this.submitterNumber = showPlace.getSubmitterNumber();
        this.sinkName = showPlace.getSinkName();
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
        view = clientFactory.getItemsShowView();
        view.setPresenter(this);
        view.jobHeader.setText(constructJobHeaderText());
        containerWidget.setWidget(view.asWidget());
        view.failedItemsButton.setValue(true);
        getItems();
        view.tabPanel.clear();
        view.tabPanel.setVisible(false);
    }


    /*
     * Protected methods
     *
     */

    /**
     * This method fetches items from the job store, and instantiates a callback class to take further action
     * The method checks, whether to fetch All items, only Failed items or only Ignored items, and sets
     * the ItemListCriteria according to that.
     */
    protected void getItems() {
        ItemListCriteriaModel itemListCriteriaModel = new ItemListCriteriaModel();
        itemListCriteriaModel.setJobId(this.jobId);
        if (view.allItemsButton.getValue()) {
            itemListCriteriaModel.setItemSearchType(ItemListCriteriaModel.ItemSearchType.ALL);
        } else if (view.failedItemsButton.getValue()) {
            itemListCriteriaModel.setItemSearchType(ItemListCriteriaModel.ItemSearchType.FAILED);
        } else if (view.ignoredItemsButton.getValue()) {
            itemListCriteriaModel.setItemSearchType(ItemListCriteriaModel.ItemSearchType.IGNORED);
        } else {
            return;
        }
        jobStoreProxy.listItems(itemListCriteriaModel, new GetItemsCallback());
    }

    /*
     * Private methods
     */
    private String constructJobHeaderText() {
        return texts.text_JobId() + " " + jobId + ", "
                + texts.text_Submitter() + " " + submitterNumber + ", "
                + texts.text_Sink() + " " + sinkName;
    }

    /*
     * Overridden methods
     */

     /**
     * An indication from the view, that an item has been selected
     * @param itemModel The model for the selected item
     */
    @Override
    public void itemSelected(ItemModel itemModel) {
        view.tabPanel.clear();
        view.addTab(new JavascriptLogTabContent(texts, logStoreProxy, itemModel), texts.tab_JavascriptLog());
        if (view.tabPanel.getWidgetCount() > 0) {
            view.tabPanel.selectTab(0);
            view.tabPanel.setVisible(true);
        }
    }

    /**
     * This method filters the shown posts, so that the items as requested by the
     * Radio Buttons are shown
     */
    @Override
    public void filterItems() {
        view.tabPanel.setVisible(false);
        view.tabPanel.clear();
        getItems();
    }


    /*
     * Private classes
     */

    class GetItemsCallback implements AsyncCallback<List<ItemModel>> {
        @Override
        public void onFailure(Throwable throwable) {
            view.setErrorText(texts.error_CouldNotFetchItems());
        }
        @Override
        public void onSuccess(List<ItemModel> itemModels) {
            view.setItems(itemModels);
        }
    }

}
