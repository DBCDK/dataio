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
    protected static final int PAGE_SIZE = 20;

    protected ClientFactory clientFactory;
    protected Texts texts;
    protected View view;
    protected PlaceController placeController;
    protected JobStoreProxyAsync jobStoreProxy;
    protected LogStoreProxyAsync logStoreProxy;
    protected String jobId;
    protected String submitterNumber;
    protected String sinkName;
    protected int allItemCounter;
    protected int failedItemCounter;
    protected int ignoredItemCounter;

    public PresenterImpl(com.google.gwt.place.shared.Place place, ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        texts = clientFactory.getItemsShowTexts();
        placeController = clientFactory.getPlaceController();
        jobStoreProxy = clientFactory.getJobStoreProxyAsync();
        logStoreProxy = clientFactory.getLogStoreProxyAsync();
        Place showPlace = (Place) place;
        this.jobId = showPlace.getJobId();
        this.submitterNumber = showPlace.getSubmitterNumber();
        this.sinkName = showPlace.getSinkName();
        this.allItemCounter = Integer.parseInt(showPlace.getItemCounter());
        this.failedItemCounter = Integer.parseInt(showPlace.getFailedItemCounter());
        this.ignoredItemCounter = Integer.parseInt(showPlace.getIgnoredItemCounter());
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
        view.allItemsList.itemsPager.setPageSize(PAGE_SIZE);
        view.failedItemsList.itemsPager.setPageSize(PAGE_SIZE);
        view.ignoredItemsList.itemsPager.setPageSize(PAGE_SIZE);
        view.allItemsList.itemsTable.setRowCount(0); //clear table on startup
        view.failedItemsList.itemsTable.setRowCount(0); //clear table on startup
        view.ignoredItemsList.itemsTable.setRowCount(0); //clear table on startup
        if (failedItemCounter != 0) {
            view.tabPanel.selectTab(ViewWidget.FAILED_ITEMS_TAB_INDEX);
        } else if (ignoredItemCounter != 0) {
            view.tabPanel.selectTab(ViewWidget.IGNORED_ITEMS_TAB_INDEX);
        } else {
            view.tabPanel.selectTab(ViewWidget.ALL_ITEMS_TAB_INDEX);
        }
    }


    /*
     * Overridden methods
     */

    /**
     * This method is called by the view, whenever the All Items tab has been selected
     */
    @Override
    public void allItemsTabSelected() {
        getItems(ItemListCriteriaModel.ItemSearchType.ALL, allItemCounter, view.allItemsList);
    }

    /**
     * This method is called by the view, whenever the Failed Items tab has been selected
     */
    @Override
    public void failedItemsTabSelected() {
        getItems(ItemListCriteriaModel.ItemSearchType.FAILED, failedItemCounter, view.failedItemsList);
    }

    /**
     * This method is called by the view, whenever the Ignored Items tab has been selected
     */
    @Override
    public void ignoredItemsTabSelected() {
        getItems(ItemListCriteriaModel.ItemSearchType.IGNORED, ignoredItemCounter, view.ignoredItemsList);
    }

    /**
     * This method is called by the view, whenever the Job Info tab has been selected
     */
    @Override
    public void jobInfoTabSelected() {
        // Stuff to be put in here...
    }

    /**
     * An indication from the view, that an item has been selected
     * @param itemModel The model for the selected item
     */
    @Override
    public void itemSelected(ItemsListView listView, ItemModel itemModel) {
        listView.detailedTabs.clear();
        listView.detailedTabs.add(new JavascriptLogTabContent(texts, logStoreProxy, itemModel), texts.tab_JavascriptLog());
        if (listView.detailedTabs.getWidgetCount() > 0) {
            listView.detailedTabs.selectTab(0);
            listView.detailedTabs.setVisible(true);
        }
    }


    /*
     * Private methods
     */

    /**
     * This method fetches items from the job store, and instantiates a callback class to take further action
     *
     * @param itemSearchType The search type (ALL, FAILED or IGNORED)
     * @param rowCount The number of rows in the data
     * @param listView The list view in question
     */
    private void getItems(ItemListCriteriaModel.ItemSearchType itemSearchType, int rowCount, ItemsListView listView) {
        final ItemListCriteriaModel itemListCriteriaModel = new ItemListCriteriaModel();
        final int offset = listView.itemsTable.getVisibleRange().getStart();
        ItemsCallback callback = new ItemsCallback(listView, rowCount, offset);

        view.setSelectionEnabled(false);
        listView.detailedTabs.clear();
        listView.detailedTabs.setVisible(false);
        itemListCriteriaModel.setItemSearchType(itemSearchType);
        itemListCriteriaModel.setJobId(this.jobId);
        itemListCriteriaModel.setLimit(listView.itemsPager.getPageSize());
        itemListCriteriaModel.setOffset(offset);
        jobStoreProxy.listItems(itemListCriteriaModel, callback);
    }

    /**
     * This method constructs a Job Header Text from a Job Id, a Submitter Number and a Sink Name
     * @return The resulting Job Header Text
     */
    private String constructJobHeaderText() {
        return texts.text_JobId() + " " + jobId + ", "
                + texts.text_Submitter() + " " + submitterNumber + ", "
                + texts.text_Sink() + " " + sinkName;
    }


    /*
     * Private classes
     */

    class ItemsCallback implements AsyncCallback<List<ItemModel>> {
        private final ItemsListView listView;
        private final int rowCount;
        private final int offset;

        public ItemsCallback(ItemsListView listView, int rowCount, int offset) {
            this.listView = listView;
            this.rowCount = rowCount;
            this.offset = offset;
        }
        @Override
        public void onFailure(Throwable throwable) {
            view.setErrorText(texts.error_CouldNotFetchItems());
        }
        @Override
        public void onSuccess(List<ItemModel> itemModels) {
            listView.itemsTable.setRowCount(rowCount);
            listView.itemsTable.setRowData(offset, itemModels);
            view.setSelectionEnabled(true);
            if(itemModels.size() > 0) {
                listView.itemsTable.getSelectionModel().setSelected(itemModels.get(0), true);
            }
        }
    }

}
